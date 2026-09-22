package com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.ArticuloDisponible;

import java.util.Optional;

/**
 * Lo que el catalogo sabe de un articulo: si se puede vender, cuanto hay y a que precio.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface CatalogoArticuloPort {

    /**
     * Lee el articulo <b>bloqueando la fila</b> para el resto de la transaccion.
     *
     * <p>Sin el bloqueo, dos ediciones simultaneas del mismo articulo leen el mismo stock y las
     * dos creen que alcanza -- que es como se llega a stock negativo. El flujo de creacion de
     * pedidos ya lee asi ({@code findByIdWithLock}).
     */
    Optional<ArticuloDisponible> leerParaEditar(Integer varianteId);
}
