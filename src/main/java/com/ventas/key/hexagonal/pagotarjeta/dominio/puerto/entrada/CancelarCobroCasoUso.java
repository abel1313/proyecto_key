package com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface CancelarCobroCasoUso {

    /** Cancela un cobro que sigue activo. Si la pasarela ya lo cobro, lo aplica y avisa que no se pudo cancelar. */
    IntentoDePago cancelar(Long intentoId);
}
