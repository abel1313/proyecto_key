package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/**
 * Un artículo que el modelo propuso en la marca {@code ##PEDIDO{...}##}. Es solo una PROPUESTA: el id (la «ref» del
 * catálogo que vio el modelo) y la cantidad no se creen hasta que el sistema los valida contra la base (regla R16).
 */
public record LineaSolicitada(Integer articuloId, Integer cantidad) {
}
