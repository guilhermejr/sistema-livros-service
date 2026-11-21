package net.guilhermejr.sistema.livrosservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.guilhermejr.sistema.livrosservice.api.response.LivroResponse;
import net.guilhermejr.sistema.livrosservice.domain.entity.Capa;
import net.guilhermejr.sistema.livrosservice.domain.entity.Livro;
import net.guilhermejr.sistema.livrosservice.domain.enums.OrigemCapa;
import net.guilhermejr.sistema.livrosservice.domain.repository.CapaRepository;
import net.guilhermejr.sistema.livrosservice.domain.repository.LivroRepository;
import net.guilhermejr.sistema.livrosservice.exception.ExceptionDefault;
import net.guilhermejr.sistema.livrosservice.exception.ExceptionNotFound;
import net.guilhermejr.sistema.livrosservice.util.ImagemCapa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
@Service
public class CapaService {

    private final CapaRepository capaRepository;
    private final LivroRepository livroRepository;
    private final LivroService livroService;
    private final BuscadorCapas buscadorCapas;
    private final TransactionTemplate transactionTemplate;

    /** Para o endereço público: quem tem o id da capa pode ver a imagem. */
    @Transactional(readOnly = true)
    public Capa retornar(UUID id) {
        return capaRepository.findById(id).orElseThrow(() -> new ExceptionNotFound("Capa não encontrada."));
    }

    /** Busca na hora, pelo botão do formulário: troca até uma capa enviada à mão. */
    public LivroResponse buscar(Long livroId) {

        Livro livro = livroService.livroDoUsuario(livroId);
        if (!BuscadorCapas.isbnPesquisavel(livro.getIsbn()) && !buscadorCapas.temChaveGoogle()) {
            throw new ExceptionDefault("Para buscar a capa o livro precisa de um ISBN de 10 ou 13 dígitos.");
        }

        Optional<BuscadorCapas.Imagem> imagem;
        try {
            imagem = buscadorCapas.buscar(consulta(livro));
        } catch (BuscadorCapas.CotaEsgotadaException e) {
            throw new ExceptionDefault("O Google Books não aceita mais consultas hoje. Tente amanhã ou envie a imagem.");
        } catch (BuscadorCapas.BuscaIndisponivelException e) {
            log.error("Busca da capa do livro {} falhou: {}", livroId, e.getMessage());
            throw new ExceptionDefault("O serviço de capas não respondeu. Tente de novo daqui a pouco.");
        }
        if (imagem.isEmpty()) {
            gravarBusca(livroId, null);
            throw new ExceptionDefault("Nenhuma capa encontrada" + (livro.getIsbn() == null ? "" : " para o ISBN " + livro.getIsbn())
                    + (buscadorCapas.temChaveGoogle() ? " nem pelo título" : "") + ". Envie a imagem.");
        }
        gravarBusca(livroId, imagem.get());
        return livroService.detalhar(livroId);

    }

    @Transactional
    public LivroResponse enviar(Long livroId, MultipartFile arquivo) {

        Livro livro = livroService.livroDoUsuario(livroId);
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ExceptionDefault("Envie a imagem da capa.");
        }

        byte[] conteudo;
        try {
            conteudo = ImagemCapa.converter(arquivo.getBytes());
        } catch (IOException e) {
            conteudo = null;
        }
        if (conteudo == null) {
            throw new ExceptionDefault("Envie uma imagem JPEG ou PNG.");
        }

        trocar(livroId, new Capa(livroId, conteudo, ImagemCapa.TIPO, OrigemCapa.MANUAL));
        log.info("Capa do livro {} enviada à mão", livroId);
        return livroService.resposta(livro);

    }

    /** Também marca a busca como feita, para a tarefa não trazer a capa de volta. */
    @Transactional
    public LivroResponse remover(Long livroId) {

        Livro livro = livroService.livroDoUsuario(livroId);
        capaRepository.apagarDoLivro(livroId);
        livro.setCapaBuscada(LocalDateTime.now(ZoneOffset.UTC));
        log.info("Capa do livro {} removida", livroId);
        return livroService.resposta(livro);

    }

    /**
     * Grava o resultado de uma busca; sem imagem, só registra que tentou. A transação é
     * aberta aqui porque {@link #buscar} chama este método de dentro da classe, onde o
     * {@code @Transactional} não teria efeito, e a busca na internet fica fora dela.
     */
    public void gravarBusca(Long livroId, BuscadorCapas.Imagem imagem) {

        transactionTemplate.executeWithoutResult(status -> livroRepository.findById(livroId).ifPresent(livro -> {
            livro.setCapaBuscada(LocalDateTime.now(ZoneOffset.UTC));
            if (imagem != null) {
                trocar(livroId, new Capa(livroId, imagem.conteudo(), imagem.tipo(), imagem.origem()));
                log.info("Capa do livro {} achada em {}", livroId, imagem.origem());
            }
        }));

    }

    /** Os dados da busca, lidos do banco: a tarefa roda fora de uma transação. */
    public Optional<BuscadorCapas.Consulta> consulta(Long livroId) {
        return livroRepository.findComAutoresById(livroId).map(CapaService::consulta);
    }

    /** O livro precisa vir com autores e editora carregados. */
    private static BuscadorCapas.Consulta consulta(Livro livro) {
        return new BuscadorCapas.Consulta(livro.getIsbn(), livro.getTitulo(),
                livro.getAutores().stream().map(a -> a.getNome()).findFirst().orElse(null),
                livro.getEditora() == null ? null : livro.getEditora().getNome());
    }

    private void trocar(Long livroId, Capa nova) {
        capaRepository.apagarDoLivro(livroId);
        capaRepository.save(nova);
    }

}
