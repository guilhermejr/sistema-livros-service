package net.guilhermejr.sistema.livrosservice.api.response;

import java.util.List;

/**
 * O que as fontes sabem de um ISBN, para pré-preencher o cadastro. Editora e autores
 * já vêm com o nome que o usuário usa quando batem com um cadastro dele.
 *
 * @param fontes          de onde veio algum dado; vazia quando nenhuma fonte conhece o ISBN
 * @param jaCadastrados   livros do usuário com o mesmo ISBN (volumes podem repetir)
 */
public record DadosIsbnResponse(
        String isbn,
        List<String> fontes,
        String titulo,
        String subtitulo,
        List<String> autores,
        String editora,
        Integer anoPublicacao,
        Integer paginas,
        List<ItemResponse> jaCadastrados) {
}
