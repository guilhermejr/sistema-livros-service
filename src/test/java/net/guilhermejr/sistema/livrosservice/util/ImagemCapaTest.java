package net.guilhermejr.sistema.livrosservice.util;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ImagemCapaTest {

    private static byte[] png(int largura, int altura) throws IOException {
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "png", saida);
        return saida.toByteArray();
    }

    private static BufferedImage ler(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    @Test
    void reduzImagemGrandeMantendoAProporcao() throws IOException {

        BufferedImage convertida = ler(ImagemCapa.converter(png(1200, 1800)));
        assertEquals(400, convertida.getWidth());
        assertEquals(600, convertida.getHeight());

    }

    @Test
    void naoAumentaImagemPequena() throws IOException {
        assertEquals(128, ler(ImagemCapa.converter(png(128, 190))).getWidth());
    }

    @Test
    void transparenciaViraFundoBranco() throws IOException {

        BufferedImage convertida = ler(ImagemCapa.converter(png(10, 10)));
        assertEquals(0xFFFFFF, convertida.getRGB(5, 5) & 0xFFFFFF, 0x050505);

    }

    @Test
    void recusaOQueNaoEImagem() {
        assertNull(ImagemCapa.converter("%PDF-1.7".getBytes()));
    }

}
