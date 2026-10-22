package com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface ConfirmarCobroCasoUso {

    /**
     * Le pregunta a la pasarela como va el cobro y aplica el resultado. Es idempotente (T4): llamarlo varias
     * veces, o a la vez desde el webhook y el polling, no repite efectos.
     */
    IntentoDePago confirmar(Long intentoId);
}
