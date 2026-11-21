package net.guilhermejr.sistema.livrosservice.util;

import java.text.Normalizer;
import java.util.Locale;

public final class Textos {

    private Textos() {
    }

    /** Tira os espaços das pontas e junta os repetidos; vazio vira null. */
    public static String limpar(String texto) {

        if (texto == null) {
            return null;
        }
        String limpo = texto.strip().replaceAll("\\s+", " ");
        return limpo.isEmpty() ? null : limpo;

    }

    /** Para comparar nomes: sem acento, minúsculo, pontuação vira espaço. */
    public static String normalizar(String texto) {

        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .strip();

    }

    /** "POEIRA DAS ESTRELAS" vira "Poeira das estrelas"; texto com minúsculas fica como está. */
    public static String semCaixaAlta(String texto) {

        if (texto == null || !texto.equals(texto.toUpperCase(Locale.ROOT)) || texto.equals(texto.toLowerCase(Locale.ROOT))) {
            return texto;
        }
        String minusculo = texto.toLowerCase(Locale.forLanguageTag("pt-BR"));
        return minusculo.substring(0, 1).toUpperCase(Locale.ROOT) + minusculo.substring(1);

    }

}
