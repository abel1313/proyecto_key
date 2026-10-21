package com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CanalDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface IniciarCobroCasoUso {

    /**
     * Empieza a cobrar un pedido con tarjeta. El monto lo calcula el back (T2); {@code cuotas} es 1 (de contado)
     * o un plan de meses sin intereses.
     */
    IntentoDePago iniciar(Integer pedidoId, CanalDePago canal, int cuotas, String iniciadoPor);
}
