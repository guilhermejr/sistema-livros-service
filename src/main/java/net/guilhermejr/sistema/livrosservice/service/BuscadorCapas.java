package net.guilhermejr.sistema.livrosservice.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.log4j.Log4j2;
import net.guilhermejr.sistema.livrosservice.domain.enums.OrigemCapa;
import net.guilhermejr.sistema.livrosservice.util.Textos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Procura a capa em três passos: o ISBN no Google Books (precisa de chave), o ISBN na
 * Open Library e, por último, título e autor no Google Books. Na base real o Google
 * quase não conhece os ISBNs brasileiros, mas acha o livro pelo título; essa capa
 * pode ser de outra edição e fica marcada como {@link OrigemCapa#GOOGLE_BOOKS_TITULO}.
 */
@Log4j2
@Component
public class BuscadorCapas {

    private static final Pattern ISBN_VALIDO = Pattern.compile("\\d{9}[\\dXx]|\\d{13}");
    private static final Duration TEMPO_LIMITE = Duration.ofSeconds(15);

    /** Uma imagem menor que isso é um "sem capa" de 1 pixel, não uma capa. */
    private static final int TAMANHO_MINIMO = 1000;

    private final RestClient restClient;
    private final String chaveGoogle;

    public BuscadorCapas(@Value("${livros.capas.google-books-chave:}") String chaveGoogle) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(TEMPO_LIMITE)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(TEMPO_LIMITE);
        this.restClient = RestClient.builder().requestFactory(fabrica).build();
        this.chaveGoogle = chaveGoogle == null ? "" : chaveGoogle.strip();
        if (this.chaveGoogle.isEmpty()) {
            log.warn("Sem googleBooksApiKey: a busca automática de capas fica parada e o botão usa só a Open Library");
        }

    }

    public record Imagem(byte[] conteudo, String tipo, OrigemCapa origem) {
    }

    /** O que a busca usa do livro. Autor é o primeiro; editora serve de desempate. */
    public record Consulta(String isbn, String titulo, String autor, String editora) {
    }

    /**
     * A fonte não respondeu (rede, erro 5xx): não dá para dizer que o livro não tem capa,
     * então a busca deve ser repetida mais tarde em vez de registrada.
     */
    public static class BuscaIndisponivelException extends RuntimeException {
        public BuscaIndisponivelException(String mensagem) {
            super(mensagem);
        }
    }

    /** O Google respondeu 429: a cota do dia acabou. */
    public static class CotaEsgotadaException extends BuscaIndisponivelException {
        public CotaEsgotadaException() {
            super("Cota diária do Google Books esgotada.");
        }
    }

    public boolean temChaveGoogle() {
        return !chaveGoogle.isEmpty();
    }

    public static boolean isbnPesquisavel(String isbn) {
        return isbn != null && ISBN_VALIDO.matcher(isbn).matches();
    }

    /**
     * Vazio quando nenhuma das fontes tem a capa. O ISBN só é usado quando é um
     * ISBN-10/13; a busca por título vale também para os livros sem ISBN.
     * Lança {@link BuscaIndisponivelException} quando uma fonte não respondeu.
     */
    public Optional<Imagem> buscar(Consulta consulta) {

        boolean google = !chaveGoogle.isEmpty();
        // A Open Library pode falhar só para uma capa (um 503 de arquivo quebrado do lado
        // dela): a busca por título ainda tenta. Se ela também não achar, a falha volta,
        // porque a Open Library talvez tivesse a capa.
        BuscaIndisponivelException falhaOpenLibrary = null;
        if (isbnPesquisavel(consulta.isbn())) {
            Optional<Imagem> imagem = google ? googleBooks("isbn:" + consulta.isbn(), null, OrigemCapa.GOOGLE_BOOKS) : Optional.empty();
            if (imagem.isEmpty()) {
                try {
                    imagem = openLibrary(consulta.isbn());
                } catch (BuscaIndisponivelException e) {
                    falhaOpenLibrary = e;
                }
            }
            if (imagem.isPresent()) {
                return imagem;
            }
        }
        String titulo = tituloBase(consulta.titulo());
        Optional<Imagem> imagem = Optional.empty();
        if (google && titulo != null) {
            String autor = consulta.autor() == null ? "" : consulta.autor().split(",")[0].strip();
            String q = "intitle:" + titulo + (autor.isEmpty() ? "" : " inauthor:" + autor);
            imagem = googleBooks(q, consulta, OrigemCapa.GOOGLE_BOOKS_TITULO);
        }
        if (imagem.isEmpty() && falhaOpenLibrary != null) {
            throw falhaOpenLibrary;
        }
        return imagem;

    }

    /**
     * Com {@code consulta}, é a busca por título: só aceita volume cujo título bata com
     * o do livro, e prefere o da mesma editora. Sem ela, vale a primeira capa achada.
     */
    private Optional<Imagem> googleBooks(String q, Consulta consulta, OrigemCapa origem) {

        Volumes volumes;
        try {
            volumes = restClient.get()
                    .uri("https://www.googleapis.com/books/v1/volumes?q={q}&maxResults=10"
                                    + "&fields=items(volumeInfo(title,publisher,imageLinks))&key={chave}",
                            q, chaveGoogle)
                    .retrieve()
                    .body(Volumes.class);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new CotaEsgotadaException();
        } catch (HttpClientErrorException e) {
            log.warn("Google Books recusou a consulta {}: {}", q, e.getMessage());
            return Optional.empty();
        } catch (RestClientException e) {
            throw new BuscaIndisponivelException("Google Books não respondeu: " + e.getMessage());
        }

        String endereco = consulta == null
                ? enderecoGoogle(volumes)
                : enderecoGooglePorTitulo(volumes, tituloBase(consulta.titulo()), consulta.editora());
        if (endereco == null) {
            return Optional.empty();
        }
        return baixar(endereco, origem);

    }

    /** A primeira miniatura entre os volumes achados, em https e sem a dobra de página desenhada. */
    static String enderecoGoogle(Volumes volumes) {
        return volumesComCapa(volumes).findFirst().map(BuscadorCapas::miniatura).orElse(null);
    }

    /**
     * Só volumes cujo título bate com o do livro: sem essa conferência, "Eu Morreria
     * Por Ti" trazia "24 contos de F. Scott Fitzgerald". Entre os que batem, o da mesma
     * editora vem primeiro.
     */
    static String enderecoGooglePorTitulo(Volumes volumes, String titulo, String editora) {

        if (titulo == null) {
            return null;
        }
        List<Volumes.VolumeInfo> iguais = volumesComCapa(volumes)
                .filter(v -> mesmoTitulo(v.title(), titulo))
                .toList();
        return iguais.stream()
                .filter(v -> mesmaEditora(v.publisher(), editora))
                .findFirst()
                .or(() -> iguais.stream().findFirst())
                .map(BuscadorCapas::miniatura)
                .orElse(null);

    }

    private static Stream<Volumes.VolumeInfo> volumesComCapa(Volumes volumes) {

        if (volumes == null || volumes.items() == null) {
            return Stream.empty();
        }
        return volumes.items().stream()
                .map(Volumes.Item::volumeInfo)
                .filter(Objects::nonNull)
                .filter(v -> v.imageLinks() != null
                        && (v.imageLinks().thumbnail() != null || v.imageLinks().smallThumbnail() != null));

    }

    private static String miniatura(Volumes.VolumeInfo volume) {
        String endereco = volume.imageLinks().thumbnail() != null ? volume.imageLinks().thumbnail() : volume.imageLinks().smallThumbnail();
        return endereco.replaceFirst("^http://", "https://").replace("&edge=curl", "");
    }

    /** O título até o primeiro " - " ou ":", que costumam separar o subtítulo ou o volume. */
    static String tituloBase(String titulo) {

        if (titulo == null) {
            return null;
        }
        String base = titulo.split(" - |:", 2)[0].strip();
        return base.isEmpty() ? null : base;

    }

    /** Igual, ou o achado começa pelo título e segue com um complemento ("O castelo - Grandes obras"). */
    static boolean mesmoTitulo(String achado, String titulo) {

        String a = normalizar(achado);
        String t = normalizar(titulo);
        return !t.isEmpty() && (a.equals(t) || a.startsWith(t + " "));

    }

    /** Alguma palavra significativa em comum: "Galera Record" e "Editora Record". */
    static boolean mesmaEditora(String achada, String editora) {

        Set<String> palavras = new HashSet<>(Arrays.asList(normalizar(editora).split(" ")));
        palavras.removeAll(PALAVRAS_EDITORA);
        palavras.removeIf(p -> p.length() < 3);
        return Arrays.stream(normalizar(achada).split(" ")).anyMatch(palavras::contains);

    }

    private static final Set<String> PALAVRAS_EDITORA = Set.of("editora", "editorial", "edicoes", "livros", "ltda", "publishing", "books");

    static String normalizar(String texto) {
        return Textos.normalizar(texto);
    }

    /** default=false faz a Open Library responder 404 em vez de uma imagem em branco. */
    private Optional<Imagem> openLibrary(String isbn) {
        return baixar("https://covers.openlibrary.org/b/isbn/" + isbn + "-M.jpg?default=false", OrigemCapa.OPEN_LIBRARY);
    }

    private Optional<Imagem> baixar(String endereco, OrigemCapa origem) {

        try {
            ResponseEntity<byte[]> resposta = restClient.get()
                    .uri(URI.create(endereco))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (requisicao, r) -> { })
                    .toEntity(byte[].class);
            byte[] conteudo = resposta.getBody();
            String tipo = resposta.getHeaders().getContentType() == null ? null : resposta.getHeaders().getContentType().toString();
            if (resposta.getStatusCode() != HttpStatus.OK || conteudo == null || conteudo.length < TAMANHO_MINIMO
                    || tipo == null || !tipo.startsWith("image/")) {
                return Optional.empty();
            }
            return Optional.of(new Imagem(conteudo, tipo, origem));
        } catch (RestClientException e) {
            throw new BuscaIndisponivelException("Não foi possível baixar a capa de " + endereco + ": " + e.getMessage());
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Volumes(List<Item> items) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Item(VolumeInfo volumeInfo) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record VolumeInfo(String title, String publisher, ImageLinks imageLinks) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record ImageLinks(String thumbnail, String smallThumbnail) {
        }

    }

}
