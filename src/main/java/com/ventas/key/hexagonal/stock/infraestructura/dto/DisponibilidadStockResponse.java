package com.ventas.key.hexagonal.stock.infraestructura.dto;

import com.ventas.key.hexagonal.stock.dominio.modelo.DisponibilidadStock;

/**
 * Lo que ve el front al preguntar cuanto stock queda libre.
 *
 * <p>[Hexagonal: parte del adaptador] [Clean: Interface Adapters]
 *
 * <p>{@code disponible} viaja calculado y no como formula, para que la pantalla no tenga que
 * repetir la resta -- si la repitiera, el dia que cambie la regla habria dos versiones.
 *
 * @param mensaje texto ya armado para mostrar tal cual: "10 en total, 4 en modelos, quedan 6"
 */
public record DisponibilidadStockResponse(
        Integer productoId,
        String nombreProducto,
        int stockTotal,
        int enVariantes,
        int variantesActivas,
        int enVariantesDeBaja,
        int disponible,
        boolean descuadrado,
        String mensaje) {

    public static DisponibilidadStockResponse de(DisponibilidadStock d) {
        return new DisponibilidadStockResponse(
                d.productoId(),
                d.nombreProducto(),
                d.stockTotal(),
                d.enVariantes(),
                d.variantesActivas(),
                d.enVariantesDeBaja(),
                d.disponible(),
                d.estaDescuadrado(),
                mensajeDe(d));
    }

    private static String mensajeDe(DisponibilidadStock d) {
        if (d.estaDescuadrado()) {
            return String.format(
                    "Este producto esta descuadrado: tiene %d en total pero sus %d modelos suman %d.",
                    d.stockTotal(), d.variantesActivas(), d.enVariantes());
        }
        if (d.disponible() == 0) {
            return String.format("Los %d en total ya estan repartidos en %d modelos: no queda disponible.",
                    d.stockTotal(), d.variantesActivas());
        }
        return String.format("%d en total, %d repartidos en %d modelos, quedan %d disponibles.",
                d.stockTotal(), d.enVariantes(), d.variantesActivas(), d.disponible());
    }
}
