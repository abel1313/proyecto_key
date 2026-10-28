package com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.ConfigMsiArticulo;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Lee y guarda la casilla "acepta meses sin intereses" de productos y artículos.
 *
 * <p>Va aparte de las entidades JPA a propósito: si esas entidades mapearan la columna y un ambiente todavía
 * no tuviera la migración, se caería todo el catálogo. Aquí, sin la columna, solo falla esta función.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface AceptaMsiPort {

    /** Vacío = el producto no existe. */
    Optional<Boolean> productoAcepta(Integer productoId);

    /** @return false si el producto no existe */
    boolean fijarProducto(Integer productoId, boolean acepta);

    /** Vacío = el artículo no existe. */
    Optional<ConfigMsiArticulo> articulo(Integer varianteId);

    /** @param acepta null = hereda del producto. @return false si el artículo no existe */
    boolean fijarArticulo(Integer varianteId, Boolean acepta);

    /** Valor efectivo (con herencia) de varios artículos de una vez; los que no existen no salen. */
    Map<Integer, Boolean> efectivos(Collection<Integer> varianteIds);
}
