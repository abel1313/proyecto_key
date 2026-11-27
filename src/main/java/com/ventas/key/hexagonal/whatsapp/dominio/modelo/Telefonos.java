package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/** Teléfonos mexicanos: Meta manda el número con o sin el «1» después del 52, y el sistema guarda 10 dígitos. */
public final class Telefonos {

    private Telefonos() {
    }

    /** Los 10 dígitos del número, o null si no se puede sacar uno válido. */
    public static String diezDigitos(String telefono) {
        if (telefono == null) {
            return null;
        }
        String d = telefono.replaceAll("\\D", "");
        if (d.length() == 13 && d.startsWith("521")) {
            d = d.substring(3);
        } else if (d.length() == 12 && d.startsWith("52")) {
            d = d.substring(2);
        }
        return d.matches("\\d{10}") ? d : null;
    }

    /** Liga directa a ese WhatsApp, con el número tal como llegó de Meta. */
    public static String ligaDeWhatsapp(String telefonoDeMeta) {
        String d = telefonoDeMeta == null ? "" : telefonoDeMeta.replaceAll("\\D", "");
        return "https://wa.me/" + d;
    }
}
