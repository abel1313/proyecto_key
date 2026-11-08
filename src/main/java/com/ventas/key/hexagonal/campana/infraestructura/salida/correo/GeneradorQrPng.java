package com.ventas.key.hexagonal.campana.infraestructura.salida.correo;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * [Hexagonal: Driven Adapter] Dibuja un QR en PNG (negro sobre blanco, con margen) para ir dentro del correo del código de campaña.
 * Solo usa el núcleo de zxing y el JDK ({@link BufferedImage} + {@link ImageIO}); no necesita pantalla.
 */
public final class GeneradorQrPng {

    private GeneradorQrPng() { }

    /** @param px lado aproximado de la imagen en píxeles (el QR se ajusta a módulos enteros). */
    public static byte[] png(String texto, int px) {
        // El contenedor no tiene pantalla: sin esto algunas JDK intentan cargar el AWT gráfico.
        System.setProperty("java.awt.headless", "true");
        try {
            BitMatrix m = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, px, px,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 4));
            BufferedImage img = new BufferedImage(m.getWidth(), m.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < m.getWidth(); x++) {
                for (int y = 0; y < m.getHeight(); y++) {
                    img.setRGB(x, y, m.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("No se pudo dibujar el QR: " + e.getMessage(), e);
        }
    }
}
