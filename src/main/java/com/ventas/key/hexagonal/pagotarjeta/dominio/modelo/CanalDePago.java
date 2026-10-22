package com.ventas.key.hexagonal.pagotarjeta.dominio.modelo;

import java.time.Duration;

/**
 * Por donde se cobra: la terminal del local o la tienda en linea.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Cada canal tiene su propia vigencia (T7): en la terminal el cliente esta parado frente al admin;
 * en linea puede tardar mas en completar el pago. Un pedido de contado sin pagar admite los dos canales.
 */
public enum CanalDePago {
    TERMINAL(Duration.ofMinutes(10)),
    EN_LINEA(Duration.ofMinutes(30));

    private final Duration vigencia;

    CanalDePago(Duration vigencia) {
        this.vigencia = vigencia;
    }

    /** T7: pasado este tiempo sin respuesta, el intento expira y libera el pedido. */
    public Duration vigencia() {
        return vigencia;
    }
}
