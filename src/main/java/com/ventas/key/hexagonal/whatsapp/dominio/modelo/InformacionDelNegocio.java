package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.math.BigDecimal;

/**
 * Lo que el negocio tiene capturado en Sistema → Negocio y contactos y que el bot puede decir tal cual
 * (regla R22): dirección, punto exacto en el mapa y los links de sus redes. Todo es opcional: un dato que
 * no existe NO se menciona ni se inventa.
 *
 * <p>Java puro. El horario no va aquí: ya tiene su propio modelo ({@link HorarioDeAtencion}).</p>
 */
public record InformacionDelNegocio(String direccion, Double latitud, Double longitud,
                                    String whatsappUrl, String facebookUrl, String instagramUrl, String tiktokUrl) {

    /** Tope de largo: lo que se escribe en Negocio llega al prompt y a respuestas públicas. */
    static final int MAX_DIRECCION = 200;
    static final int MAX_LINK = 300;

    private static final InformacionDelNegocio VACIA = new InformacionDelNegocio(null, null, null, null, null, null, null);

    /** Sin ningún dato (el negocio no los capturó o no se pudieron leer). */
    public static InformacionDelNegocio vacia() {
        return VACIA;
    }

    /** La dirección en una sola línea y sin espacios de más, o {@code null} si no hay. */
    public String direccionLimpia() {
        String limpia = limpiar(direccion);
        return limpia != null && limpia.length() > MAX_DIRECCION ? limpia.substring(0, MAX_DIRECCION) : limpia;
    }

    /**
     * Liga de Google Maps al punto exacto, o {@code null} si falta alguna coordenada o no es válida
     * (la latitud va de -90 a 90 y la longitud de -180 a 180; (0, 0) se toma como «sin capturar»).
     */
    public String ligaDelMapa() {
        if (latitud == null || longitud == null || latitud.isNaN() || longitud.isNaN()
                || latitud.isInfinite() || longitud.isInfinite()
                || latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180
                || (latitud == 0 && longitud == 0)) {
            return null;
        }
        return "https://www.google.com/maps/search/?api=1&query="
                + BigDecimal.valueOf(latitud).toPlainString() + "," + BigDecimal.valueOf(longitud).toPlainString();
    }

    /** ¿Hay dirección o punto en el mapa? Si no, el bot manda a la tienda en línea a ver la ubicación. */
    public boolean tieneUbicacion() {
        return direccionLimpia() != null || ligaDelMapa() != null;
    }

    public String whatsappLimpio() {
        return link(whatsappUrl);
    }

    public String facebookLimpio() {
        return link(facebookUrl);
    }

    public String instagramLimpio() {
        return link(instagramUrl);
    }

    public String tiktokLimpio() {
        return link(tiktokUrl);
    }

    /** Un link solo si empieza con https://, no trae espacios y no es larguísimo (nada de «javascript:» ni instrucciones). */
    private static String link(String texto) {
        String limpio = limpiar(texto);
        if (limpio == null || limpio.length() > MAX_LINK || limpio.contains(" ")
                || !limpio.regionMatches(true, 0, "https://", 0, 8)) {
            return null;
        }
        return limpio;
    }

    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.replaceAll("\\s+", " ").trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
