package com.ventas.key.hexagonal.precio.infraestructura.dto;

/** @param precioRebaja null o 0 = sin descuento */
public record CambiarPrecioRequest(Double precioVenta, Double precioRebaja) {
}
