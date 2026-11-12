package com.ventas.key.hexagonal.campana.dominio.modelo;

import com.ventas.key.hexagonal.campana.dominio.excepcion.CampanaInvalidaException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * [Hexagonal: Domain] [Clean: Entity] Java puro. Un producto de la campaña y el precio al que se cobra cada pieza a quien es recurrente.
 * {@code nombre} y {@code codigoBarras} son solo para mostrar (el código identifica al juguete en la pantalla de Campañas; puede ser null).
 * {@code precioVenta} y {@code precioRebaja} (el precio con descuento del producto) tambien son solo para mostrar: no se guardan, salen del catalogo; {@code stock} tambien (cuantas piezas hay hoy; no reserva nada).
 */
public record PrecioDeCampana(int productoId, String nombre, String codigoBarras, BigDecimal precio, Double precioVenta, Double precioRebaja, Integer stock) {

    public PrecioDeCampana(int productoId, String nombre, BigDecimal precio) {
        this(productoId, nombre, null, precio, null, null, null);
    }

    public PrecioDeCampana(int productoId, String nombre, String codigoBarras, BigDecimal precio) {
        this(productoId, nombre, codigoBarras, precio, null, null, null);
    }

    public PrecioDeCampana(int productoId, String nombre, String codigoBarras, BigDecimal precio, Double precioVenta, Double precioRebaja) {
        this(productoId, nombre, codigoBarras, precio, precioVenta, precioRebaja, null);
    }

    public PrecioDeCampana {
        if (productoId <= 0) {
            throw new CampanaInvalidaException("Falta elegir el producto de uno de los precios de campaña.");
        }
        // Se redondea a centavos ANTES de validar: 0.004 se guardaría como $0.00 (gratis) y debe rechazarse como «mayor a cero».
        precio = precio == null ? null : precio.setScale(2, RoundingMode.HALF_UP);
        if (precio == null || precio.signum() <= 0) {
            throw new CampanaInvalidaException("El precio de campaña de " + (nombre == null ? "el producto " + productoId : "«" + nombre + "»")
                    + " debe ser mayor a cero.");
        }
        if (precio.compareTo(Campana.MONTO_MAXIMO) > 0) {
            throw new CampanaInvalidaException("El precio de campaña de " + (nombre == null ? "el producto " + productoId : "«" + nombre + "»")
                    + " es demasiado grande.");
        }
    }
}
