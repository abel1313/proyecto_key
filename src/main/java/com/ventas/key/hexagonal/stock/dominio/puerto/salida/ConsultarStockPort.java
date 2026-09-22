package com.ventas.key.hexagonal.stock.dominio.puerto.salida;

import com.ventas.key.hexagonal.stock.dominio.modelo.DisponibilidadStock;

import java.util.List;
import java.util.Optional;

/**
 * Lectura del stock de un producto y de lo que tienen repartido sus variantes.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>Solo lectura: quien mueve stock sigue siendo el flujo de variantes y el de ventas. Este
 * puerto existe para responder "cuanto queda libre" sin tener que pasar por ellos.
 */
public interface ConsultarStockPort {

    Optional<DisponibilidadStock> disponibilidadDe(Integer productoId);

    /**
     * Los productos cuyas variantes habilitadas piden mas stock del que el producto declara.
     *
     * <p>Es el reporte que hay que mirar antes de migrar el modelo de stock: dice cuales estan
     * descuadrados y por cuanto. En produccion al 2026-09-22 habia varios (el 269 tenia 12 en el
     * producto y 18 repartidos en variantes).
     */
    List<DisponibilidadStock> productosDescuadrados();
}
