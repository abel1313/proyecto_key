package com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.Devolucion;

import java.util.List;

/**
 * Guarda las devoluciones (reservadas y confirmadas). Solo persistencia.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface DevolucionesPort {

    /**
     * Reserva la devolucion. La clave es unica: si ya existe, lanza
     * {@link com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException.ConflictoDeConcurrencia}
     * (otra solicitud ya la tomo).
     */
    Devolucion reservar(Devolucion devolucion);

    /** Marca la devolucion como confirmada (la pasarela la acepto). Idempotente. */
    Devolucion confirmar(Devolucion devolucion);

    /** Devoluciones reservadas que nunca se confirmaron, del cobro dado. */
    List<Devolucion> pendientesDelIntento(Long intentoId);
}
