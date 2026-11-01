package com.ventas.key.hexagonal.graficasventas.dominio.modelo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Cuántos pedidos hay en cada punto del camino. Siempre vienen todas las categorías (con 0 si no hay).
 *
 * @param atrasados pedidos sin entregar y sin cancelar cuya fecha de recogida ya pasó
 */
public record EstadoDePedidos(LocalDate desde, LocalDate hasta, int activos, int atrasados, List<ConteoPedidos> estados) {

    public record ConteoPedidos(String codigo, String etiqueta, boolean activo, int pedidos) {
    }

    public EstadoDePedidos {
        estados = List.copyOf(estados);
    }

    public static EstadoDePedidos de(LocalDate desde, LocalDate hasta, Map<CategoriaPedido, Integer> conteos, int atrasados) {
        List<ConteoPedidos> lista = java.util.Arrays.stream(CategoriaPedido.values())
                .map(c -> new ConteoPedidos(c.name(), c.etiqueta(), c.activo(), conteos.getOrDefault(c, 0))).toList();
        int activos = lista.stream().filter(ConteoPedidos::activo).mapToInt(ConteoPedidos::pedidos).sum();
        return new EstadoDePedidos(desde, hasta, activos, atrasados, lista);
    }
}
