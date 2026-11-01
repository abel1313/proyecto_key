package com.ventas.key.hexagonal.revisionventas.dominio.modelo;

/**
 * Un caso concreto que conviene revisar. Los ids que no aplican van en {@code null}.
 *
 * @param detalle una frase en palabras del dueño, con los números que importan ("Pedido #12 por $450.00 no tiene venta")
 */
public record Hallazgo(TipoHallazgo tipo, Integer pedidoId, Integer ventaId, Integer articuloId, String detalle) {
}
