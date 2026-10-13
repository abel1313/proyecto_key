package com.ventas.key.hexagonal.grupopedido.dominio.puerto.salida;

/**
 * Cambia la forma de cobro de un pedido del grupo, con el mismo camino que "🔁 Cambiar forma de
 * cobro" de un pedido suelto.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>No se reescribe el cambio: ahi ya vive lo que pasa con el estado (un Pendiente hecho desde la
 * cuenta del cliente deja de serlo) y con la entrega. Sin cobro: el dinero, si hay, entra despues
 * como abono del grupo (R6).
 */
public interface FormaDeCobroPort {

    /** @param tipoNuevo {@code APARTADO} o {@code FIADO} */
    void cambiar(Integer pedidoId, String tipoNuevo, String nota);
}
