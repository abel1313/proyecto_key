package com.ventas.key.hexagonal.campana.dominio.modelo;

import java.math.BigDecimal;
import java.util.List;

/**
 * [Hexagonal: Domain] Lo que la venta le pregunta a la campaña: el código que escribió o escaneó (puede ser vacío), si el administrador marcó
 * «Cliente recurrente», si el cliente aceptó el aviso de privacidad, cómo se cobra, quién es el cliente y qué lleva.
 */
public record SolicitudDeCampana(String codigo, boolean recurrenteManual, boolean aceptaPrivacidad, FormaDeCobro cobro,
                                 ContactoCliente contacto, List<Linea> lineas) {

    /** Una línea de la venta: producto, cuántas piezas y el precio que ya llevaba (normal, rebaja o grupo). */
    public record Linea(int productoId, int cantidad, BigDecimal precioBase) {
    }

    public SolicitudDeCampana {
        lineas = lineas == null ? List.of() : List.copyOf(lineas);
        cobro = cobro == null ? FormaDeCobro.CONTADO : cobro;
    }
}
