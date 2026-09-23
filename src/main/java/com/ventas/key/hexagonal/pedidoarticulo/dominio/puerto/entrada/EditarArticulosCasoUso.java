package com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.entrada;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.PedidoEditable;

/**
 * Editar los articulos de un pedido ya creado.
 *
 * <p>[Hexagonal: Driving Port] [Clean: Use Case]
 *
 * <p>Las tres operaciones viven juntas porque comparten el mismo invariante: despues de
 * cualquiera de ellas el pedido tiene que quedar con su total recalculado, su stock cuadrado y su
 * combo entero o ausente. Separarlas en tres puertos haria parecer que se pueden razonar por
 * separado, y no se puede.
 */
public interface EditarArticulosCasoUso {

    /**
     * Suma un articulo al pedido, a precio de catalogo (R2, R5).
     *
     * <p>Si ese articulo ya esta en el pedido como linea normal, suma cantidad en vez de crear
     * una segunda linea (R6).
     */
    PedidoEditable agregar(Integer pedidoId, AgregarArticulo peticion);

    /**
     * Cambia una linea por otro articulo.
     *
     * <p>Si la linea es de una promocion y el articulo nuevo no pertenece al combo, lanza
     * {@code CambioRompePromocionException} con las dos salidas, en vez de elegir por el negocio
     * (R4). El modo del request dice cual eligio el usuario.
     */
    PedidoEditable cambiar(Integer pedidoId, Integer detalleId, CambiarArticulo peticion);

    /** Saca una promocion completa del pedido y devuelve su stock (R4). */
    PedidoEditable quitarPromocion(Integer pedidoId, Integer promocionId);

    /**
     * Que se quiere agregar.
     *
     * @param varianteId     el articulo
     * @param cantidad       piezas; tiene que ser mayor a 0
     * @param precioUnitario null = precio normal. Solo se acepta el normal o el de rebaja (R2)
     */
    record AgregarArticulo(Integer varianteId, int cantidad, Double precioUnitario) {}

    /**
     * Por que articulo se cambia una linea, y que hacer si eso rompe un combo.
     *
     * @param varianteId     el articulo nuevo
     * @param cantidad       piezas; null = la misma que tenia la linea
     * @param precioUnitario null = precio normal (o el del combo, si sigue dentro de la promocion)
     * @param modo           que hacer si el articulo nuevo no esta en la promocion de la linea
     */
    record CambiarArticulo(Integer varianteId, Integer cantidad, Double precioUnitario, ModoCambio modo) {

        public int cantidadElegida(int cantidadActual) {
            return cantidad != null ? cantidad : cantidadActual;
        }
    }

    /** Las dos salidas de R4, mas la de "todavia no elegi". */
    enum ModoCambio {
        /** Preguntar: si rompe el combo, no hace nada y devuelve las opciones. Es el default. */
        VALIDAR,
        /** Opcion (a): sale el combo entero y entra el articulo nuevo a precio normal. */
        QUITAR_PROMOCION,
        /** Opcion (b): la promocion queda intacta y el articulo nuevo se suma aparte. */
        CONSERVAR_PROMOCION
    }
}
