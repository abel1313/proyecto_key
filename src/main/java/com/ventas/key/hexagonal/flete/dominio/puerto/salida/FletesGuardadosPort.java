package com.ventas.key.hexagonal.flete.dominio.puerto.salida;

import com.ventas.key.hexagonal.flete.dominio.modelo.ProductoConFlete;
import java.util.Collection;
import java.util.List;

/** [Hexagonal: Driven Port] Datos de los productos para armar un viaje: costo de hoy y el flete por pieza guardado en su último cálculo de precio. */
public interface FletesGuardadosPort {
    /** Los productos pedidos que existen (los que no existen no salen), con su flete guardado (cero si no tienen). */
    List<ProductoConFlete> productosConFlete(Collection<Integer> productoIds);
}
