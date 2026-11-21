package net.guilhermejr.sistema.livrosservice.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuscadorCapasTest {

    private static BuscadorCapas.Volumes.Item volume(String titulo, String editora, String miniatura) {
        return new BuscadorCapas.Volumes.Item(new BuscadorCapas.Volumes.VolumeInfo(titulo, editora,
                miniatura == null ? null : new BuscadorCapas.Volumes.ImageLinks(miniatura, null)));
    }

    @Test
    void soPesquisaIsbn10Ou13() {

        assertTrue(BuscadorCapas.isbnPesquisavel("9788535931624"));
        assertTrue(BuscadorCapas.isbnPesquisavel("853593162X"));
        assertFalse(BuscadorCapas.isbnPesquisavel("62"));
        assertFalse(BuscadorCapas.isbnPesquisavel("86908194"));
        assertFalse(BuscadorCapas.isbnPesquisavel(null));

    }

    @Test
    void usaAPrimeiraMiniaturaEmHttpsESemDobra() {

        BuscadorCapas.Volumes volumes = new BuscadorCapas.Volumes(List.of(
                volume("Sem capa", null, null),
                volume("Com capa", null, "http://books.google.com/books/content?id=abc&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api")));

        assertEquals("https://books.google.com/books/content?id=abc&printsec=frontcover&img=1&zoom=1&source=gbs_api",
                BuscadorCapas.enderecoGoogle(volumes));

    }

    @Test
    void semVolumesNaoHaEndereco() {
        assertNull(BuscadorCapas.enderecoGoogle(new BuscadorCapas.Volumes(null)));
        assertNull(BuscadorCapas.enderecoGoogle(null));
    }

    @Test
    void tituloBaseCortaSubtituloEVolume() {
        assertEquals("Contos", BuscadorCapas.tituloBase("Contos - Machado De Assis"));
        assertEquals("Edgar Allan Poe", BuscadorCapas.tituloBase("Edgar Allan Poe: Medo Clássico"));
        assertEquals("1984", BuscadorCapas.tituloBase("1984"));
    }

    // Casos da amostra real: os acertos passam, os livros errados ficam de fora.
    @Test
    void tituloPrecisaBater() {

        assertTrue(BuscadorCapas.mesmoTitulo("O castelo - Grandes obras de Franz Kafka", "O Castelo"));
        assertTrue(BuscadorCapas.mesmoTitulo("OS MISERÁVEIS", "Os Miseraveis"));
        assertTrue(BuscadorCapas.mesmoTitulo("Cidadã de segunda classe", "Cidadã de Segunda Classe"));
        assertFalse(BuscadorCapas.mesmoTitulo("24 contos de F. Scott Fitzgerald", "Eu Morreria Por Ti & Outras Histórias"));
        assertFalse(BuscadorCapas.mesmoTitulo("Dois sonhos", "Crônicas de Petersburgo"));
        assertFalse(BuscadorCapas.mesmoTitulo("Geografia de Mato Grosso", "Geografia do Brasil"));
        assertFalse(BuscadorCapas.mesmoTitulo("Castelos de areia", "O Castelo"));

    }

    @Test
    void preferePelaMesmaEditora() {

        BuscadorCapas.Volumes volumes = new BuscadorCapas.Volumes(List.of(
                volume("Os primos", "Outra Casa", "http://x/outra"),
                volume("Os primos", "Editora Record", "http://x/record")));

        assertEquals("https://x/record", BuscadorCapas.enderecoGooglePorTitulo(volumes, "Os Primos", "Galera Record"));
        assertEquals("https://x/outra", BuscadorCapas.enderecoGooglePorTitulo(volumes, "Os Primos", "Intrínseca"));
        assertNull(BuscadorCapas.enderecoGooglePorTitulo(volumes, "Os Primos de Longe", "Galera Record"));

    }

    @Test
    void palavrasGenericasNaoContamComoMesmaEditora() {
        assertFalse(BuscadorCapas.mesmaEditora("Editora Intrinseca", "Editora Record"));
        assertTrue(BuscadorCapas.mesmaEditora("Editora Alto Astral Ltda", "Astral Cultural"));
    }

}
