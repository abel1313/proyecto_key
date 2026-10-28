package com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.OfertaDeMeses;

import java.util.List;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface OfrecerPlanesCasoUso {

    /** Los meses sin intereses que se pueden ofrecer hoy para ese pedido (vacio = solo de contado). */
    List<Integer> mesesPara(Integer pedidoId);

    /** Lo mismo, pero si no hay meses explica por que y que articulos lo impiden. */
    OfertaDeMeses ofertaPara(Integer pedidoId);
}
