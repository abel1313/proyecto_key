package com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida;

import java.util.function.Supplier;

/**
 * Ejecuta varias escrituras de LA BASE como una sola unidad: o se guardan todas o ninguna.
 *
 * <p>Contrato: dentro de {@code accion} solo se hacen escrituras locales (pedido, intento,
 * devolucion). Jamas se llama a la pasarela dentro: una transaccion abierta mientras se espera a
 * Mercado Pago bloquea filas y agota el pool de conexiones.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface TransaccionPort {

    <T> T ejecutar(Supplier<T> accion);
}
