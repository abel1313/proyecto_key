package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

/**
 * Lo que el puerto devuelve por cada forma de pago (sin porcentaje: eso lo calcula el servicio).
 *
 * @param comision lo que se queda la terminal de lo vendido con esa forma de pago (tarifa + IVA de la terminal)
 */
public record FormaDePagoVentas(String forma, int ventas, double total, double comision) {
}
