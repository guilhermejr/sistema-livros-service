package net.guilhermejr.sistema.livrosservice.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuscadorDadosIsbnTest {

    // Casos reais: a CBL traz título e editora, mas sem autor, ano e páginas; a Open Library completa.
    @Test
    void cadaCampoVemDaPrimeiraFonteQueOTem() {

        BuscadorDadosIsbn.Dados cbl = new BuscadorDadosIsbn.Dados("BrasilAPI", "Androides sonham com ovelhas elétricas?", null,
                List.of(), "Editora Aleph", null, 0);
        BuscadorDadosIsbn.Dados openLibrary = new BuscadorDadosIsbn.Dados("Open Library", "Androides Sonham com Ovelhas Elétricas?", null,
                List.of("Philip K. Dick"), "ALEPH", 2014, null);

        BuscadorDadosIsbn.Dados juntos = BuscadorDadosIsbn.juntar(List.of(cbl, openLibrary));

        assertEquals("Androides sonham com ovelhas elétricas?", juntos.titulo());
        assertEquals(List.of("Philip K. Dick"), juntos.autores());
        assertEquals("Editora Aleph", juntos.editora());
        assertEquals(2014, juntos.ano());
        assertNull(juntos.paginas());
        assertEquals("BrasilAPI, Open Library", juntos.fonte());

    }

    @Test
    void tituloTodoEmMaiusculasEEditoraNaoDefinida() {

        BuscadorDadosIsbn.Dados juntos = BuscadorDadosIsbn.juntar(List.of(
                new BuscadorDadosIsbn.Dados("BrasilAPI", "POEIRA DAS ESTRELAS", null, null, "Não definido", null, null)));

        assertEquals("Poeira das estrelas", juntos.titulo());
        assertNull(juntos.editora());

    }

    @Test
    void anoDentroDeQualquerData() {
        assertEquals(2014, BuscadorDadosIsbn.ano("Aug 22, 2014"));
        assertEquals(2017, BuscadorDadosIsbn.ano("2017-06-15"));
        assertNull(BuscadorDadosIsbn.ano("s.d."));
    }

}
