package com.ventas.key.mis.productos.Utils;

import java.util.Locale;
import java.util.Map;

/**
 * Alinea el nombre del archivo con el formato REAL de sus bytes antes de mandarlo a
 * micro_imagenes.
 *
 * El micro valida la subida comparando los magic bytes contra la extension del nombre
 * (ValidadorImagenSubida) y responde 400 si no coinciden. El front recorta/reencoda la foto en
 * un canvas, que siempre saca JPEG, pero conserva el nombre original del archivo que eligio el
 * usuario: una foto "logo.png" llega con bytes JPEG y nombre .png, y el micro la rechaza.
 *
 * Renombrar por los bytes tambien arregla el Content-Type de la parte: Spring lo deduce de la
 * extension del filename al escribir el multipart, asi que con el nombre correcto la parte sale
 * declarada como lo que el archivo es de verdad.
 *
 * Si los bytes no son ninguno de los formatos que el micro acepta, el nombre se deja igual para
 * que el rechazo venga del micro con su mensaje, en vez de inventarle una extension aqui.
 */
public final class NombreArchivoImagen {

    private NombreArchivoImagen() {
    }

    /** Formato real -> extension canonica con la que el micro lo acepta. */
    private static final Map<String, String> EXTENSION_POR_FORMATO = Map.of(
            "jpeg", "jpg",
            "png", "png",
            "gif", "gif",
            "webp", "webp"
    );

    /** Extensiones que ya son validas para un formato, para no renombrar de gratis. */
    private static final Map<String, java.util.Set<String>> EXTENSIONES_VALIDAS = Map.of(
            "jpeg", java.util.Set.of("jpg", "jpeg"),
            "png", java.util.Set.of("png"),
            "gif", java.util.Set.of("gif"),
            "webp", java.util.Set.of("webp")
    );

    private static final String NOMBRE_POR_DEFECTO = "imagen";

    /**
     * @param nombreOriginal nombre tal como llego del front (puede venir null o sin extension)
     * @param bytes contenido del archivo
     * @return el mismo nombre si su extension ya concuerda con los bytes; si no, el nombre con la
     *         extension corregida
     */
    public static String normalizar(String nombreOriginal, byte[] bytes) {
        String nombre = (nombreOriginal == null || nombreOriginal.isBlank())
                ? NOMBRE_POR_DEFECTO
                : nombreOriginal.trim();

        String formatoReal = detectarFormato(bytes);
        if (formatoReal == null) {
            return nombre;
        }

        int punto = nombre.lastIndexOf('.');
        String base = (punto > 0) ? nombre.substring(0, punto) : nombre;
        String extensionActual = (punto > 0 && punto < nombre.length() - 1)
                ? nombre.substring(punto + 1).toLowerCase(Locale.ROOT)
                : "";

        if (EXTENSIONES_VALIDAS.get(formatoReal).contains(extensionActual)) {
            return nombre;
        }
        return base + "." + EXTENSION_POR_FORMATO.get(formatoReal);
    }

    /** Misma deteccion que ValidadorImagenSubida del micro. null = formato no aceptado. */
    private static String detectarFormato(byte[] b) {
        if (b == null) return null;
        if (empiezaCon(b, 0xFF, 0xD8, 0xFF)) return "jpeg";
        if (empiezaCon(b, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return "png";
        if (empiezaCon(b, 'G', 'I', 'F', '8') && b.length >= 6
                && (b[4] == '7' || b[4] == '9') && b[5] == 'a') return "gif";
        if (empiezaCon(b, 'R', 'I', 'F', 'F') && b.length >= 12
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        return null;
    }

    private static boolean empiezaCon(byte[] datos, int... firma) {
        if (datos.length < firma.length) return false;
        for (int i = 0; i < firma.length; i++) {
            if ((datos[i] & 0xFF) != (firma[i] & 0xFF)) return false;
        }
        return true;
    }
}
