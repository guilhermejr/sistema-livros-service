package net.guilhermejr.sistema.livrosservice.util;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Converte a imagem enviada à mão num JPEG de até {@link #LARGURA_MAXIMA} px de largura.
 * Uma foto de celular tem vários MB; a capa aparece pequena na lista e no formulário.
 */
public final class ImagemCapa {

    public static final int LARGURA_MAXIMA = 400;
    public static final String TIPO = "image/jpeg";

    private ImagemCapa() {
    }

    /** Devolve null quando os bytes não são uma imagem que o Java sabe ler (JPEG, PNG, GIF, BMP). */
    public static byte[] converter(byte[] original) {

        BufferedImage imagem;
        try {
            imagem = ImageIO.read(new ByteArrayInputStream(original));
        } catch (IOException | RuntimeException e) {
            return null;
        }
        if (imagem == null || imagem.getWidth() < 1 || imagem.getHeight() < 1) {
            return null;
        }

        int largura = Math.min(imagem.getWidth(), LARGURA_MAXIMA);
        int altura = Math.max(1, Math.round(imagem.getHeight() * (largura / (float) imagem.getWidth())));

        // Fundo branco: PNG com transparência ficaria preto no JPEG.
        BufferedImage destino = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, largura, altura);
        g.drawImage(imagem, 0, 0, largura, altura, null);
        g.dispose();

        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpg").next();
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(saida)) {
            escritor.setOutput(stream);
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(0.85f);
            escritor.write(null, new IIOImage(destino, null, null), parametros);
        } catch (IOException e) {
            return null;
        } finally {
            escritor.dispose();
        }
        return saida.toByteArray();

    }

}
