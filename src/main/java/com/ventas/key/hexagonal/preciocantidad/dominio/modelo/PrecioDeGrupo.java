package com.ventas.key.hexagonal.preciocantidad.dominio.modelo;

import java.math.BigDecimal;

/**
 * [Hexagonal: Domain] El precio de grupo que le toca a un producto en una venta porque su grupo juntó el mínimo.
 *
 * @param piezasDelGrupo cuántas piezas del grupo lleva la venta en total
 */
public record PrecioDeGrupo(int productoId, int grupoId, String grupo, BigDecimal precio, int minimoPiezas, int piezasDelGrupo) {
}
