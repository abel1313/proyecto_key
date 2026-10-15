package com.ventas.key.hexagonal.precio.infraestructura.dto;

/**
 * @param precioRebaja  null o 0 = sin descuento
 * @param usarDescuento true = el articulo se vende al descuento (R8); null o false = al normal
 */
public record CambiarPrecioArticuloRequest(Double precioVenta, Double precioRebaja, Boolean usarDescuento) {
}
