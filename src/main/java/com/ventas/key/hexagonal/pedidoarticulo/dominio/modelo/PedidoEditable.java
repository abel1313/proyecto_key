package com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo;

import java.util.List;
import java.util.Optional;

/**
 * Un pedido visto como "lo que se le puede editar": sus lineas y si esta abierto.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>No es la entidad {@code Pedido} entera -- no trae cliente, entrega, ni notificaciones. Trae
 * lo justo para decidir si una edicion es valida. Lo que no esta aqui es lo que este dominio no
 * tiene por que mirar.
 *
 * @param pedidoId    id del pedido
 * @param estado      {@code Pendiente}, {@code Entregado}, {@code cancelado}...
 * @param totalPagado lo que el cliente ya puso; no se toca al editar (R7)
 * @param articulos   las lineas actuales
 */
public record PedidoEditable(
        Integer pedidoId,
        String estado,
        double totalPagado,
        List<ArticuloDePedido> articulos) {

    private static final String ENTREGADO = "Entregado";
    private static final String CANCELADO = "cancelado";

    /** Un pedido cerrado ya no se edita (R1). */
    public boolean estaCerrado() {
        return ENTREGADO.equals(estado) || CANCELADO.equals(estado);
    }

    /** Por que esta cerrado, para poder decirselo a quien lo intento. */
    public String motivoDelCierre() {
        if (ENTREGADO.equals(estado)) {
            return "ya se entrego";
        }
        if (CANCELADO.equals(estado)) {
            return "esta cancelado";
        }
        return "no esta abierto";
    }

    public Optional<ArticuloDePedido> linea(Integer detalleId) {
        return articulos.stream().filter(a -> a.detalleId().equals(detalleId)).findFirst();
    }

    /**
     * La linea normal (sin promocion) de esta variante, si ya existe.
     *
     * <p>Se ignoran a proposito las lineas de promocion: por R5 lo que se agrega nunca entra a
     * una promocion, asi que una linea promocional de la misma variante no es la misma cosa y no
     * puede absorber el agregado.
     */
    public Optional<ArticuloDePedido> lineaNormalDe(Integer varianteId) {
        return articulos.stream()
                .filter(a -> !a.esDePromocion())
                .filter(a -> a.varianteId().equals(varianteId))
                .findFirst();
    }

    /** Todas las lineas de una promocion -- el combo completo, que entra o sale junto (R4). */
    public List<ArticuloDePedido> lineasDe(Integer promocionId) {
        return articulos.stream().filter(a -> a.perteneceA(promocionId)).toList();
    }

    /**
     * Cuantas lineas quedarian si se quitaran estas.
     *
     * <p>Un pedido sin articulos no es un pedido: si el cliente ya no quiere nada se cancela,
     * que ademas registra el motivo.
     */
    public boolean quedariaVacio(List<ArticuloDePedido> aQuitar) {
        return articulos.size() - aQuitar.size() <= 0;
    }

    /** El total recalculado desde cero, nunca ajustado sobre el anterior (R7). */
    public double total() {
        return articulos.stream().mapToDouble(ArticuloDePedido::subTotal).sum();
    }
}
