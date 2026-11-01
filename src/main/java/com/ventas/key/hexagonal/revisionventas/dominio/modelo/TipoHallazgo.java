package com.ventas.key.hexagonal.revisionventas.dominio.modelo;

/**
 * Cada cosa que la Revisión de ventas sabe buscar, con su explicación en palabras del dueño.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Las reglas exactas de cada una están en {@code PLAN_REVISION_VENTAS_Y_GRAFICAS.md}. El orden de declaración
 * es el orden en que se muestran (primero las graves). La revisión es de SOLO LECTURA: avisa, no corrige.
 */
public enum TipoHallazgo {

    PAGADO_SIN_VENTA(Gravedad.ALTA,
            "Pedido pagado que no tiene venta",
            "El pedido dice que ya se pagó (Pagado o Entregado) pero no hay una venta registrada: ese dinero no sale en los reportes."),
    TOTAL_PEDIDO_NO_CUADRA(Gravedad.ALTA,
            "El total del pedido no es la suma de sus artículos",
            "El total guardado del pedido es distinto a sumar sus artículos: el cliente podría estar pagando de más o de menos."),
    TOTAL_VENTA_NO_CUADRA(Gravedad.ALTA,
            "El total de la venta no es la suma de sus artículos",
            "El total de la venta es distinto a sumar sus artículos: los reportes de ventas y ganancia pueden estar mal."),
    ABONOS_NO_CUADRAN(Gravedad.ALTA,
            "Lo pagado no coincide con los abonos",
            "En un Apartado o Ir pagando, lo que dice que lleva pagado no es la suma de sus abonos."),
    COBRO_TARJETA_SIN_APLICAR(Gravedad.ALTA,
            "Cobro con tarjeta que no quedó aplicado al pedido",
            "Se cobró con tarjeta pero el pedido no quedó pagado: hay que revisar si se devuelve o se aplica a mano."),
    STOCK_NEGATIVO(Gravedad.ALTA,
            "Stock negativo",
            "Un artículo o un modelo tiene menos de cero piezas: se vendió más de lo que había o se descontó dos veces."),
    ARTICULO_CON_STOCK_SIN_MODELO(Gravedad.ALTA,
            "Se ve con stock en la tienda pero el pedido lo rechaza",
            "El artículo tiene piezas pero su modelo tiene 0: el cliente lo agrega al carrito y al pedir sale «Disponible: 0». "
                    + "Hay que subirle el stock al modelo."),
    VENTA_SIN_PEDIDO(Gravedad.MEDIA,
            "Venta que no está ligada a ningún pedido",
            "La venta no tiene pedido: no se puede ver el detalle de entrega ni los datos del cliente desde el pedido.");

    private final Gravedad gravedad;
    private final String titulo;
    private final String explicacion;

    TipoHallazgo(Gravedad gravedad, String titulo, String explicacion) {
        this.gravedad = gravedad;
        this.titulo = titulo;
        this.explicacion = explicacion;
    }

    public Gravedad gravedad() { return gravedad; }
    public String titulo() { return titulo; }
    public String explicacion() { return explicacion; }
}
