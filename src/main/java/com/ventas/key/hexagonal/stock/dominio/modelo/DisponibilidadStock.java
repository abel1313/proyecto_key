package com.ventas.key.hexagonal.stock.dominio.modelo;

import com.ventas.key.hexagonal.stock.dominio.excepcion.AjusteStockInvalidoException;

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
 * @param modeloHabilitado  false si el modelo esta deshabilitado o dado de baja: no sale en la tienda
 *                          ni en ventas, y sus articulos tampoco (2026-10-08)
 * @param modeloConFoto     false si el modelo no tiene foto (al darlo de baja se le borran)
 */
public record DisponibilidadStock(
        Integer productoId,
        String nombreProducto,
        int stockTotal,
        int enVariantes,
        int variantesActivas,
        int enVariantesDeBaja,
        boolean modeloHabilitado,
        boolean modeloConFoto) {

    /** Sin el estado del modelo (como se armaba antes del 2026-10-08): habilitado y con foto. */
    public DisponibilidadStock(Integer productoId, String nombreProducto, int stockTotal, int enVariantes,
                               int variantesActivas, int enVariantesDeBaja) {
        this(productoId, nombreProducto, stockTotal, enVariantes, variantesActivas, enVariantesDeBaja, true, true);
    }

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

    /**
     * Cuantos articulos mas se pueden dar de alta: cada uno necesita al menos 1 pieza, asi que es
     * lo libre (nunca negativo). Es lo que Agregar articulo muestra al elegir el modelo.
     */
    public int articulosQueAunCaben() {
        return Math.max(disponible(), 0);
    }

    /**
     * El modelo despues de agregarle (+) o quitarle (-) stock, sin tocar sus articulos (R-A1..R-A3,
     * README de este dominio). Lo agregado queda libre para articulos nuevos.
     *
     * @throws AjusteStockInvalidoException si el ajuste es 0, deja el modelo en negativo o por debajo
     *                                      de lo que ya tienen repartido sus articulos
     */
    public DisponibilidadStock conAjuste(int ajuste) {
        if (ajuste == 0) {
            throw new AjusteStockInvalidoException("Escribe cuánto stock agregar (+) o quitar (−) al modelo");
        }
        int nuevo = stockTotal + ajuste;
        if (nuevo < 0) {
            throw new AjusteStockInvalidoException(String.format(
                    "No se puede quitar %d: el modelo solo tiene %d", -ajuste, stockTotal));
        }
        if (nuevo < enVariantes) {
            throw new AjusteStockInvalidoException(String.format(
                    "No se puede dejar el modelo en %d: ya tiene %d repartidos en sus artículos", nuevo, enVariantes));
        }
        return new DisponibilidadStock(productoId, nombreProducto, nuevo, enVariantes, variantesActivas,
                enVariantesDeBaja, modeloHabilitado, modeloConFoto);
    }
}
