package net.guilhermejr.sistema.livrosservice.api.response;

import java.util.List;

/** O que o formulário do livro oferece: listas fixas e os cadastros do usuário. */
public record OpcoesResponse(
        List<ItemResponse> paises,
        List<ItemResponse> generos,
        List<ItemResponse> formatos,
        List<ItemResponse> autores,
        List<ItemResponse> editoras,
        List<ItemResponse> estantes) {
}
