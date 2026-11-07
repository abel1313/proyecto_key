package com.ventas.key.hexagonal.preciocantidad.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.PrecioDeGrupo;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** El precio de grupo que le toca a cada producto de la venta; lo que no aparece se cobra a su precio normal. */
public record PrecioPorCantidadResponse(List<Precio> precios) {

    public record Precio(int productoId, double precioPromocion, int grupoId, String grupo, int minimoPiezas, int piezasDelGrupo) {
    }

    public static PrecioPorCantidadResponse de(Collection<PrecioDeGrupo> precios) {
        return new PrecioPorCantidadResponse(precios.stream()
                .sorted(Comparator.comparingInt(PrecioDeGrupo::productoId))
                .map(p -> new Precio(p.productoId(), p.precio().doubleValue(), p.grupoId(), p.grupo(), p.minimoPiezas(), p.piezasDelGrupo()))
                .toList());
    }
}
