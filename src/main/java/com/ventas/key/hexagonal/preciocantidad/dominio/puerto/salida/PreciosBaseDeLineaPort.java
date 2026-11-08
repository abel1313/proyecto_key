package com.ventas.key.hexagonal.preciocantidad.dominio.puerto.salida;

import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.OrigenDePrecio;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * [Hexagonal: Driven Port] El precio que tenía una línea de pedido ANTES de cobrarse con precio de grupo. Solo existe para las líneas que se
 * cobraron con precio de grupo; sirve para regresarlas a ese precio si al editar el pedido el grupo deja de juntar el mínimo.
 */
public interface PreciosBaseDeLineaPort {

    /** Por id de línea y de ESE origen; las demás no aparecen. Vacío si la tabla todavía no existe. */
    Map<Integer, BigDecimal> leer(Collection<Integer> detalleIds, OrigenDePrecio origen);

    /** Guarda (o reemplaza) el precio sin descuento de cada línea, con su origen. No hace nada si la tabla todavía no existe. */
    void guardar(Map<Integer, BigDecimal> preciosSinDescuento, OrigenDePrecio origen);

    /** Borra la marca de estas líneas, sea cual sea su origen (ya no se cobran con descuento). */
    void borrar(Collection<Integer> detalleIds);

    /** Solo las de grupo de precio por cantidad: lo que usa el recálculo al editar un pedido (las de campaña no se tocan ahí). */
    default Map<Integer, BigDecimal> leer(Collection<Integer> detalleIds) {
        return leer(detalleIds, OrigenDePrecio.GRUPO);
    }

    default void guardar(Map<Integer, BigDecimal> preciosSinGrupo) {
        guardar(preciosSinGrupo, OrigenDePrecio.GRUPO);
    }
}
