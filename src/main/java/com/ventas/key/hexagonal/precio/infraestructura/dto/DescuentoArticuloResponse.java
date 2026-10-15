package com.ventas.key.hexagonal.precio.infraestructura.dto;

import com.ventas.key.hexagonal.precio.dominio.modelo.PreciosDeArticulo;

/**
 * @param precioRebaja   el descuento cobrable; 0 si el articulo no tiene
 * @param tieneDescuento false = no hay nada que mostrar ni aplicar
 */
public record DescuentoArticuloResponse(Integer varianteId, double precioRebaja, boolean tieneDescuento) {

    public static DescuentoArticuloResponse de(PreciosDeArticulo p) {
        double descuento = p.descuentoCobrable();
        return new DescuentoArticuloResponse(p.varianteId(), descuento, descuento > 0);
    }
}
