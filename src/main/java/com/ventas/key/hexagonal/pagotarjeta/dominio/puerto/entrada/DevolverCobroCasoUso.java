package com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;

import java.math.BigDecimal;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface DevolverCobroCasoUso {

    /**
     * Devuelve dinero de un cobro aprobado (T8). Quien llama (la capa web) ya comprobo que quien pide
     * la devolucion es un administrador.
     *
     * @param monto lo que se devuelve; null = todo lo que falte por devolver
     */
    IntentoDePago devolver(Long intentoId, BigDecimal monto, String hechaPor);
}
