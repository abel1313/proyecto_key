package com.ventas.key.hexagonal.flete.dominio.puerto.entrada;

import com.ventas.key.hexagonal.flete.dominio.modelo.ProductoConFlete;
import java.util.Collection;
import java.util.List;

/** [Hexagonal: Driving Port] Los productos con el flete por pieza ya guardado, para precargar un viaje sin volver a calcularlo. Solo lectura. */
public interface ConsultarFletesGuardadosCasoUso {
    List<ProductoConFlete> deProductos(Collection<Integer> productoIds);
}
