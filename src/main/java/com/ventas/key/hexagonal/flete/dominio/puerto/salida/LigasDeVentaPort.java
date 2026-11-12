package com.ventas.key.hexagonal.flete.dominio.puerto.salida;

import com.ventas.key.hexagonal.flete.dominio.modelo.ParteAsignada;
import com.ventas.key.hexagonal.flete.dominio.modelo.RenglonDisponible;

import java.util.List;

/** [Hexagonal: Driven Port] Lo que necesita la asignación de viajes a las ventas: ver las piezas que quedan, bloquearlas y guardar las ligas. */
public interface LigasDeVentaPort {
    /** ¿Ya existen las tablas (migración corrida)? Si no, no se asigna nada y la venta sigue igual. */
    boolean disponible();

    /** Bloquea los renglones de viaje de este producto (para que dos ventas no tomen la misma pieza) y devuelve los que aún tienen piezas, del viaje más viejo al más nuevo. */
    List<RenglonDisponible> bloquearYLeer(int productoId);

    /** Las piezas que hay hoy en existencia del producto (el stock ya trae descontadas las de la línea que se está ligando). */
    int existenciaActual(int productoId);

    void guardar(int detalleVentaId, List<ParteAsignada> partes);
}
