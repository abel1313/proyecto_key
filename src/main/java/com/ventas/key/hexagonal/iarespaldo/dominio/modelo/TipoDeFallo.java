package com.ventas.key.hexagonal.iarespaldo.dominio.modelo;

import java.time.Duration;

/** Por qué falló un proveedor de IA y cuánto conviene dejarlo descansar antes de volver a probarlo. */
public enum TipoDeFallo {

    /** Llave inválida, sin saldo o sin permiso (401, 402, 403, o 429 por cuota agotada). Se arregla a mano. */
    SIN_SALDO_O_LLAVE(true),
    /** Demasiadas peticiones por minuto (429): pasa solo en poco tiempo. */
    LIMITE_TEMPORAL(true),
    /** Servidor caído, lento o que no contestó (5xx, tiempo agotado, sin conexión, respuesta vacía). */
    CAIDO(true),
    /** Error nuestro (400, 404, 422...): otro proveedor fallaría igual, así que no se cambia. */
    NUESTRO(false);

    private final boolean cambiaDeProveedor;

    TipoDeFallo(boolean cambiaDeProveedor) {
        this.cambiaDeProveedor = cambiaDeProveedor;
    }

    public boolean cambiaDeProveedor() {
        return cambiaDeProveedor;
    }

    public Duration pausa(Duration pausaLarga) {
        return switch (this) {
            case SIN_SALDO_O_LLAVE -> pausaLarga;
            case LIMITE_TEMPORAL, CAIDO -> Duration.ofMinutes(1);
            case NUESTRO -> Duration.ZERO;
        };
    }
}
