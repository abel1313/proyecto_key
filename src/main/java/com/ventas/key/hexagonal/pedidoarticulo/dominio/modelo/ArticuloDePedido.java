package com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo;

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

    /** Si esta linea es de la promocion indicada. */
    public boolean perteneceA(Integer promocion) {
        return promocionId != null && promocionId.equals(promocion);
    }
}
