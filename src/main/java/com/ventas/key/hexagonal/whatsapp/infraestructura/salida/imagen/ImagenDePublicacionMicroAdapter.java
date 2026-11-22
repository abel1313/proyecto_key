package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.imagen;

import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ImagenDePublicacionPort;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenPort;
import com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks — servicio de imagenes]
 *
 * <p>Pide la imagen al micro de imagenes, la reduce (lado mayor 1280 px) y la comprime a JPEG calidad 0.80 (WhatsApp igual la
 * comprime). Son bytes, nunca pasan por el modelo de IA (cero tokens). Guarda el resultado en memoria por id (maximo
 * {@value #MAX_EN_CACHE}) porque el bot puede pedir la misma foto varias veces.
 */
@Slf4j
@Component
public class ImagenDePublicacionMicroAdapter implements ImagenDePublicacionPort {

    static final int LADO_MAXIMO = 1280;
    static final float CALIDAD = 0.80f;
    static final int MAX_EN_CACHE = 50;

    private final ImagenPort imagenes;
    private final Map<Long, byte[]> cache = new ConcurrentHashMap<>();

    public ImagenDePublicacionMicroAdapter(ImagenPort imagenes) {
        this.imagenes = imagenes;
    }

    @Override
    public Optional<byte[]> jpegLiviano(long imagenId) {
        byte[] enCache = cache.get(imagenId);
        if (enCache != null) {
            return Optional.of(enCache);
        }
        try {
            ImagenDto dto = imagenes.getOne(imagenId);
            if (dto == null || dto.getImagen() == null || dto.getImagen().length == 0) {
                return Optional.empty();
            }
            byte[] jpeg = aJpegLiviano(dto.getImagen());
            if (jpeg == null) {
                return Optional.empty();
            }
            if (cache.size() >= MAX_EN_CACHE) {
                cache.clear();
            }
            cache.put(imagenId, jpeg);
            return Optional.of(jpeg);
        } catch (Exception e) {
            log.warn("WhatsApp: no se pudo preparar la imagen {}: {}", imagenId, e.getMessage());
            return Optional.empty();
        }
    }

    /** Package-private para poder probarlo sin el micro. Devuelve null si los bytes no son una imagen que Java lea. */
    static byte[] aJpegLiviano(byte[] original) throws java.io.IOException {
        BufferedImage origen = ImageIO.read(new ByteArrayInputStream(original));
        if (origen == null) {
            return null;
        }
        int ancho = origen.getWidth();
        int alto = origen.getHeight();
        double escala = Math.min(1.0, (double) LADO_MAXIMO / Math.max(ancho, alto));
        int nuevoAncho = Math.max(1, (int) Math.round(ancho * escala));
        int nuevoAlto = Math.max(1, (int) Math.round(alto * escala));

        // RGB sin transparencia: JPEG no la soporta (un PNG con fondo transparente saldria negro).
        BufferedImage destino = new BufferedImage(nuevoAncho, nuevoAlto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, nuevoAncho, nuevoAlto);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(origen, 0, 0, nuevoAncho, nuevoAlto, null);
        } finally {
            g.dispose();
        }

        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam parametros = escritor.getDefaultWriteParam();
        parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parametros.setCompressionQuality(CALIDAD);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream flujo = new MemoryCacheImageOutputStream(salida)) {
            escritor.setOutput(flujo);
            escritor.write(null, new IIOImage(destino, null, null), parametros);
        } finally {
            escritor.dispose();
        }
        return salida.toByteArray();
    }
}
