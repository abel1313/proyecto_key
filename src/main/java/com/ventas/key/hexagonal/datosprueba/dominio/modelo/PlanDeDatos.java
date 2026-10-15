package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

import com.ventas.key.hexagonal.datosprueba.dominio.excepcion.PlanInvalidoException;

/**
 * Lo que se pide generar en una corrida. Los topes son R11 del README: que un numero mal escrito
 * no cree millones de filas.
 *
 * @param modelos        cuantos modelos (productos) nuevos
 * @param articulosMin   minimo de articulos por modelo
 * @param articulosMax   maximo de articulos por modelo
 * @param pedidos        cuantos pedidos aleatorios, usando los articulos recien creados
 * @param semilla        misma semilla = mismos nombres, precios y mezcla de pedidos
 */
public record PlanDeDatos(int modelos, int articulosMin, int articulosMax, int pedidos, long semilla) {

    public static final int MAX_MODELOS = 20_000;
    public static final int MAX_ARTICULOS_POR_MODELO = 4;
    public static final int MAX_PEDIDOS = 3_000;

    public PlanDeDatos {
        if (modelos < 1 || modelos > MAX_MODELOS) {
            throw new PlanInvalidoException("Los modelos van de 1 a " + MAX_MODELOS + " por corrida; se pidieron " + modelos);
        }
        if (articulosMin < 1 || articulosMax > MAX_ARTICULOS_POR_MODELO || articulosMin > articulosMax) {
            throw new PlanInvalidoException("Los artículos por modelo van de 1 a " + MAX_ARTICULOS_POR_MODELO
                    + " (mínimo " + articulosMin + ", máximo " + articulosMax + ")");
        }
        if (pedidos < 0 || pedidos > MAX_PEDIDOS) {
            throw new PlanInvalidoException("Los pedidos van de 0 a " + MAX_PEDIDOS + " por corrida; se pidieron " + pedidos);
        }
    }
}
