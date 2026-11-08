package com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida;

/**
 * [Hexagonal: Driven Port] El precio por cantidad (grupos de precio) de un pedido ya guardado. Tras cada edición el precio de cada línea se
 * decide de nuevo con lo que lleva el pedido en ese momento (G6): si el grupo junta su mínimo, las líneas pasan a su precio de grupo; si ya no
 * lo junta, regresan al precio que tenían antes.
 */
public interface RepreciarPorCantidadPort {

    /** Vuelve a decidir el precio de las líneas del pedido (sin tocar el total: de eso se encarga quien llama, después). */
    void repreciar(Integer pedidoId);

    /** ¿Este precio es el de grupo vigente de ese artículo? Solo para no rechazar el precio que el front muestra; el que se guarda lo decide {@link #repreciar}. */
    boolean esPrecioDeGrupo(Integer varianteId, double precio);

    /** La línea cambió de artículo o de precio por su cuenta: el precio de antes que se recordaba ya no le corresponde. */
    void olvidarPrecioDeAntes(Integer detalleId);
}
