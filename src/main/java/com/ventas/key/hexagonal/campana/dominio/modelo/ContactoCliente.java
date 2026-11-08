package com.ventas.key.hexagonal.campana.dominio.modelo;

/**
 * [Hexagonal: Domain] Los datos de contacto del cliente de la venta, ya normalizados: correo en minúsculas y teléfono con los últimos 10 dígitos
 * (o vacío). Sirve para comprobar que un código es de ESA persona.
 */
public record ContactoCliente(String nombre, String correo, String telefono) {

    public ContactoCliente {
        nombre = nombre == null ? "" : nombre.trim();
        correo = correo == null || correo.isBlank() ? null : correo.trim().toLowerCase();
        telefono = ultimos10(telefono);
    }

    public boolean tieneDatos() {
        return correo != null || !telefono.isEmpty();
    }

    public static String ultimos10(String telefono) {
        if (telefono == null) {
            return "";
        }
        String digitos = telefono.replaceAll("\\D", "");
        return digitos.length() > 10 ? digitos.substring(digitos.length() - 10) : digitos;
    }
}
