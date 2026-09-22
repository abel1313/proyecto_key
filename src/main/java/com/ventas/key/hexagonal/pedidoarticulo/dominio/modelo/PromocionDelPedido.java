package com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo;

import java.util.Map;
import java.util.Optional;

/**
 * Una promocion vista como combo: que articulos la componen y a que precio.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Sirve para responder la unica pregunta que este dominio le hace a una promocion al editar un
 * pedido: <i>"el articulo nuevo, ¿esta dentro de este combo?"</i> (R4).
 *
 * @param promocionId  id de la promocion
 * @param descripcion  como la conoce el negocio, para los mensajes
 * @param vigente      si sigue activa y sin vencer
 * @param precioPorVariante que articulos la componen y a que precio cada uno
 */
public record PromocionDelPedido(
        Integer promocionId,
        String descripcion,
        boolean vigente,
        Map<Integer, Double> precioPorVariante) {

    /** Si el articulo forma parte de este combo. */
    public boolean incluye(Integer varianteId) {
        return precioPorVariante.containsKey(varianteId);
    }

    /**
     * El precio promocional de un articulo del combo.
     *
     * <p>Vacio si el articulo no pertenece -- es justamente el caso que dispara las dos salidas
     * de R4.
     */
    public Optional<Double> precioDe(Integer varianteId) {
        return Optional.ofNullable(precioPorVariante.get(varianteId));
    }
}
