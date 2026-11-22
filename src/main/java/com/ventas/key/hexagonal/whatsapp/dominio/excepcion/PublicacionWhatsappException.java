package com.ventas.key.hexagonal.whatsapp.dominio.excepcion;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Regla de publicaciones de WhatsApp rota. El controller la devuelve como 404 (no existe) o 409 (estado que no lo permite),
 * con este mensaje.
 */
public class PublicacionWhatsappException extends RuntimeException {

    private final boolean noExiste;

    private PublicacionWhatsappException(String mensaje, boolean noExiste) {
        super(mensaje);
        this.noExiste = noExiste;
    }

    public boolean noExiste() {
        return noExiste;
    }

    public static PublicacionWhatsappException noSeEncuentra(int id) {
        return new PublicacionWhatsappException("No existe la publicacion de WhatsApp " + id, true);
    }

    public static PublicacionWhatsappException sinImagen(int id) {
        return new PublicacionWhatsappException("La publicacion " + id + " no tiene imagen: se publica solo el texto", true);
    }

    public static PublicacionWhatsappException estadoNoPermite(String accion, String estado) {
        return new PublicacionWhatsappException("No se puede " + accion + " una publicacion que esta " + estado, false);
    }
}
