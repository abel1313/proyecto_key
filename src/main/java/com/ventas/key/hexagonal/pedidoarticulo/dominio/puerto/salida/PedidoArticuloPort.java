package com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.ArticuloDePedido;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.PedidoEditable;

import java.util.List;
import java.util.Optional;

/**
 * Lectura y escritura de las lineas de un pedido.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>Cada metodo hace <b>una</b> cosa (regla "un metodo, una responsabilidad" de CLAUDE.md):
 * agregar una linea agrega una linea, no mueve stock ni recalcula totales. Quien orquesta es el
 * servicio de aplicacion, que los llama en orden.
 *
 * <p>Es la leccion del caso {@code ClienteDiscoPort} en {@code micro_imagenes}: un puerto que
 * mezcla responsabilidades no se puede reusar sin arrastrar todo lo demas detras.
 */
public interface PedidoArticuloPort {

    Optional<PedidoEditable> buscarPedido(Integer pedidoId);

    /** Crea una linea nueva y devuelve su id. */
    Integer agregarLinea(Integer pedidoId, Integer varianteId, int cantidad, double precioUnitario);

    /** Cambia la cantidad de una linea que ya existe; el subtotal se recalcula solo. */
    void cambiarCantidad(Integer detalleId, int cantidadNueva);

    /** Cambia a que articulo apunta una linea, con su precio. Conserva la promocion de la linea. */
    void cambiarArticulo(Integer detalleId, Integer varianteNuevaId, double precioUnitario);

    void borrarLineas(List<ArticuloDePedido> lineas);

    /** Guarda el total recalculado. No toca lo ya pagado (R7). */
    void guardarTotal(Integer pedidoId, double total);
}
