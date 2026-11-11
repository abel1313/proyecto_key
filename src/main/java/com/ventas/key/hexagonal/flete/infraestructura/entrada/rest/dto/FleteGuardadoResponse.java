package com.ventas.key.hexagonal.flete.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.flete.dominio.modelo.ProductoConFlete;

/** Un producto listo para entrar a un viaje: costo de hoy y flete por pieza guardado ({@code transporte} 0 = sin flete guardado). */
public record FleteGuardadoResponse(int productoId, String nombre, String codigoBarras, double precioCosto, int stock, double transporte) {

    public static FleteGuardadoResponse de(ProductoConFlete p) {
        return new FleteGuardadoResponse(p.productoId(), p.nombre(), p.codigoBarras(), p.precioCosto() == null ? 0 : p.precioCosto().doubleValue(),
                p.stock(), p.transporte() == null ? 0 : p.transporte().doubleValue());
    }
}
