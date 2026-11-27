package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/**
 * Lo que ESTE mismo número de WhatsApp dio en un pedido anterior hecho por el bot (regla R17). Al modelo solo se le
 * muestra enmascarado; el dato real lo pone el sistema cuando el cliente confirma que lo quiere usar.
 *
 * @param direccion puede ser null
 */
public record DatosConocidos(String nombre, String correo, String direccion) {

    /** {@code ana@correo.com} → {@code a***@correo.com}. */
    public String correoEnmascarado() {
        if (correo == null) {
            return "";
        }
        int arroba = correo.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        return correo.charAt(0) + "***" + correo.substring(arroba);
    }

    /** Solo el inicio de la dirección: «Calle Hid***». */
    public String direccionParcial() {
        if (direccion == null || direccion.isBlank()) {
            return "";
        }
        String d = direccion.trim();
        return (d.length() <= 6 ? d.substring(0, Math.max(1, d.length() / 2)) : d.substring(0, 6)) + "***";
    }
}
