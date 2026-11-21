package net.guilhermejr.sistema.livrosservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.guilhermejr.seguranca.jwt.AuthenticationCurrentUserService;
import net.guilhermejr.sistema.livrosservice.api.request.LivroRequest;
import net.guilhermejr.sistema.livrosservice.api.response.DadosIsbnResponse;
import net.guilhermejr.sistema.livrosservice.api.response.ItemResponse;
import net.guilhermejr.sistema.livrosservice.api.response.LivroResponse;
import net.guilhermejr.sistema.livrosservice.api.response.OpcoesResponse;
import net.guilhermejr.sistema.livrosservice.domain.entity.*;
import net.guilhermejr.sistema.livrosservice.domain.enums.OrigemCapa;
import net.guilhermejr.sistema.livrosservice.domain.repository.*;
import net.guilhermejr.sistema.livrosservice.exception.ExceptionDefault;
import net.guilhermejr.sistema.livrosservice.exception.ExceptionNotFound;
import net.guilhermejr.sistema.livrosservice.util.Textos;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@RequiredArgsConstructor
@Service
public class LivroService {

    private static final Sort POR_NOME = Sort.by("nome");
    private static final Sort POR_ID = Sort.by("id");

    private final LivroRepository livroRepository;
    private final AutorRepository autorRepository;
    private final EditoraRepository editoraRepository;
    private final EstanteRepository estanteRepository;
    private final PaisRepository paisRepository;
    private final GeneroRepository generoRepository;
    private final FormatoRepository formatoRepository;
    private final CapaRepository capaRepository;
    private final BuscadorDadosIsbn buscadorDadosIsbn;
    private final AuthenticationCurrentUserService authenticationCurrentUserService;

    @Transactional(readOnly = true)
    public List<LivroResponse> listar() {

        UUID usuario = usuario();
        Map<Long, CapaRepository.CapaDoLivro> capas = capaRepository.idsDoUsuario(usuario).stream()
                .collect(Collectors.toMap(CapaRepository.CapaDoLivro::getLivroId, Function.identity()));
        return livroRepository.findAllByUsuarioOrderByTituloAsc(usuario).stream()
                .map(l -> resposta(l, capas.get(l.getId())))
                .toList();

    }

    @Transactional(readOnly = true)
    public LivroResponse detalhar(Long id) {
        return resposta(livroDoUsuario(id));
    }

    @Transactional(readOnly = true)
    public OpcoesResponse opcoes() {

        UUID usuario = usuario();
        return new OpcoesResponse(
                itens(paisRepository.findAll(POR_NOME), Pais::getId, Pais::getNome),
                itens(generoRepository.findAll(POR_ID), Genero::getId, Genero::getNome),
                itens(formatoRepository.findAll(POR_ID), Formato::getId, Formato::getNome),
                itens(autorRepository.findAllByUsuarioOrderByNomeAsc(usuario), Autor::getId, Autor::getNome),
                itens(editoraRepository.findAllByUsuarioOrderByNomeAsc(usuario), Editora::getId, Editora::getNome),
                itens(estanteRepository.findAllByUsuarioOrderByNomeAsc(usuario), Estante::getId, Estante::getNome));

    }

    /**
     * Para o cadastro começar preenchido. Fontes vazias = nenhuma conhece o ISBN. As
     * consultas externas ficam fora de transação; só o casamento com os cadastros lê o banco.
     */
    public DadosIsbnResponse dadosPorIsbn(String isbnDigitado) {

        String isbn = isbnDigitado == null ? "" : isbnDigitado.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        if (!BuscadorCapas.isbnPesquisavel(isbn)) {
            throw new ExceptionDefault("Informe um ISBN de 10 ou 13 dígitos.");
        }

        List<BuscadorDadosIsbn.Dados> fontes = buscadorDadosIsbn.buscar(isbn);
        BuscadorDadosIsbn.Dados dados = BuscadorDadosIsbn.juntar(fontes);
        UUID usuario = usuario();
        log.info("ISBN {}: {}", isbn, fontes.isEmpty() ? "nenhuma fonte conhece" : dados.fonte());

        List<String> autores = dados.autores() == null ? List.of() : nomesDoUsuario(dados.autores(),
                autorRepository.findAllByUsuarioOrderByNomeAsc(usuario).stream().map(Autor::getNome).toList());
        return new DadosIsbnResponse(
                isbn,
                fontes.stream().map(BuscadorDadosIsbn.Dados::fonte).toList(),
                dados.titulo(),
                dados.subtitulo(),
                autores,
                dados.editora() == null ? null : editoraDoUsuario(dados.editora(), usuario),
                dados.ano(),
                dados.paginas(),
                livroRepository.findAllByUsuarioAndIsbnOrderByTituloAsc(usuario, isbn).stream()
                        .map(l -> new ItemResponse(l.getId(), l.getTitulo())).toList());

    }

    /** Autor que o usuário já tem, escrito de outro jeito ("Nikolai Gogol" / "Nikolai Gógol"), fica com o nome dele. */
    private static List<String> nomesDoUsuario(List<String> achados, List<String> doUsuario) {

        Map<String, String> porChave = new HashMap<>();
        doUsuario.forEach(nome -> porChave.putIfAbsent(Textos.normalizar(nome), nome));
        return achados.stream()
                .map(Textos::limpar)
                .filter(Objects::nonNull)
                .map(nome -> porChave.getOrDefault(Textos.normalizar(nome), nome))
                .distinct()
                .toList();

    }

    /**
     * A editora do usuário com o mesmo nome depois de tirar "Editora", "Ltda", "Brasil",
     * espaços etc.: "Editora Antofágica LTDA" vira a "Antofágica" que ele já usa. Se duas
     * batem ("Globo Livros" e "Editora Globo"), fica a de mais livros.
     */
    private String editoraDoUsuario(String achada, UUID usuario) {

        String chave = chaveEditora(achada);
        return editoraRepository.findAllByUsuarioOrderByNomeAsc(usuario).stream()
                .filter(e -> chaveEditora(e.getNome()).equals(chave))
                .max(Comparator.comparingLong(livroRepository::countByEditora))
                .map(Editora::getNome)
                .orElse(achada);

    }

    private static final Set<String> PALAVRAS_GENERICAS_EDITORA = Set.of("editora", "editorial", "ltda", "me", "sa", "brasil", "livros");

    static String chaveEditora(String nome) {
        return Arrays.stream(Textos.normalizar(nome).split(" "))
                .filter(p -> !PALAVRAS_GENERICAS_EDITORA.contains(p))
                .reduce("", String::concat);
    }

    @Transactional
    public LivroResponse inserir(LivroRequest request) {

        Livro livro = new Livro();
        livro.setUsuario(usuario());
        preencher(livro, request);
        livroRepository.save(livro);
        log.info("Livro {} inserido", livro.getId());
        return resposta(livro);

    }

    @Transactional
    public LivroResponse atualizar(Long id, LivroRequest request) {

        Livro livro = livroDoUsuario(id);
        String isbnAnterior = livro.getIsbn();
        String tituloAnterior = livro.getTitulo();
        preencher(livro, request);

        // Outro ISBN, outro livro: a capa achada automaticamente deixa de valer e a
        // tarefa tenta de novo. Outro título derruba só a achada pelo título. Uma
        // capa enviada à mão fica.
        boolean outroIsbn = !Objects.equals(isbnAnterior, livro.getIsbn());
        boolean outroTitulo = !Objects.equals(tituloAnterior, livro.getTitulo());
        if (outroIsbn || outroTitulo) {
            capaRepository.findByLivroId(id)
                    .filter(c -> outroIsbn ? c.getOrigem() != OrigemCapa.MANUAL : c.getOrigem() == OrigemCapa.GOOGLE_BOOKS_TITULO)
                    .ifPresent(c -> {
                        capaRepository.delete(c);
                        livro.setCapaBuscada(null);
                    });
            if (outroIsbn) {
                livro.setCapaBuscada(null);
            }
        }

        livroRepository.save(livro);
        apagarCadastrosSemLivro(livro.getUsuario());
        log.info("Livro {} atualizado", id);
        return resposta(livro);

    }

    @Transactional
    public void apagar(Long id) {

        Livro livro = livroDoUsuario(id);
        livroRepository.delete(livro);
        apagarCadastrosSemLivro(livro.getUsuario());
        log.info("Livro {} apagado", id);

    }

    /** Livro de outro usuário responde 404, como nos demais serviços. */
    Livro livroDoUsuario(Long id) {
        return livroRepository.findByIdAndUsuario(id, usuario())
                .orElseThrow(() -> {
                    log.error("Livro {} não encontrado", id);
                    return new ExceptionNotFound("Livro não encontrado.");
                });
    }

    LivroResponse resposta(Livro livro) {
        return resposta(livro, capaRepository.idDoLivro(livro.getId()).orElse(null));
    }

    private void preencher(Livro livro, LivroRequest request) {

        UUID usuario = livro.getUsuario();
        livro.setIsbn(Textos.limpar(request.getIsbn()) == null ? null : request.getIsbn().strip().toUpperCase());
        livro.setTitulo(Textos.limpar(request.getTitulo()));
        livro.setSubtitulo(Textos.limpar(request.getSubtitulo()));
        livro.setAnoPublicacao(request.getAnoPublicacao());
        livro.setPaginas(request.getPaginas());
        livro.setColecao(Textos.limpar(request.getColecao()));
        livro.setTradutor(Textos.limpar(request.getTradutor()));

        livro.setPais(paisRepository.findById(request.getPaisId())
                .orElseThrow(() -> new ExceptionNotFound("País não encontrado.")));
        livro.setGenero(generoRepository.findById(request.getGeneroId())
                .orElseThrow(() -> new ExceptionNotFound("Gênero não encontrado.")));
        livro.setFormato(formatoRepository.findById(request.getFormatoId())
                .orElseThrow(() -> new ExceptionNotFound("Formato não encontrado.")));

        String editora = Textos.limpar(request.getEditora());
        livro.setEditora(editoraRepository.findFirstByUsuarioAndNomeIgnoreCase(usuario, editora)
                .orElseGet(() -> editoraRepository.save(new Editora(null, editora, usuario))));

        String estante = Textos.limpar(request.getEstante());
        livro.setEstante(estante == null ? null : estanteRepository.findFirstByUsuarioAndNomeIgnoreCase(usuario, estante)
                .orElseGet(() -> estanteRepository.save(new Estante(null, estante, usuario))));

        // O mesmo nome digitado duas vezes vira um autor só.
        Map<String, String> nomes = new LinkedHashMap<>();
        request.getAutores().stream()
                .map(Textos::limpar)
                .filter(Objects::nonNull)
                .forEach(nome -> nomes.putIfAbsent(nome.toLowerCase(Locale.ROOT), nome));
        Set<Autor> autores = new LinkedHashSet<>();
        nomes.values().forEach(nome -> autores.add(autorRepository.findFirstByUsuarioAndNomeIgnoreCase(usuario, nome)
                .orElseGet(() -> autorRepository.save(new Autor(null, nome, usuario)))));
        livro.getAutores().clear();
        livro.getAutores().addAll(autores);

    }

    /** Autor ou editora que ficou sem livro some das sugestões do formulário. */
    private void apagarCadastrosSemLivro(UUID usuario) {

        livroRepository.flush();
        int autores = autorRepository.apagarSemLivro(usuario);
        int editoras = editoraRepository.apagarSemLivro(usuario);
        if (autores + editoras > 0) {
            log.info("Apagados {} autores e {} editoras sem livro", autores, editoras);
        }

    }

    private static LivroResponse resposta(Livro livro, CapaRepository.CapaDoLivro capa) {

        return LivroResponse.builder()
                .id(livro.getId())
                .isbn(livro.getIsbn())
                .titulo(livro.getTitulo())
                .subtitulo(livro.getSubtitulo())
                .anoPublicacao(livro.getAnoPublicacao())
                .paginas(livro.getPaginas())
                .editora(livro.getEditora() == null ? null : livro.getEditora().getNome())
                .autores(livro.getAutores().stream().map(Autor::getNome).sorted(String.CASE_INSENSITIVE_ORDER).toList())
                .colecao(livro.getColecao())
                .tradutor(livro.getTradutor())
                .pais(item(livro.getPais(), Pais::getId, Pais::getNome))
                .genero(item(livro.getGenero(), Genero::getId, Genero::getNome))
                .formato(item(livro.getFormato(), Formato::getId, Formato::getNome))
                .estante(livro.getEstante() == null ? null : livro.getEstante().getNome())
                .capa(capa == null ? null : capa.getId())
                .capaOrigem(capa == null ? null : capa.getOrigem())
                .capaBuscada(livro.getCapaBuscada())
                .criado(livro.getCriado())
                .atualizado(livro.getAtualizado())
                .build();

    }

    private static <T> ItemResponse item(T valor, Function<T, Long> id, Function<T, String> nome) {
        return valor == null ? null : new ItemResponse(id.apply(valor), nome.apply(valor));
    }

    private static <T> List<ItemResponse> itens(List<T> valores, Function<T, Long> id, Function<T, String> nome) {
        return valores.stream().map(v -> item(v, id, nome)).toList();
    }

    private UUID usuario() {
        return authenticationCurrentUserService.getCurrentUser().getId();
    }

}
