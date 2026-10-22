package com.ventas.key.hexagonal.pagotarjeta.dominio.modelo;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Un intento de cobrar un pedido con tarjeta, por terminal o en linea.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Inmutable: cada cambio de estado devuelve un intento nuevo y rechaza los que el flujo no
 * permite ({@link EstadoIntento}). No guarda datos de tarjeta, nunca (T6).
 *
 * <p>{@code version} es el candado optimista: el adaptador de persistencia rechaza guardar un
 * intento si alguien mas lo cambio desde que se leyo (polling y webhook a la vez, dos admins).
 */
public final class IntentoDePago {

    private final Long id;
    private final Integer pedidoId;
    private final Integer clienteId;
    private final CanalDePago canal;
    private final BigDecimal monto;
    private final int cuotas;
    private final EstadoIntento estado;
    private final String idExterno;
    private final String iniciadoPor;
    private final Instant creadoEn;
    private final Instant actualizadoEn;
    private final BigDecimal devuelto;
    private final long version;

    private IntentoDePago(Long id, Integer pedidoId, Integer clienteId, CanalDePago canal, BigDecimal monto,
                          int cuotas, EstadoIntento estado, String idExterno, String iniciadoPor,
                          Instant creadoEn, Instant actualizadoEn, BigDecimal devuelto, long version) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.clienteId = clienteId;
        this.canal = canal;
        this.monto = monto;
        this.cuotas = cuotas;
        this.estado = estado;
        this.idExterno = idExterno;
        this.iniciadoPor = iniciadoPor;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
        this.devuelto = devuelto;
        this.version = version;
    }

    /** Un cobro recien creado, todavia no enviado a la pasarela. */
    public static IntentoDePago nuevo(Integer pedidoId, Integer clienteId, CanalDePago canal, BigDecimal monto,
                                      int cuotas, String iniciadoPor, Instant ahora) {
        if (pedidoId == null) throw PagoConTarjetaException.datosInvalidos("falta el pedido");
        if (canal == null) throw PagoConTarjetaException.datosInvalidos("falta el canal de pago");
        if (monto == null || monto.signum() <= 0) {
            throw PagoConTarjetaException.datosInvalidos("el monto debe ser mayor a 0");
        }
        if (cuotas < 1) throw PagoConTarjetaException.cuotasInvalidas(cuotas);
        if (iniciadoPor == null || iniciadoPor.isBlank()) {
            throw PagoConTarjetaException.datosInvalidos("falta quién inició el cobro");
        }
        Instant momento = sinFraccion(ahora);
        return new IntentoDePago(null, pedidoId, clienteId, canal, monto.setScale(2, RoundingMode.HALF_UP), cuotas,
                EstadoIntento.CREADO, null, iniciadoPor, momento, momento, BigDecimal.ZERO.setScale(2), 0L);
    }

    /** Para que el adaptador de persistencia reconstruya un intento ya guardado. */
    public static IntentoDePago reconstruir(Long id, Integer pedidoId, Integer clienteId, CanalDePago canal,
                                            BigDecimal monto, int cuotas, EstadoIntento estado, String idExterno,
                                            String iniciadoPor, Instant creadoEn, Instant actualizadoEn,
                                            BigDecimal devuelto, long version) {
        return new IntentoDePago(id, pedidoId, clienteId, canal, monto, cuotas, estado, idExterno, iniciadoPor,
                creadoEn, actualizadoEn, devuelto == null ? BigDecimal.ZERO.setScale(2) : devuelto, version);
    }

    public IntentoDePago conId(Long nuevoId) {
        return new IntentoDePago(nuevoId, pedidoId, clienteId, canal, monto, cuotas, estado, idExterno,
                iniciadoPor, creadoEn, actualizadoEn, devuelto, version);
    }

    public IntentoDePago enviado(String idExternoNuevo, Instant ahora) {
        if (idExternoNuevo == null || idExternoNuevo.isBlank()) {
            throw PagoConTarjetaException.datosInvalidos("la pasarela no devolvió el identificador del cobro");
        }
        return paso(EstadoIntento.ENVIADO, idExternoNuevo, ahora, devuelto);
    }

    /**
     * Recuerda el id del cobro encontrado por referencia sin cambiar el estado (sirve para cobros
     * ya cancelados o vencidos de los que se perdio la respuesta al crearlos).
     */
    public IntentoDePago conIdExterno(String idExternoNuevo) {
        if (idExternoNuevo == null || idExternoNuevo.isBlank()) {
            throw PagoConTarjetaException.datosInvalidos("la pasarela no devolvió el identificador del cobro");
        }
        return new IntentoDePago(id, pedidoId, clienteId, canal, monto, cuotas, estado, idExternoNuevo, iniciadoPor,
                creadoEn, actualizadoEn, devuelto, version);
    }

    /** Tambien sirve para una aprobacion tardia (RECHAZADO, CANCELADO o EXPIRADO que la pasarela si cobro). */
    public IntentoDePago aprobado(Instant ahora) {
        return paso(EstadoIntento.APROBADO, idExterno, ahora, devuelto);
    }

    /** El cobro se hizo en la pasarela pero NO se aplico al pedido (ya estaba pagado o cambio de total). */
    public IntentoDePago cobradoSinAplicar(Instant ahora) {
        return paso(EstadoIntento.COBRADO_SIN_APLICAR, idExterno, ahora, devuelto);
    }

    public IntentoDePago rechazado(Instant ahora) {
        return paso(EstadoIntento.RECHAZADO, idExterno, ahora, devuelto);
    }

    public IntentoDePago cancelado(Instant ahora) {
        return paso(EstadoIntento.CANCELADO, idExterno, ahora, devuelto);
    }

    public IntentoDePago expirado(Instant ahora) {
        return paso(EstadoIntento.EXPIRADO, idExterno, ahora, devuelto);
    }

    /**
     * T8: devuelve {@code cantidad} (total o parcial). Con la ultima parte queda DEVUELTO. La
     * cantidad tiene que venir con a lo mas dos decimales: lo que se manda a la pasarela es
     * exactamente lo que se guarda.
     */
    public IntentoDePago devolviendo(BigDecimal cantidad, Instant ahora) {
        if (!estado.admiteDevolucion()) {
            throw PagoConTarjetaException.devolucionInvalida(
                    "solo se devuelve un cobro aprobado (este está " + estado.etiqueta() + ")");
        }
        if (cantidad == null || cantidad.signum() <= 0) {
            throw PagoConTarjetaException.devolucionInvalida("el monto a devolver debe ser mayor a 0");
        }
        if (cantidad.stripTrailingZeros().scale() > 2) {
            throw PagoConTarjetaException.devolucionInvalida("el monto solo puede tener centavos (dos decimales)");
        }
        BigDecimal exacta = cantidad.setScale(2, RoundingMode.UNNECESSARY);
        if (exacta.compareTo(restanteADevolver()) > 0) {
            throw PagoConTarjetaException.devolucionInvalida(
                    "máximo " + PagoConTarjetaException.pesos(restanteADevolver()) + " (ya se devolvió "
                            + PagoConTarjetaException.pesos(devuelto) + " de " + PagoConTarjetaException.pesos(monto) + ")");
        }
        BigDecimal nuevoDevuelto = devuelto.add(exacta).setScale(2, RoundingMode.HALF_UP);
        EstadoIntento nuevo = nuevoDevuelto.compareTo(monto) == 0
                ? EstadoIntento.DEVUELTO : EstadoIntento.DEVUELTO_PARCIAL;
        return paso(nuevo, idExterno, ahora, nuevoDevuelto);
    }

    private IntentoDePago paso(EstadoIntento nuevo, String idExternoNuevo, Instant ahora, BigDecimal devueltoNuevo) {
        if (!estado.puedePasarA(nuevo)) {
            throw PagoConTarjetaException.transicionInvalida(estado, nuevo);
        }
        return new IntentoDePago(id, pedidoId, clienteId, canal, monto, cuotas, nuevo, idExternoNuevo, iniciadoPor,
                creadoEn, sinFraccion(ahora), devueltoNuevo, version);
    }

    /** T7: sigue activo y ya paso su vigencia sin respuesta. */
    public boolean haExpirado(Instant ahora) {
        return estado.estaActivo() && ahora.isAfter(creadoEn.plus(canal.vigencia()));
    }

    public BigDecimal restanteADevolver() {
        return monto.subtract(devuelto);
    }

    /** Milisegundos como maximo: lo que la base guarda (DATETIME(3)) vuelve igual a como se escribio. */
    private static Instant sinFraccion(Instant i) {
        return i.truncatedTo(ChronoUnit.MILLIS);
    }

    public Long id() { return id; }
    public Integer pedidoId() { return pedidoId; }
    public Integer clienteId() { return clienteId; }
    public CanalDePago canal() { return canal; }
    public BigDecimal monto() { return monto; }
    public int cuotas() { return cuotas; }
    public EstadoIntento estado() { return estado; }
    public String idExterno() { return idExterno; }
    public String iniciadoPor() { return iniciadoPor; }
    public Instant creadoEn() { return creadoEn; }
    public Instant actualizadoEn() { return actualizadoEn; }
    public BigDecimal devuelto() { return devuelto; }
    public long version() { return version; }
}
