package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

/**
 * [Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>El chatbot (IA). Devuelve el texto crudo, con sus marcas (##ESCALAR##, ##GRACIAS##). Lanza excepcion si falla o tarda.
 */
public interface CerebroWhatsappPort {

    /** Contesta SOLO sobre el producto de la publicacion; todo lo demas lo escala. */
    String responderSobreProducto(String texto, int varianteId, boolean enGrupo);

    /** Chat privado sin publicacion de por medio: contesta con todo el catalogo. */
    String responderMensajeDirecto(String texto, boolean esPrimeraVez);
}
