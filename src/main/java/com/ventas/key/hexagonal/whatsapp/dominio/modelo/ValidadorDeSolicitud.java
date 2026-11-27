package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Revisa los datos del cliente que trae la marca del pedido (regla R16): obligatorios y con formato válido. No toca
 * la base: qué artículo es y a cuánto sale lo decide el servicio de aplicación con lo que hay en la base.
 */
public final class ValidadorDeSolicitud {

    /** Tope por línea: cualquier cantidad es válida mientras haya stock; esto solo evita cifras absurdas del modelo. */
    static final int MAX_CANTIDAD = 999;
    /** Artículos distintos por pedido: el resumen tiene que caber en un mensaje de WhatsApp (4096 caracteres) con su pregunta. */
    public static final int MAX_LINEAS = 10;
    private static final int MIN_LETRAS = 3;
    private static final String CORREO = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private ValidadorDeSolicitud() {
    }

    public static List<Falta> faltas(SolicitudDePedido s, String telefonoDeWhatsapp) {
        List<Falta> faltas = new ArrayList<>();
        if (s.articulos().isEmpty() || s.articulos().stream().anyMatch(l -> l.articuloId() == null)) {
            faltas.add(Falta.ARTICULO);
        } else if (s.articulos().stream().map(LineaSolicitada::articuloId).distinct().count() > MAX_LINEAS) {
            faltas.add(Falta.LINEAS);
        } else if (s.articulos().stream().anyMatch(l -> l.cantidad() == null || l.cantidad() < 1 || l.cantidad() > MAX_CANTIDAD)) {
            faltas.add(Falta.CANTIDAD);
        }
        if (letras(s.nombre()) < MIN_LETRAS) {
            faltas.add(Falta.NOMBRE);
        }
        if (telefonoElegido(s, telefonoDeWhatsapp) == null) {
            faltas.add(Falta.TELEFONO);
        }
        // N2: un correo con «*» es el enmascarado que vio el modelo, no uno real: se pide de nuevo.
        if (s.correo() == null || s.correo().contains("*") || !s.correo().trim().matches(CORREO)) {
            faltas.add(Falta.CORREO);
        }
        if (letras(s.recibe()) < MIN_LETRAS) {
            faltas.add(Falta.RECIBE);
        }
        if (!s.recogerEnLocal()) {
            if (s.direccion() == null || s.direccion().trim().length() < 5) {
                faltas.add(Falta.DIRECCION);
            }
            if (s.lugar() == null || s.lugar().isBlank()) {
                faltas.add(Falta.LUGAR);
            }
        }
        return faltas;
    }

    /** Los 10 dígitos que se van a guardar: el de WhatsApp si el cliente confirmó usarlo, o el que dio. Null si no sirve. */
    public static String telefonoElegido(SolicitudDePedido s, String telefonoDeWhatsapp) {
        return Telefonos.diezDigitos(s.usarTelefonoDeWhatsapp() ? telefonoDeWhatsapp : s.telefono());
    }

    static long letras(String t) {
        return t == null ? 0 : t.codePoints().filter(Character::isLetter).count();
    }
}
