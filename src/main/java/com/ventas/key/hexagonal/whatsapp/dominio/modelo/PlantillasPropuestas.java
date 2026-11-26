package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.util.List;

/**
 * Plantillas de seguimiento propuestas para mandar a aprobación en Meta (ver
 * {@code WHATSAPP_PLANTILLAS.md}). Hasta que Meta las apruebe y la cuenta tenga método de pago,
 * enviarlas falla y queda registrado. Los nombres deben coincidir EXACTO con los de Meta.
 *
 * <p>La categoría es la que se pide; Meta decide la final y puede reclasificarla.</p>
 */
public final class PlantillasPropuestas {

    public record Propuesta(String nombre, String idioma, String categoria, String cuerpo,
                            List<String> variables, String pie) {
    }

    public static final Propuesta SEGUIMIENTO_PRODUCTO = new Propuesta(
            "seguimiento_producto", "es_MX", "UTILITY",
            "Hola {{1}} 👋 Te escribimos de Novedades Jade. Hace poco nos preguntaste por {{2}}. "
                    + "¿Sigues interesada(o)? Con gusto resolvemos tus dudas. Responde a este mensaje para continuar 💖",
            List.of("nombre del cliente", "producto por el que preguntó"),
            "Novedades Jade");

    public static final Propuesta PROMOCION = new Propuesta(
            "promocion_novedades_jade", "es_MX", "MARKETING",
            "¡Hola {{1}}! 💖 En Novedades Jade tenemos una promoción para ti: {{2}}. "
                    + "Conócela en https://shop.novedades-jade.com.mx/ o responde a este mensaje y te ayudamos con tu pedido.",
            List.of("nombre del cliente", "texto corto de la promoción"),
            "Si ya no quieres recibir mensajes, responde BAJA");

    public static final List<Propuesta> TODAS = List.of(SEGUIMIENTO_PRODUCTO, PROMOCION);

    private PlantillasPropuestas() {
    }
}
