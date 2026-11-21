package net.guilhermejr.sistema.livrosservice.api.response;

import lombok.Builder;
import lombok.Getter;

import net.guilhermejr.sistema.livrosservice.domain.enums.OrigemCapa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class LivroResponse {

    private Long id;
    private String isbn;
    private String titulo;
    private String subtitulo;
    private Integer anoPublicacao;
    private Integer paginas;
    private String editora;
    private List<String> autores;
    private String colecao;
    private String tradutor;
    private ItemResponse pais;
    private ItemResponse genero;
    private ItemResponse formato;
    private String estante;
    /** Id da imagem em GET /capas/{id}; null quando o livro não tem capa. */
    private UUID capa;
    /** GOOGLE_BOOKS_TITULO = achada pelo título, pode ser de outra edição. */
    private OrigemCapa capaOrigem;
    /** Última busca automática de capa; null quando ainda não foi feita. */
    private LocalDateTime capaBuscada;
    private LocalDateTime criado;
    private LocalDateTime atualizado;

}
