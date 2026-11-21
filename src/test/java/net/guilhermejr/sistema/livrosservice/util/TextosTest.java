package net.guilhermejr.sistema.livrosservice.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TextosTest {

    @Test
    void tiraEspacosDasPontasEJuntaOsRepetidos() {
        assertEquals("Edgar Allan Poe: Medo Clássico", Textos.limpar("  Edgar Allan Poe:  Medo\tClássico "));
    }

    @Test
    void vazioViraNull() {
        assertNull(Textos.limpar("   "));
        assertNull(Textos.limpar(null));
    }

}
