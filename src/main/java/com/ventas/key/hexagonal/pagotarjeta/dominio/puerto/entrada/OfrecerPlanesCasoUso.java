package com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada;

import java.util.List;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface OfrecerPlanesCasoUso {

    /** Los meses sin intereses que se pueden ofrecer hoy para ese pedido (vacio = solo de contado). */
    List<Integer> mesesPara(Integer pedidoId);
}
