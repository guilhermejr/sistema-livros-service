package net.guilhermejr.sistema.livrosservice.api.response;

/** Um item de lista: país, gênero, formato, autor, editora ou estante. */
public record ItemResponse(Long id, String nome) {
}
