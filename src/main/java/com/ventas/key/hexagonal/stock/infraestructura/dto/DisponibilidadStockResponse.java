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
 * @param mensaje texto ya armado para mostrar tal cual: "10 en total, 4 en artículos, quedan 6"
 * @param habilitado false si el modelo esta deshabilitado o dado de baja (2026-10-08)
 * @param conFoto false si el modelo no tiene foto
 * @param articulosQueAunCaben cuantos articulos mas se pueden dar de alta (1 pieza cada uno)
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
        String mensaje,
        boolean habilitado,
        boolean conFoto,
        int articulosQueAunCaben) {

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
                mensajeDe(d),
                d.modeloHabilitado(),
                d.modeloConFoto(),
                d.articulosQueAunCaben());
    }

    private static String mensajeDe(DisponibilidadStock d) {
        if (d.estaDescuadrado()) {
            return String.format(
                    "Este modelo está descuadrado: tiene %d en total pero sus %d artículos suman %d.",
                    d.stockTotal(), d.variantesActivas(), d.enVariantes());
        }
        if (d.disponible() == 0) {
            return String.format("Los %d en total ya están repartidos en %d artículos: no queda disponible.",
                    d.stockTotal(), d.variantesActivas());
        }
        return String.format("%d en total, %d repartidos en %d artículos, quedan %d disponibles.",
                d.stockTotal(), d.enVariantes(), d.variantesActivas(), d.disponible());
    }
}
