package com.ventas.key.hexagonal.pagotarjeta.dominio.modelo;

import java.util.Set;

/**
 * Estados de un intento de pago y a cuales puede pasar.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <pre>
 *  CREADO ──▶ ENVIADO ──▶ APROBADO ──▶ DEVUELTO_PARCIAL ──▶ DEVUELTO
 *     │          │            └────────────────────────────▶ DEVUELTO
 *     └──────────┴──▶ RECHAZADO | CANCELADO | EXPIRADO
 *                          └─────────── (aprobacion tardia) ──▶ APROBADO | COBRADO_SIN_APLICAR
 * </pre>
 *
 * <p><b>COBRADO_SIN_APLICAR:</b> la pasarela cobro, pero el pedido ya estaba pagado por otro cobro (doble
 * pago) o su total cambio. El dinero NO se aplica al pedido y queda pendiente de devolver.
 *
 * <p><b>Aprobacion tardia:</b> RECHAZADO, CANCELADO y EXPIRADO pueden pasar a APROBADO. Pasa cuando
 * la pasarela cobro aunque aqui ya se hubiera dado por perdido (el cliente paga justo al vencer;
 * en Checkout Pro reintenta en la misma preferencia). Si no existiera, ese dinero cobrado se
 * perderia sin que el pedido se enterara.
 */
public enum EstadoIntento {
    CREADO("creado"),
    ENVIADO("en proceso"),
    APROBADO("aprobado"),
    COBRADO_SIN_APLICAR("cobrado sin aplicar al pedido"),
    RECHAZADO("rechazado"),
    CANCELADO("cancelado"),
    EXPIRADO("vencido"),
    DEVUELTO_PARCIAL("devuelto en parte"),
    DEVUELTO("devuelto");

    private final String etiqueta;

    EstadoIntento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Como lo dice el dueño; es lo que ven los mensajes. */
    public String etiqueta() {
        return etiqueta;
    }

    /** Sigue esperando respuesta: ocupa al pedido (T1). */
    public boolean estaActivo() {
        return this == CREADO || this == ENVIADO;
    }

    /** Se cobro y se puede devolver, total o parcialmente. */
    public boolean admiteDevolucion() {
        return this == APROBADO || this == COBRADO_SIN_APLICAR || this == DEVUELTO_PARCIAL;
    }

    /** Ya se cobro (o se cobro y se devolvio): no hay nada mas que confirmar. */
    public boolean estaCobrado() {
        return this == APROBADO || this == COBRADO_SIN_APLICAR || this == DEVUELTO_PARCIAL || this == DEVUELTO;
    }

    /** Se dio por perdido, pero la pasarela todavia podria haberlo cobrado (aprobacion tardia). */
    public boolean admiteAprobacionTardia() {
        return this == RECHAZADO || this == CANCELADO || this == EXPIRADO;
    }

    public boolean puedePasarA(EstadoIntento nuevo) {
        return destinosPermitidos().contains(nuevo);
    }

    private Set<EstadoIntento> destinosPermitidos() {
        return switch (this) {
            case CREADO -> Set.of(ENVIADO, RECHAZADO, CANCELADO, EXPIRADO);
            case ENVIADO -> Set.of(APROBADO, COBRADO_SIN_APLICAR, RECHAZADO, CANCELADO, EXPIRADO);
            case RECHAZADO, CANCELADO, EXPIRADO -> Set.of(APROBADO, COBRADO_SIN_APLICAR);
            case APROBADO, COBRADO_SIN_APLICAR, DEVUELTO_PARCIAL -> Set.of(DEVUELTO_PARCIAL, DEVUELTO);
            case DEVUELTO -> Set.of();
        };
    }
}
