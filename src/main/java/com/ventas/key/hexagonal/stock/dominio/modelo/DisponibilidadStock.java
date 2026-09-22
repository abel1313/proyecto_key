package com.ventas.key.hexagonal.stock.dominio.modelo;

/**
 * Cuanto stock de un producto esta libre para armar variantes nuevas.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Responde la pregunta que el admin se hace antes de dar de alta una variante: <i>"tengo 10
 * del producto y ya hice 4 variantes, ¿cuantas me quedan?"</i>. Hasta ahora no habia forma de
 * saberlo sin abrir la base, y el unico aviso llegaba como un error al intentar guardar.
 *
 * <p><b>Solo las variantes habilitadas cuentan.</b> Una dada de baja no retiene nada: su stock
 * vuelve al pozo en cuanto se deshabilita (regla R1, ver el README de este dominio).
 *
 * @param productoId       id del producto
 * @param nombreProducto   para mostrarlo sin tener que pedir el producto aparte
 * @param stockTotal       lo que declara el producto
 * @param enVariantes      repartido entre las variantes habilitadas
 * @param variantesActivas cuantas variantes habilitadas hay
 * @param enVariantesDeBaja stock que retienen las dadas de baja -- NO cuenta para el disponible,
 *                          se informa solo para explicar diferencias contra la tabla
 */
public record DisponibilidadStock(
        Integer productoId,
        String nombreProducto,
        int stockTotal,
        int enVariantes,
        int variantesActivas,
        int enVariantesDeBaja) {

    /**
     * Lo que queda libre para variantes nuevas.
     *
     * <p>Se calcula, no se guarda: un campo persistido se desincroniza en cuanto alguien toca el
     * stock por otro camino, y este sistema ya tiene inventario descuadrado justamente por
     * llevar la cuenta en dos lugares a la vez.
     *
     * <p>Puede dar negativo cuando el producto quedo descuadrado (las variantes suman mas de lo
     * que el producto declara). No se recorta a cero a proposito: un -6 en pantalla es un
     * problema real que el admin tiene que ver, no uno que convenga esconder.
     */
    public int disponible() {
        return stockTotal - enVariantes;
    }

    /** Si este producto esta descuadrado: sus variantes piden mas de lo que el producto tiene. */
    public boolean estaDescuadrado() {
        return disponible() < 0;
    }
}
