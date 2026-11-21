package net.guilhermejr.sistema.livrosservice.api.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * Editora, autores e estante vão pelo nome: um nome que o usuário ainda não tem
 * vira cadastro novo. País, gênero e formato são de listas fixas e vão pelo id.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LivroRequest {

    @Size(max = 20)
    @Pattern(regexp = "[0-9]*[0-9Xx]?", message = "deve ter só números (e X no fim)")
    private String isbn;

    @NotBlank
    @Size(max = 200)
    private String titulo;

    @Size(max = 300)
    private String subtitulo;

    @NotNull
    @Min(1000)
    @Max(2100)
    private Integer anoPublicacao;

    @NotNull
    @Min(1)
    @Max(100000)
    private Integer paginas;

    @NotBlank
    @Size(max = 100)
    private String editora;

    @NotEmpty
    private List<@NotBlank @Size(max = 200) String> autores;

    @NotNull
    private Long formatoId;

    /** Vazio = sem estante. */
    @Size(max = 100)
    private String estante;

}
