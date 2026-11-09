package com.ventas.key.hexagonal.calculoprecio.dominio.puerto.salida;

import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.CalculoPrecio;
import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.FiltroCalculos;
import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.PaginaCalculos;

import java.util.List;
import java.util.Optional;

/** [Hexagonal: Driven Port] Lo que el dominio necesita para guardar y leer los cálculos de precio. */
public interface CalculosPrecioPort {
    boolean existeProducto(int productoId);

    CalculoPrecio guardar(CalculoPrecio calculo);

    Optional<CalculoPrecio> ultimo(int productoId);

    /** Del más reciente al más viejo. */
    List<CalculoPrecio> historial(int productoId, int maximo);

    /** El último cálculo de cada producto que tenga uno, del más reciente al más viejo (página desde 1). */
    PaginaCalculos ultimosPorProducto(FiltroCalculos filtro, int pagina, int tamano);
}
