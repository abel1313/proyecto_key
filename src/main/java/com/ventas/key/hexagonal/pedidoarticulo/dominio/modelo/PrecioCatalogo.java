package com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo;

/**
 * Los precios a los que un producto se puede cobrar.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>El producto tiene tres precios y solo dos son cobrables (regla R6 del dominio
 * {@code promocion}):
 *
 * <ul>
 *   <li>{@code precio_costo} — a lo que se compro. Nunca se vende a eso, por eso no esta aqui.
 *   <li>{@code precio_venta} — el normal, el que ve el cliente en la tienda.
 *   <li>{@code precio_rebaja} — el descuento que maneja el admin. Hasta el 2026-09-22 se
 *       guardaba y se mostraba pero <b>nunca se cobraba</b>: para bajar un precio habia que armar
 *       una promocion, que es mas trabajo y deja registrado algo que no es.
 * </ul>
 *
 * <p>Que sean exactamente dos y no un monto libre es lo que impide que el front invente un
 * precio al editar un pedido.
 *
 * @param nombreProducto para poder decir en el error de que producto se trata
 * @param precioVenta    el normal
 * @param precioRebaja   el rebajado, o null/0 si el producto no tiene rebaja cargada
 */
public record PrecioCatalogo(String nombreProducto, Double precioVenta, Double precioRebaja) {

    /** Un centavo de margen: el front redondea para mostrar y vuelve el numero redondeado. */
    private static final double TOLERANCIA = 0.01;

    public double normal() {
        return precioVenta != null ? precioVenta : 0.0;
    }

    /** 0 significa "sin rebaja", no "gratis". */
    public double rebaja() {
        return precioRebaja != null ? precioRebaja : 0.0;
    }

    public boolean tieneRebaja() {
        return rebaja() > 0;
    }

    /** Si este monto es uno de los dos precios del catalogo. */
    public boolean admite(Double precio) {
        if (precio == null) {
            return false;
        }
        boolean esNormal = Math.abs(precio - normal()) <= TOLERANCIA;
        boolean esRebaja = tieneRebaja() && Math.abs(precio - rebaja()) <= TOLERANCIA;
        return esNormal || esRebaja;
    }

    /**
     * El precio a cobrar cuando el request no manda ninguno.
     *
     * <p>Es el normal a proposito: la rebaja es una decision que alguien tiene que tomar
     * explicitamente, no algo que se aplique solo por no mandar el campo.
     */
    public double porDefecto() {
        return normal();
    }

    /** Los precios validos, escritos para meterlos en un mensaje de error. */
    public String explicacion() {
        return tieneRebaja()
                ? String.format("$%.2f (normal) o $%.2f (rebaja)", normal(), rebaja())
                : String.format("$%.2f (normal, este producto no tiene rebaja cargada)", normal());
    }
}
