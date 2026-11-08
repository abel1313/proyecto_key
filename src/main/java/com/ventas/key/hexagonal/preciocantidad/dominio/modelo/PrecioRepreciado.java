package com.ventas.key.hexagonal.preciocantidad.dominio.modelo;

import java.math.BigDecimal;

/**
 * [Hexagonal: Domain] El precio que le toca a una línea después de volver a calcular.
 *
 * @param precioSinGrupoNuevo lo que hay que recordar para poder regresarla ({@code null} = ya no se cobra con precio de grupo y se borra la marca)
 */
public record PrecioRepreciado(int detalleId, BigDecimal precioNuevo, BigDecimal precioSinGrupoNuevo) {
}
