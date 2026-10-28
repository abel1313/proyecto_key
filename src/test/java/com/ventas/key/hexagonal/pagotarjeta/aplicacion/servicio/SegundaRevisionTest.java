package com.ventas.key.hexagonal.pagotarjeta.aplicacion.servicio;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CanalDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CobroEnPasarela;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.Devolucion;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoExterno;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoIntento;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.TipoDeCobro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Hallazgos de la segunda revision: cada prueba reproduce un escenario que antes perdia dinero o el rastro. */
class SegundaRevisionTest {

    private Escenario nuevo() {
        Escenario e = new Escenario();
        e.pedidos.agregar(Escenario.pedido(1, TipoDeCobro.CONTADO, "450.00", true, true));
        return e;
    }

    private IntentoDePago dudoso(Escenario e) {
        e.pasarela.creaYPierdeLaRespuesta = true;
        assertThatThrownBy(() -> e.cobro.iniciar(1, CanalDePago.TERMINAL, 1, "admin"))
                .isInstanceOf(PagoConTarjetaException.class);
        e.pasarela.creaYPierdeLaRespuesta = false;
        return e.intentos.ultimo();
    }

    // H1: cancelar sin id externo

    @Test
    void cancelarUnCobroSinIdExterno_noLoCancelaALaCiega() {
        Escenario e = nuevo();
        IntentoDePago d = dudoso(e);
        e.pasarela.porReferencia.clear();                                          // la pasarela no lo encuentra
        assertThatThrownBy(() -> e.cobro.cancelar(d.id())).hasMessageContaining("NO lo vuelvas a intentar");
        assertThat(e.intentos.buscar(d.id()).orElseThrow().estado()).isEqualTo(EstadoIntento.CREADO);
    }

    @Test
    void cancelarUnCobroSinIdExterno_siLaPasarelaYaLoCobro_seAplicaEnLugarDeCancelar() {
        Escenario e = nuevo();
        IntentoDePago d = dudoso(e);
        e.pasarela.respuesta = EstadoExterno.APROBADO;
        assertThatThrownBy(() -> e.cobro.cancelar(d.id())).hasMessageContaining("ya se aprobó");
        assertThat(e.intentos.buscar(d.id()).orElseThrow().estado()).isEqualTo(EstadoIntento.APROBADO);
        assertThat(e.pedidoActualizado.intentosPagados).containsExactly(d.id());
    }

    @Test
    void cancelarUnCobroSinIdExterno_siLaPasarelaLoTieneYSigueVivo_seCancelaEnLaPasarela() {
        Escenario e = nuevo();
        IntentoDePago d = dudoso(e);
        IntentoDePago c = e.cobro.cancelar(d.id());
        assertThat(c.estado()).isEqualTo(EstadoIntento.CANCELADO);
        assertThat(e.pasarela.cancelaciones).isEqualTo(1);
        assertThat(c.idExterno()).isEqualTo("ext-1");
    }

    // H5: aprobacion tardia de un cobro sin id externo

    @Test
    void unCobroVencidoSinIdExterno_siLaPasarelaLoCobroDespues_seAplica() {
        Escenario e = nuevo();
        IntentoDePago d = dudoso(e);
        e.pasarela.porReferencia.clear();
        e.reloj.avanzarMinutos(11);
        assertThat(e.cobro.confirmar(d.id()).estado()).isEqualTo(EstadoIntento.EXPIRADO);

        e.pasarela.porReferencia.put(d.id(), new CobroEnPasarela("ext-9", EstadoExterno.APROBADO));
        e.pasarela.respuesta = EstadoExterno.APROBADO;
        IntentoDePago r = e.cobro.confirmar(d.id());
        assertThat(r.estado()).isEqualTo(EstadoIntento.APROBADO);
        assertThat(r.idExterno()).isEqualTo("ext-9");
        assertThat(e.pedidoActualizado.intentosPagados).containsExactly(d.id());
    }

    // H6: conflicto al guardar el envio

    @Test
    void siAlGuardarElEnvioOtroProcesoYaCambioElIntento_noSePierdeElCobro() {
        Escenario e = nuevo();
        e.intentos.otroProcesoSeAdelanta = viejo -> e.intentos.porId.put(viejo.id(), IntentoDePago.reconstruir(
                viejo.id(), viejo.pedidoId(), viejo.clienteId(), viejo.canal(), viejo.monto(), viejo.cuotas(),
                EstadoIntento.CREADO, null, viejo.iniciadoPor(), viejo.creadoEn(), viejo.actualizadoEn(),
                viejo.devuelto(), viejo.version() + 1));
        IntentoDePago r = e.cobro.iniciar(1, CanalDePago.TERMINAL, 1, "admin");
        assertThat(r.idExterno()).isEqualTo("ext-1");
        assertThat(r.estado()).isEqualTo(EstadoIntento.ENVIADO);
    }

    // H4: doble pago y cambio de total

    @Test
    void siElPedidoYaLoPagoOtroCobro_elDineroNoSeAplicaYQuedaPendienteDeDevolver() {
        Escenario e = nuevo();
        IntentoDePago primero = e.cobro.iniciar(1, CanalDePago.TERMINAL, 1, "admin");
        e.pasarela.respuesta = EstadoExterno.PENDIENTE;
        e.reloj.avanzarMinutos(11);
        assertThat(e.cobro.confirmar(primero.id()).estado()).isEqualTo(EstadoIntento.EXPIRADO);
        IntentoDePago segundo = e.cobro.iniciar(1, CanalDePago.TERMINAL, 1, "admin");
        e.pasarela.respuesta = EstadoExterno.APROBADO;
        assertThat(e.cobro.confirmar(segundo.id()).estado()).isEqualTo(EstadoIntento.APROBADO);

        IntentoDePago tardio = e.cobro.confirmar(primero.id());               // el primero llega tarde y tambien se cobro
        assertThat(tardio.estado()).isEqualTo(EstadoIntento.COBRADO_SIN_APLICAR);
        assertThat(e.pedidoActualizado.intentosPagados).containsExactly(segundo.id());

        IntentoDePago devuelto = e.devolucion.devolver(primero.id(), null, "admin");
        assertThat(devuelto.estado()).isEqualTo(EstadoIntento.DEVUELTO);
        assertThat(e.pasarela.devoluciones).hasSize(1);
        assertThat(e.pedidoActualizado.devueltos).isEmpty();                   // el pedido no se toca
    }

    @Test
    void siElTotalDelPedidoCambioMientrasSeCobraba_noSeMarcaPagado() {
        Escenario e = nuevo();
        IntentoDePago i = e.cobro.iniciar(1, CanalDePago.TERMINAL, 1, "admin");
        e.pedidos.agregar(Escenario.pedido(1, TipoDeCobro.CONTADO, "600.00", true, true));
        e.pasarela.respuesta = EstadoExterno.APROBADO;
        assertThat(e.cobro.confirmar(i.id()).estado()).isEqualTo(EstadoIntento.COBRADO_SIN_APLICAR);
        assertThat(e.pedidoActualizado.intentosPagados).isEmpty();
    }

    @Test
    void confirmarDosVecesElMismoCobroAprobado_noLoTomaPorDoblePago() {
        Escenario e = nuevo();
        IntentoDePago i = e.cobro.iniciar(1, CanalDePago.TERMINAL, 1, "admin");
        e.pasarela.respuesta = EstadoExterno.APROBADO;
        e.cobro.confirmar(i.id());
        assertThat(e.cobro.confirmar(i.id()).estado()).isEqualTo(EstadoIntento.APROBADO);
        assertThat(e.pedidoActualizado.llamadasMarcarPagado).isEqualTo(1);
    }

    // Devoluciones

    @Test
    void dosSolicitudesDeLaMismaDevolucion_laSegundaNoReservaNiLlamaALaPasarela() {
        Escenario e = nuevo();
        IntentoDePago i = e.cobro.iniciar(1, CanalDePago.TERMINAL, 1, "admin");
        e.pasarela.respuesta = EstadoExterno.APROBADO;
        e.cobro.confirmar(i.id());
        e.devoluciones.reservar(Devolucion.reservada(
                i.id(), new BigDecimal("100.00"), "otro", "dev-" + i.id() + "-0.00", e.reloj.instant()));
        e.devoluciones.porId.replaceAll((k, d) -> d.comoConfirmada());          // ya no esta pendiente
        assertThatThrownBy(() -> e.devolucion.devolver(i.id(), new BigDecimal("100"), "admin"))
                .isInstanceOf(PagoConTarjetaException.ConflictoDeConcurrencia.class);
        assertThat(e.pasarela.clavesUsadas).isEmpty();
    }

    @Test
    void siLaPasarelaFallaAlDevolver_laReservaQuedaPendienteYSeReintentaConLaMismaClave() {
        Escenario e = nuevo();
        IntentoDePago i = e.cobro.iniciar(1, CanalDePago.TERMINAL, 1, "admin");
        e.pasarela.respuesta = EstadoExterno.APROBADO;
        e.cobro.confirmar(i.id());
        e.pasarela.caida = true;
        assertThatThrownBy(() -> e.devolucion.devolver(i.id(), new BigDecimal("100"), "admin"))
                .hasMessageContaining("No se pudo comunicar");
        assertThat(e.devoluciones.pendientesDelIntento(i.id())).hasSize(1);
        e.pasarela.caida = false;
        assertThatThrownBy(() -> e.devolucion.devolver(i.id(), new BigDecimal("100"), "admin"))
                .hasMessageContaining("a medias");
        assertThat(e.pasarela.devoluciones).hasSize(1);
        assertThat(e.intentos.buscar(i.id()).orElseThrow().devuelto()).isEqualByComparingTo("100");
    }

    @Test
    void siSeCaeElGuardadoTrasDevolverEnLaPasarela_seTerminaConLaMismaClaveSinDevolverDeMas() {
        Escenario e = nuevo();
        IntentoDePago i = e.cobro.iniciar(1, CanalDePago.TERMINAL, 1, "admin");
        e.pasarela.respuesta = EstadoExterno.APROBADO;
        e.cobro.confirmar(i.id());
        e.intentos.fallarProximosGuardados = 1;
        assertThatThrownBy(() -> e.devolucion.devolver(i.id(), new BigDecimal("200"), "admin")).hasMessageContaining("se cayo");
        assertThat(e.devoluciones.pendientesDelIntento(i.id())).hasSize(1);
        assertThatThrownBy(() -> e.devolucion.devolver(i.id(), new BigDecimal("200"), "admin")).hasMessageContaining("a medias");

        assertThat(e.pasarela.clavesUsadas).hasSize(2).containsOnly("dev-" + i.id() + "-0.00");
        assertThat(e.pasarela.devoluciones).hasSize(1);                          // solo se devolvio una vez
        assertThat(e.intentos.buscar(i.id()).orElseThrow().devuelto()).isEqualByComparingTo("200");
        assertThat(e.devoluciones.pendientesDelIntento(i.id())).isEmpty();
        assertThat(e.pedidoActualizado.devueltos).hasSize(1);
    }

    // En linea

    @Test
    void enLineaUnCobroRechazadoSigueVivoHastaQueVence() {
        Escenario e = nuevo();
        e.pedidos.agregar(Escenario.pedido(6, TipoDeCobro.CONTADO, "450", true));
        IntentoDePago i = e.cobro.iniciar(6, CanalDePago.EN_LINEA, 1, "cliente");
        e.pasarela.respuesta = EstadoExterno.RECHAZADO;
        assertThat(e.cobro.confirmar(i.id()).estado()).isEqualTo(EstadoIntento.ENVIADO);
        e.reloj.avanzarMinutos(31);
        assertThat(e.cobro.confirmar(i.id()).estado()).isEqualTo(EstadoIntento.EXPIRADO);
    }

    // Reconciliacion

    @Test
    void reconciliarEncuentraLosCobrosPerdidosQueLaPasarelaSiCobro() {
        Escenario e = nuevo();
        IntentoDePago i = e.cobro.iniciar(1, CanalDePago.TERMINAL, 1, "admin");
        e.pasarela.respuesta = EstadoExterno.PENDIENTE;
        e.reloj.avanzarMinutos(11);
        e.cobro.confirmar(i.id());                                               // EXPIRADO
        assertThat(e.cobro.reconciliar()).isZero();
        e.pasarela.respuesta = EstadoExterno.APROBADO;                           // el webhook nunca llego
        assertThat(e.cobro.reconciliar()).isEqualTo(1);
        assertThat(e.intentos.buscar(i.id()).orElseThrow().estado()).isEqualTo(EstadoIntento.APROBADO);
        assertThat(e.cobro.reconciliar()).isEqualTo(0);
    }

    // ── Tercera revision (agente revisor): hallazgos reproducidos ─────────

    @Test
    void siElPedidoYaNoAdmiteElPagoCuandoSeBloquea_queda_cobradoSinAplicar_yNoAprobado() {
        Escenario e = nuevo();
        IntentoDePago i = e.cobro.iniciar(1, CanalDePago.TERMINAL, 1, "admin");
        e.pedidoActualizado.noAplicable = true;                                // cancelado / abonado / ya pagado en efectivo
        e.pasarela.respuesta = EstadoExterno.APROBADO;
        assertThat(e.cobro.confirmar(i.id()).estado()).isEqualTo(EstadoIntento.COBRADO_SIN_APLICAR);
        assertThat(e.pedidoActualizado.intentosPagados).isEmpty();
    }

    @Test
    void siFaltanDatosParaLaVenta_elCobroNoQuedaEnProcesoParaSiempre_quedaPorDevolver() {
        Escenario e = nuevo();
        IntentoDePago i = e.cobro.iniciar(1, CanalDePago.TERMINAL, 1, "admin");
        e.pedidoActualizado.faltanDatos = true;
        e.pasarela.respuesta = EstadoExterno.APROBADO;
        assertThat(e.cobro.confirmar(i.id()).estado()).isEqualTo(EstadoIntento.COBRADO_SIN_APLICAR);
        // y el pedido queda libre de cobros activos
        assertThat(e.intentos.activoDelPedido(1)).isEmpty();
    }

    @Test
    void unPedidoEnGrupoNoSeCobraConTarjeta() {
        Escenario e = nuevo();
        e.pedidos.agregar(new com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.PedidoACobrar(
                8, 50, TipoDeCobro.CONTADO, new BigDecimal("450"), false, false, true, null, java.util.List.of()));
        assertThatThrownBy(() -> e.cobro.iniciar(8, CanalDePago.TERMINAL, 1, "admin")).hasMessageContaining("grupo");
        assertThat(e.pasarela.cobrosCreados).isZero();
    }

    @Test
    void alAplicarUnaAprobacionTardia_seCancelaElOtroCobroQueSeguiaActivo() {
        Escenario e = nuevo();
        IntentoDePago a = e.cobro.iniciar(1, CanalDePago.EN_LINEA, 1, "cliente");
        e.pasarela.respuesta = EstadoExterno.PENDIENTE;
        e.reloj.avanzarMinutos(31);
        assertThat(e.cobro.confirmar(a.id()).estado()).isEqualTo(EstadoIntento.EXPIRADO);
        IntentoDePago b = e.cobro.iniciar(1, CanalDePago.TERMINAL, 1, "admin");   // en la terminal, mientras A estaba vencido

        e.pasarela.respuesta = EstadoExterno.APROBADO;                           // pero A si se cobro
        // solo A responde aprobado; B queda pendiente en la pasarela falsa (respuesta global): se simula con el orden
        assertThat(e.cobro.confirmar(a.id()).estado()).isEqualTo(EstadoIntento.APROBADO);
        assertThat(e.intentos.buscar(b.id()).orElseThrow().estado()).isEqualTo(EstadoIntento.CANCELADO);
        assertThat(e.pasarela.cancelaciones).isGreaterThanOrEqualTo(1);
    }

    // ── Los dos caminos de la terminal no cobran el mismo pedido a la vez ─────────────────

    @Test
    void noSeCobraConElModuloNuevoSiElCaminoAnteriorTieneUnCobroEnProceso() {
        Escenario e = new Escenario();
        e.pedidos.agregar(Escenario.pedido(1, TipoDeCobro.CONTADO, "450", true));
        e.pedidos.conCobroDelCaminoAnterior.add(1);
        assertThatThrownBy(() -> e.cobro.iniciar(1, CanalDePago.TERMINAL, 1, "admin"))
                .isInstanceOf(PagoConTarjetaException.class)
                .hasMessageContaining("camino anterior");
        assertThat(e.intentos.porId).isEmpty();           // no se creó ningún cobro ni se tocó la pasarela
    }

    @Test
    void sinCobroDelCaminoAnteriorElCobroNuevoSigueFuncionando() {
        Escenario e = new Escenario();
        e.pedidos.agregar(Escenario.pedido(2, TipoDeCobro.CONTADO, "450", true));
        assertThat(e.cobro.iniciar(2, CanalDePago.TERMINAL, 1, "admin").estado().estaActivo()).isTrue();
    }
}
