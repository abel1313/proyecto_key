package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>El texto de una publicacion se arma con una plantilla, SIN IA (gratis y predecible; el admin lo puede editar). La primera linea lleva
 * el codigo de referencia (J42): cuando alguien responde (cita) el mensaje, WhatsApp solo muestra las primeras lineas, y de ese codigo
 * se sabe de que publicacion hablan (PLAN_WHATSAPP_PUBLICACIONES.md, seccion 5).
 */
public final class PlantillaDeTextoWhatsapp {

    public static final String MARCA = "Novedades Jade";

    private PlantillaDeTextoWhatsapp() {
    }

    /** El cuerpo automatico: nombre del articulo y precio (sin tallas ni stock exacto: eso no se anuncia). */
    public static String cuerpoDe(String nombre, Double precio) {
        StringBuilder sb = new StringBuilder(nombre == null ? "" : nombre.trim());
        if (precio != null && precio > 0) {
            sb.append("\n$").append(Math.round(precio)).append(" MXN");
        }
        return sb.toString();
    }

    /** Cabecera con el codigo + cuerpo + invitacion a responder. El codigo va al INICIO a proposito. */
    public static String armar(String codigo, String cuerpo) {
        return "*" + MARCA + " · " + codigo + "*\n" + cuerpo.trim()
                + "\nResponde a este mensaje o escribe " + codigo + " para apartarlo";
    }
}
