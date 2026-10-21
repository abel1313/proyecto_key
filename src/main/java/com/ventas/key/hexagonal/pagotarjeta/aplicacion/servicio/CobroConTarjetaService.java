package com.ventas.key.hexagonal.pagotarjeta.aplicacion.servicio;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException.ConflictoDeConcurrencia;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CanalDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CobroEnPasarela;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoExterno;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoIntento;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.PedidoACobrar;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.ReglasMsi;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.CancelarCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.ConfirmarCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.ExpirarCobrosVencidosCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.IniciarCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.ReconciliarCobrosCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.ActualizarPedidoPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.IntentosDePagoPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PasarelaDePagoPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PedidoACobrarPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PlanesMsiPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.TransaccionPort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * [Hexagonal: implementacion de los puertos de entrada] [Clean: Use Case Interactor]
 *
 * <p>Orquesta: lee el pedido, deja que el modelo decida y habla con la pasarela. Las reglas
 * viven en {@link IntentoDePago}, {@link ReglasMsi} y {@code TipoDeCobro}.
 *
 * <h3>Principios que salen de las dos revisiones (no negociables)</h3>
 * <ol>
 *   <li><b>Nunca se da por "no cobrado" algo dudoso.</b> Si no se sabe si la pasarela cobro (tiempo
 *       agotado, red, caida del back), el cobro se queda activo y se vuelve a buscar por referencia;
 *       jamas se cancela ni se libera el pedido a ciegas (eso permitiria cobrar doble).</li>
 *   <li><b>Antes de expirar, cancelar o rechazar, se pregunta a la pasarela.</b> Si ya cobro, se
 *       aplica el pago (aprobacion tardia) en lugar de perderlo.</li>
 *   <li><b>Marcar el pedido pagado y guardar APROBADO van en una sola transaccion.</b> Dentro de ella no
 *       se llama a la pasarela.</li>
 *   <li><b>Concurrencia:</b> cada guardado lleva la version del intento; si alguien mas lo cambio
 *       se lanza {@link ConflictoDeConcurrencia} y {@code confirmar} simplemente devuelve el estado
 *       que ya dejo el otro.</li>
 *   <li><b>Doble pago:</b> si el pedido ya lo pago otro cobro, o su total cambio, el dinero no se aplica
 *       y el cobro queda COBRADO_SIN_APLICAR para devolverlo.</li>
 * </ol>
 *
 * <p>⚠️ Sin {@code @Service} a proposito: faltan adaptadores del pedido y de la pasarela real, y
 * Spring no arrancaria. Al conectarlo: {@code @Service}, un {@code @Bean Clock} y dos trabajos
 * {@code @Scheduled} que llamen {@link #expirarVencidos()} y {@link #reconciliar()}.
 */
public class CobroConTarjetaService
        implements IniciarCobroCasoUso, ConfirmarCobroCasoUso, CancelarCobroCasoUso, ExpirarCobrosVencidosCasoUso,
                   ReconciliarCobrosCasoUso {

    /** Rechazos del mismo pedido que activan el freno (regla heredada de la terminal actual). */
    static final int MAX_RECHAZOS = 3;
    static final Duration VENTANA_DE_RECHAZOS = Duration.ofHours(2);
    /** Cuanto tiempo atras se vuelve a preguntar por cobros que se dieron por perdidos. */
    static final Duration VENTANA_DE_RECONCILIACION = Duration.ofHours(24);

    private final PedidoACobrarPort pedidos;
    private final IntentosDePagoPort intentos;
    private final PlanesMsiPort planes;
    private final PasarelaDePagoPort pasarela;
    private final ActualizarPedidoPort actualizarPedido;
    private final TransaccionPort transaccion;
    private final Clock reloj;

    public CobroConTarjetaService(PedidoACobrarPort pedidos, IntentosDePagoPort intentos, PlanesMsiPort planes,
                                  PasarelaDePagoPort pasarela, ActualizarPedidoPort actualizarPedido,
                                  TransaccionPort transaccion, Clock reloj) {
        this.pedidos = pedidos;
        this.intentos = intentos;
        this.planes = planes;
        this.pasarela = pasarela;
        this.actualizarPedido = actualizarPedido;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    // ── Iniciar ───────────────────────────────────────────────────────────

    @Override
    public IntentoDePago iniciar(Integer pedidoId, CanalDePago canal, int cuotas, String iniciadoPor) {
        PedidoACobrar pedido = pedidos.buscar(pedidoId)
                .orElseThrow(() -> PagoConTarjetaException.pedidoNoExiste(pedidoId));
        if (pedido.pagado()) {
            throw PagoConTarjetaException.pedidoYaPagado(pedidoId);
        }
        if (!pedido.tipo().admiteTarjeta()) {                       // T3
            throw PagoConTarjetaException.tarjetaNoPermitida(pedido.tipo());
        }
        // Decidido con el dueño (2026-10-21): la tarjeta se usa cuando el cliente llega al local (terminal) o cuando
        // hace el pedido y paga el mismo desde la tienda (en linea). Un pedido de contado sin pagar admite
        // los dos canales; no hay forma de cobro aparte para "en linea".

        // T1: si hay un cobro activo, primero se le pregunta a la pasarela como va. Puede que ya este
        // pagado (entonces el pedido ya se marco pagado) o que haya vencido (entonces se libera).
        Optional<IntentoDePago> activo = intentos.activoDelPedido(pedidoId);
        if (activo.isPresent()) {
            IntentoDePago actual = confirmar(activo.get().id());
            if (actual.estado().estaCobrado()) {
                throw PagoConTarjetaException.pedidoYaPagado(pedidoId);
            }
            if (actual.estado().estaActivo()) {
                throw PagoConTarjetaException.intentoActivo(pedidoId);
            }
        }

        Instant ahora = reloj.instant();
        if (intentos.rechazadosDesde(pedidoId, ahora.minus(VENTANA_DE_RECHAZOS)) >= MAX_RECHAZOS) {
            throw PagoConTarjetaException.demasiadosRechazos();
        }

        ReglasMsi.validarCuotas(cuotas, pedido.tipo(), pedido.total(), pedido.articulos(),   // M1 a M6
                planes.planes(), planes.minimoParaMsi());

        // T2: el monto es el total del pedido, calculado aqui, nunca el del front. T1 tambien la
        // defiende la base (restriccion unica de un activo por pedido): un doble clic falla aqui.
        IntentoDePago creado = intentos.guardar(IntentoDePago.nuevo(
                pedidoId, pedido.clienteId(), canal, pedido.total(), cuotas, iniciadoPor, ahora));
        String idExterno;
        try {
            idExterno = pasarela.crearCobro(creado);
        } catch (PagoConTarjetaException.CobroNoCreado e) {
            intentos.guardar(creado.cancelado(reloj.instant()));       // la pasarela garantiza que no cobro
            throw e;
        } catch (RuntimeException e) {
            // NO se sabe si el cobro existe en Mercado Pago. Se deja CREADO (sigue ocupando el pedido)
            // y confirmar() lo busca por referencia. Cancelarlo aqui permitiria cobrar doble.
            throw PagoConTarjetaException.cobroIncierto();
        }
        return registrarEnvio(creado, idExterno);
    }

    /**
     * La pasarela ya tiene el cobro. Si otro proceso toco el intento mientras tanto (webhook, expiracion),
     * se conserva lo que ya hizo y se anota el id del cobro: perderlo dejaria un cobro vivo sin dueño.
     */
    private IntentoDePago registrarEnvio(IntentoDePago creado, String idExterno) {
        try {
            return intentos.guardar(creado.enviado(idExterno, reloj.instant()));
        } catch (ConflictoDeConcurrencia e) {
            IntentoDePago actual = buscar(creado.id());
            if (actual.idExterno() != null) {
                return actual;
            }
            IntentoDePago conId = actual.estado().estaActivo()
                    ? actual.enviado(idExterno, reloj.instant()) : actual.conIdExterno(idExterno);
            return confirmar(guardarOReleer(conId).id());
        }
    }

    // ── Confirmar ─────────────────────────────────────────────────────────

    @Override
    public IntentoDePago confirmar(Long intentoId) {
        IntentoDePago intento = buscar(intentoId);
        if (intento.estado().estaCobrado()) {
            return intento;                                         // T4: idempotente, no repite efectos
        }
        Instant ahora = reloj.instant();

        if (intento.idExterno() == null) {
            // Sin id externo: o la respuesta de crearlo se perdio, o nunca llego a crearse. Tambien los
            // cobros ya dados por perdidos se buscan: si la pasarela si lo cobro, es una aprobacion tardia.
            Optional<CobroEnPasarela> enPasarela = buscarPorReferencia(intento);
            if (enPasarela.isEmpty()) {
                // La pasarela no lo conoce. Si ya vencio, se libera; si no, se espera.
                return intento.haExpirado(ahora) ? guardarOReleer(intento.expirado(ahora)) : intento;
            }
            String idExterno = enPasarela.get().idExterno();
            intento = guardarOReleer(intento.estado().estaActivo()
                    ? intento.enviado(idExterno, ahora) : intento.conIdExterno(idExterno));
            if (intento.idExterno() == null) {
                return intento;                                     // otro proceso lo cambio; se vera en la siguiente
            }
        }

        EstadoExterno externo = consultar(intento.idExterno());
        return aplicar(intento, externo, ahora);
    }

    private IntentoDePago aplicar(IntentoDePago intento, EstadoExterno externo, Instant ahora) {
        boolean activo = intento.estado().estaActivo();
        return switch (externo) {
            case APROBADO -> aprobar(intento, ahora);               // tambien la aprobacion tardia
            case RECHAZADO -> {
                if (!activo) yield intento;
                // En linea el cliente puede reintentar con otra tarjeta dentro del mismo cobro: un rechazo
                // no lo termina, se sigue esperando hasta que venza. En la terminal si es definitivo.
                yield intento.canal() == CanalDePago.EN_LINEA
                        ? (intento.haExpirado(ahora) ? expirar(intento, ahora) : intento)
                        : guardarOReleer(intento.rechazado(ahora));
            }
            case CANCELADO -> activo ? guardarOReleer(intento.cancelado(ahora)) : intento;
            case PENDIENTE -> activo && intento.haExpirado(ahora) ? expirar(intento, ahora) : intento;
        };
    }

    /**
     * T4. La pasarela cobro. Si el pedido ya lo pago otro cobro (doble pago) o su total cambio desde
     * que se inicio, el dinero NO se aplica: queda COBRADO_SIN_APLICAR para devolverlo. Si no, el pedido
     * se marca pagado y el intento queda aprobado en UNA transaccion (los dos o ninguno).
     */
    private IntentoDePago aprobar(IntentoDePago intento, Instant ahora) {
        PedidoACobrar pedido = pedidos.buscar(intento.pedidoId())
                .orElseThrow(() -> PagoConTarjetaException.pedidoNoExiste(intento.pedidoId()));
        boolean loPagoOtro = pedido.pagado() && !Objects.equals(pedido.pagadoPorIntentoId(), intento.id());
        boolean cambioElTotal = !pedido.pagado() && pedido.total().compareTo(intento.monto()) != 0;
        if (loPagoOtro || cambioElTotal) {
            return guardarOReleer(intento.cobradoSinAplicar(ahora));
        }
        try {
            return transaccion.ejecutar(() -> {
                if (!pedido.pagado()) {
                    actualizarPedido.marcarPagado(intento.pedidoId(), intento.id());
                }
                return intentos.guardar(intento.aprobado(ahora));
            });
        } catch (ConflictoDeConcurrencia e) {
            return buscar(intento.id());                            // la transaccion se deshizo; otro ya lo resolvio
        }
    }

    /** Solo se llega aqui con la pasarela de acuerdo en que el cobro sigue PENDIENTE (o sin respuesta). */
    private IntentoDePago expirar(IntentoDePago intento, Instant ahora) {
        if (intento.idExterno() != null) {
            try {
                pasarela.cancelar(intento.idExterno());
            } catch (RuntimeException e) {
                // No se pudo cancelar: quiza ya se cobro. Se vuelve a preguntar antes de liberar nada.
                EstadoExterno otra = consultar(intento.idExterno());
                if (otra == EstadoExterno.APROBADO) {
                    return aprobar(intento, ahora);
                }
                if (otra == EstadoExterno.PENDIENTE) {
                    throw PagoConTarjetaException.pasarelaNoDisponible();   // sigue vivo: no se libera
                }
                // RECHAZADO o CANCELADO: ya no se puede cobrar, se libera.
            }
        }
        return guardarOReleer(intento.expirado(ahora));
    }

    // ── Cancelar ──────────────────────────────────────────────────────────

    @Override
    public IntentoDePago cancelar(Long intentoId) {
        IntentoDePago intento = buscar(intentoId);
        if (!intento.estado().estaActivo()) {
            throw PagoConTarjetaException.transicionInvalida(intento.estado(), EstadoIntento.CANCELADO);
        }
        Instant ahora = reloj.instant();
        if (intento.idExterno() == null) {
            // No se sabe si la pasarela lo creo. Se busca; si no aparece, NO se cancela a ciegas: se espera
            // a que confirmar() lo resuelva o venza (cancelar aqui permitiria cobrar y perder el rastro).
            Optional<CobroEnPasarela> enPasarela = buscarPorReferencia(intento);
            if (enPasarela.isEmpty()) {
                throw PagoConTarjetaException.cobroIncierto();
            }
            intento = guardarOReleer(intento.enviado(enPasarela.get().idExterno(), ahora));
            if (!intento.estado().estaActivo() || intento.idExterno() == null) {
                throw PagoConTarjetaException.transicionInvalida(intento.estado(), EstadoIntento.CANCELADO);
            }
        }
        if (consultar(intento.idExterno()) == EstadoExterno.APROBADO) {
            aprobar(intento, ahora);
            throw PagoConTarjetaException.yaCobrado();
        }
        try {
            pasarela.cancelar(intento.idExterno());
        } catch (RuntimeException e) {
            if (consultar(intento.idExterno()) == EstadoExterno.APROBADO) {
                aprobar(intento, ahora);
                throw PagoConTarjetaException.yaCobrado();
            }
            throw PagoConTarjetaException.pasarelaNoDisponible();
        }
        return guardarOReleer(intento.cancelado(reloj.instant()));
    }

    // ── Expirar (T7) ──────────────────────────────────────────────────────

    @Override
    public int expirarVencidos() {
        Instant ahora = reloj.instant();
        List<IntentoDePago> candidatos = intentos.activosCreadosAntesDe(
                ahora.minus(menorVigencia()));
        int resueltos = 0;
        for (IntentoDePago candidato : candidatos) {
            if (!candidato.haExpirado(ahora)) {
                continue;
            }
            try {
                if (!confirmar(candidato.id()).estado().estaActivo()) {
                    resueltos++;
                }
            } catch (RuntimeException e) {
                // Un cobro con problemas no debe detener a los demas; se reintenta en la siguiente corrida.
            }
        }
        return resueltos;
    }

    // ── Reconciliar ───────────────────────────────────────────────────────

    @Override
    public int reconciliar() {
        Instant ahora = reloj.instant();
        int aplicados = 0;
        for (IntentoDePago perdido : intentos.perdidosDesde(ahora.minus(VENTANA_DE_RECONCILIACION))) {
            try {
                if (confirmar(perdido.id()).estado().estaCobrado()) {
                    aplicados++;
                }
            } catch (RuntimeException e) {
                // Se reintenta en la siguiente corrida; un cobro con problemas no detiene a los demas.
            }
        }
        return aplicados;
    }

    private static Duration menorVigencia() {
        Duration menor = null;
        for (CanalDePago c : CanalDePago.values()) {
            if (menor == null || c.vigencia().compareTo(menor) < 0) {
                menor = c.vigencia();
            }
        }
        return menor;
    }

    // ── Apoyo ─────────────────────────────────────────────────────────────

    private EstadoExterno consultar(String idExterno) {
        try {
            return pasarela.consultar(idExterno);
        } catch (RuntimeException e) {
            throw PagoConTarjetaException.pasarelaNoDisponible();
        }
    }

    private Optional<CobroEnPasarela> buscarPorReferencia(IntentoDePago intento) {
        try {
            return pasarela.buscarPorReferencia(intento.id());
        } catch (RuntimeException e) {
            throw PagoConTarjetaException.pasarelaNoDisponible();
        }
    }

    /** Si otro proceso cambio el intento mientras tanto, se devuelve lo que ese proceso dejo. */
    private IntentoDePago guardarOReleer(IntentoDePago nuevo) {
        try {
            return intentos.guardar(nuevo);
        } catch (ConflictoDeConcurrencia e) {
            return buscar(nuevo.id());
        }
    }

    private IntentoDePago buscar(Long intentoId) {
        return intentos.buscar(intentoId).orElseThrow(() -> PagoConTarjetaException.intentoNoExiste(intentoId));
    }
}
