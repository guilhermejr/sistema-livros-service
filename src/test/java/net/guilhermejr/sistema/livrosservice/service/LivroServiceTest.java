package net.guilhermejr.sistema.livrosservice.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class LivroServiceTest {

    // Nomes da CBL ao lado dos que já estão cadastrados.
    @Test
    void editoraIgualSemAsPalavrasGenericas() {

        assertEquals(LivroService.chaveEditora("Antofágica"), LivroService.chaveEditora("Editora Antofágica LTDA"));
        assertEquals(LivroService.chaveEditora("Harper Collins"), LivroService.chaveEditora("HarperCollins Brasil"));
        assertEquals(LivroService.chaveEditora("Verus"), LivroService.chaveEditora("Verus Editora"));
        assertEquals(LivroService.chaveEditora("Globo Livros"), LivroService.chaveEditora("Editora Globo"));
        assertNotEquals(LivroService.chaveEditora("Record"), LivroService.chaveEditora("Galera Record"));

    }

}
