package net.guilhermejr.sistema.livrosservice.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.log4j.Log4j2;
import net.guilhermejr.sistema.livrosservice.util.Textos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dados de um livro pelo ISBN, para o cadastro começar preenchido. Consulta em paralelo
 * a BrasilAPI (base da CBL: conhece quase todo livro brasileiro, mas costuma vir sem
 * autor, ano e páginas), a Open Library e o Google Books (só com chave), e cada campo
 * fica com a primeira fonte, nessa ordem, que o tiver. Uma fonte fora do ar só some do
 * resultado: aqui não há o que repetir depois, o usuário completa à mão.
 */
@Log4j2
@Component
public class BuscadorDadosIsbn {

    private static final Duration TEMPO_LIMITE = Duration.ofSeconds(10);
    private static final Pattern ANO = Pattern.compile("(1[4-9]|20)\\d{2}");
    /** A CBL usa isso quando não sabe a editora. */
    private static final Set<String> EDITORA_DESCONHECIDA = Set.of("nao definido", "nao informado");

    private final RestClient restClient;
    private final String chaveGoogle;

    public BuscadorDadosIsbn(@Value("${livros.capas.google-books-chave:}") String chaveGoogle) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(TEMPO_LIMITE)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(TEMPO_LIMITE);
        this.restClient = RestClient.builder().requestFactory(fabrica).build();
        this.chaveGoogle = chaveGoogle == null ? "" : chaveGoogle.strip();

    }

    /** Um livro como uma fonte o descreve; qualquer campo pode faltar. */
    public record Dados(String fonte, String titulo, String subtitulo, List<String> autores, String editora,
                        Integer ano, Integer paginas) {
    }

    /** Lista vazia quando nenhuma fonte conhece o ISBN. Em ordem de preferência. */
    public List<Dados> buscar(String isbn) {

        List<CompletableFuture<Optional<Dados>>> consultas = List.of(
                CompletableFuture.supplyAsync(() -> consultar("BrasilAPI", isbn, this::brasilApi)),
                CompletableFuture.supplyAsync(() -> consultar("Open Library", isbn, this::openLibrary)),
                CompletableFuture.supplyAsync(() -> chaveGoogle.isEmpty() ? Optional.<Dados>empty() : consultar("Google Books", isbn, this::googleBooks)));
        return consultas.stream().map(CompletableFuture::join).flatMap(Optional::stream).toList();

    }

    /** Cada campo da primeira fonte que o tem. */
    public static Dados juntar(List<Dados> fontes) {

        return new Dados(
                String.join(", ", fontes.stream().map(Dados::fonte).toList()),
                primeiro(fontes, d -> Textos.semCaixaAlta(Textos.limpar(d.titulo()))),
                primeiro(fontes, d -> Textos.limpar(d.subtitulo())),
                primeiro(fontes, d -> d.autores() == null || d.autores().isEmpty() ? null : d.autores()),
                primeiro(fontes, d -> {
                    String editora = Textos.limpar(d.editora());
                    return editora == null || EDITORA_DESCONHECIDA.contains(Textos.normalizar(editora)) ? null : editora;
                }),
                primeiro(fontes, Dados::ano),
                primeiro(fontes, d -> d.paginas() == null || d.paginas() < 1 ? null : d.paginas()));

    }

    private static <T> T primeiro(List<Dados> fontes, Function<Dados, T> campo) {
        return fontes.stream().map(campo).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private Optional<Dados> consultar(String fonte, String isbn, Function<String, Dados> consulta) {

        try {
            Dados dados = consulta.apply(isbn);
            return dados == null || Textos.limpar(dados.titulo()) == null ? Optional.empty() : Optional.of(dados);
        } catch (RestClientException e) {
            // 404 é o "não conheço" da BrasilAPI; o resto é fonte fora do ar. Nos dois casos ela só não ajuda.
            log.info("{} não trouxe o ISBN {}: {}", fonte, isbn, e.getMessage());
            return Optional.empty();
        }

    }

    private Dados brasilApi(String isbn) {

        BrasilApi r = restClient.get().uri("https://brasilapi.com.br/api/isbn/v1/{isbn}", isbn)
                .header("User-Agent", "sistema-livros")
                .retrieve().body(BrasilApi.class);
        return r == null ? null : new Dados("BrasilAPI", r.title(), r.subtitle(), r.authors(), r.publisher(), r.year(), r.page_count());

    }

    private Dados openLibrary(String isbn) {

        Map<String, OpenLibrary> r = restClient.get()
                .uri("https://openlibrary.org/api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data", isbn)
                .retrieve().body(new org.springframework.core.ParameterizedTypeReference<>() { });
        OpenLibrary livro = r == null ? null : r.values().stream().findFirst().orElse(null);
        if (livro == null) {
            return null;
        }
        return new Dados("Open Library", livro.title(), livro.subtitle(),
                livro.authors() == null ? null : livro.authors().stream().map(OpenLibrary.Nome::name).toList(),
                livro.publishers() == null || livro.publishers().isEmpty() ? null : livro.publishers().getFirst().name(),
                ano(livro.publish_date()), livro.number_of_pages());

    }

    private Dados googleBooks(String isbn) {

        Google r = restClient.get()
                .uri("https://www.googleapis.com/books/v1/volumes?q=isbn:{isbn}"
                        + "&fields=items(volumeInfo(title,subtitle,authors,publisher,publishedDate,pageCount))&key={chave}", isbn, chaveGoogle)
                .retrieve().body(Google.class);
        if (r == null || r.items() == null || r.items().isEmpty()) {
            return null;
        }
        Google.Info v = r.items().getFirst().volumeInfo();
        return new Dados("Google Books", v.title(), v.subtitle(), v.authors(), v.publisher(), ano(v.publishedDate()), v.pageCount());

    }

    /** O ano dentro de "Aug 22, 2014", "2014-03" ou "2014". */
    static Integer ano(String data) {

        if (data == null) {
            return null;
        }
        Matcher m = ANO.matcher(data);
        return m.find() ? Integer.valueOf(m.group()) : null;

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BrasilApi(String title, String subtitle, List<String> authors, String publisher, Integer year, Integer page_count) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenLibrary(String title, String subtitle, List<Nome> authors, List<Nome> publishers, String publish_date,
                       Integer number_of_pages) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Nome(String name) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Google(List<Item> items) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Item(Info volumeInfo) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Info(String title, String subtitle, List<String> authors, String publisher, String publishedDate,
                    Integer pageCount) {
        }
    }

}
