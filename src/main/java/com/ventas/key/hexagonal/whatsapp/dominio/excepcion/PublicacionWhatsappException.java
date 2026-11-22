package com.ventas.key.hexagonal.whatsapp.dominio.excepcion;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Regla de publicaciones de WhatsApp rota. El controller la devuelve como 404 (no existe), 400 (dato que falta o no sirve) o
 * 409 (estado que no lo permite), con este mensaje.
 */
public class PublicacionWhatsappException extends RuntimeException {

    private final boolean noExiste;
    private final boolean invalida;

    private PublicacionWhatsappException(String mensaje, boolean noExiste, boolean invalida) {
        super(mensaje);
        this.noExiste = noExiste;
        this.invalida = invalida;
    }

    public boolean noExiste() {
        return noExiste;
    }

    /** Un dato de la peticion falta o no sirve (400). */
    public boolean invalida() {
        return invalida;
    }

    /** Lo que se pidio no se puede aceptar tal cual: el mensaje esta en palabras del dueño. */
    public static PublicacionWhatsappException invalida(String mensaje) {
        return new PublicacionWhatsappException(mensaje, false, true);
    }

    public static PublicacionWhatsappException noSeEncuentra(int id) {
        return new PublicacionWhatsappException("No existe la publicacion de WhatsApp " + id, true, false);
    }

    public static PublicacionWhatsappException sinImagen(int id) {
        return new PublicacionWhatsappException("La publicacion " + id + " no tiene imagen: se publica solo el texto", true, false);
    }

    public static PublicacionWhatsappException estadoNoPermite(String accion, String estado) {
        return new PublicacionWhatsappException("No se puede " + accion + " una publicacion que esta " + estado, false, false);
    }
}
