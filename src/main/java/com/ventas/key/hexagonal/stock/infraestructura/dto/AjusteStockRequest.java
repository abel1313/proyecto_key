package com.ventas.key.hexagonal.stock.infraestructura.dto;

/**
 * Cuanto agregar (+) o quitar (-) al stock del modelo.
 *
 * <p>[Hexagonal: parte del adaptador] [Clean: Interface Adapters]
 *
 * @param ajuste null o 0 se rechaza con un mensaje (la regla vive en el dominio)
 */
public record AjusteStockRequest(Integer ajuste) {
}
