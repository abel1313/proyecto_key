package com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.EdicionPedidoException;

/**
 * Una linea de un pedido: que artculo, cuantos y a que precio se cobro.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Se llama "articulo" y no "variante" a proposito: es el nombre al que va a migrar todo el
 * sistema (pedido del usuario, 2026-09-22). Mientras la migracion no pase, {@code varianteId}
 * sigue apuntando a la tabla {@code variantes}.
 *
 * @param detalleId      id de la linea (tabla {@code detalle_pedidos}) -- es lo que se edita
 * @param varianteId     el articulo concreto (talla/color)
 * @param productoId     el modelo al que pertenece
 * @param nombre         para poder explicar en los mensajes de error que linea es cual
 * @param cantidad       piezas de esta linea
 * @param precioUnitario lo que se cobro por pieza; congelado al momento de crear el pedido
 * @param promocionId    la promocion a la que pertenece, o null si es una linea normal
 */
public record ArticuloDePedido(
        Integer detalleId,
        Integer varianteId,
        Integer productoId,
        String nombre,
        int cantidad,
        double precioUnitario,
        Integer promocionId) {

    /** Si esta linea es parte de un combo -- entonces no se toca sola (R4). */
    public boolean esDePromocion() {
        return promocionId != null;
    }

    /**
     * Lo que aporta al total del pedido.
     *
     * <p>Se calcula, nunca se lee de la base: el subtotal guardado fue justamente por donde se
     * colo el agujero de dinero del 2026-09-22 (un request con el precio correcto y
     * {@code subTotal: 1} dejaba el pedido entero en $1).
     */
    public double subTotal() {
        return precioUnitario * cantidad;
    }

    /**
     * Cuantas piezas de esta linea se cambian por otro articulo (R10). Null = todas.
     *
     * <p>Se cambian 1 a 1: cada pieza que sale entra como una pieza del articulo nuevo. En una
     * linea de promocion solo se cambia la linea completa: partirla dejaria piezas del combo
     * sueltas a precio promocional.
     */
    public int piezasACambiar(Integer pedidas) {
        if (pedidas == null) {
            return cantidad;
        }
        if (pedidas <= 0) {
            throw new EdicionPedidoException(
                    "La cantidad tiene que ser mayor a 0. Para quitar un articulo esta el boton de quitar");
        }
        if (pedidas > cantidad) {
            throw new EdicionPedidoException("'" + nombre + "' tiene " + cantidad + " pieza(s) en el pedido: "
                    + "no se pueden cambiar " + pedidas + ". Para sumar mas, usa Agregar articulo");
        }
        if (pedidas < cantidad && esDePromocion()) {
            throw new EdicionPedidoException("'" + nombre + "' es parte de una promocion: se cambia la linea "
                    + "completa (" + cantidad + " pieza(s)), no una parte");
        }
        return pedidas;
    }

    /** Si cambiar esas piezas deja parte de la linea con el articulo de antes. */
    public boolean esCambioParcial(int piezas) {
        return piezas < cantidad;
    }

    /** Si esta linea es de la promocion indicada. */
    public boolean perteneceA(Integer promocion) {
        return promocionId != null && promocionId.equals(promocion);
    }
}
