package com.ventas.key.hexagonal.pagotarjeta.aplicacion.servicio;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CanalDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoExterno;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoIntento;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.PedidoACobrar;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.TipoDeCobro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CobroConTarjetaServiceTest {

    private Escenario nuevo() {
        Escenario e = new Escenario();
        e.pedidos.agregar(Escenario.pedido(1, TipoDeCobro.CONTADO, "450.00", true, true));
        return e;
    }

    private IntentoDePago iniciarEnTerminal(Escenario e) {
        return e.cobro.iniciar(1, CanalDePago.TERMINAL, 1, "admin");
    }

    private IntentoDePago aprobado(Escenario e) {
        IntentoDePago i = iniciarEnTerminal(e);
        e.pasarela.respuesta = EstadoExterno.APROBADO;
        return e.cobro.confirmar(i.id());
    }

    // ── Iniciar ───────────────────────────────────────────────────────────

    @Test
    void iniciaElCobroConElTotalDelPedido_T2() {
        Escenario e = nuevo();
        IntentoDePago i = iniciarEnTerminal(e);
        assertThat(i.estado()).isEqualTo(EstadoIntento.ENVIADO);
        assertThat(i.idExterno()).isEqualTo("ext-1");
        assertThat(i.clienteId()).isEqualTo(50);
        assertThat(e.pasarela.montoCobrado).isEqualByComparingTo("450.00");
    }

    @Test
    void tarjetaNoSePermiteEnApartadoNiIrPagando_T3() {
        Escenario e = nuevo();
        e.pedidos.agregar(Escenario.pedido(2, TipoDeCobro.APARTADO, "450", true));
        e.pedidos.agregar(Escenario.pedido(3, TipoDeCobro.IR_PAGANDO, "450", true));
        assertThatThrownBy(() -> e.cobro.iniciar(2, CanalDePago.TERMINAL, 1, "admin"))
                .isInstanceOf(PagoConTarjetaException.class).hasMessageContaining("Apartado es sin dinero");
        assertThatThrownBy(() -> e.cobro.iniciar(3, CanalDePago.EN_LINEA, 1, "admin"))
                .hasMessageContaining("abonos en efectivo o transferencia");
        assertThat(e.pasarela.cobrosCreados).isZero();
    }

    @Test
    void unPedidoDeContadoAdmiteLosDosCanales_laTerminalYLaTiendaEnLinea() {
        Escenario e = nuevo();
        e.pedidos.agregar(Escenario.pedido(4, TipoDeCobro.CONTADO, "450", true));
        assertThat(e.cobro.iniciar(4, CanalDePago.EN_LINEA, 1, "cliente").canal()).isEqualTo(CanalDePago.EN_LINEA);
        assertThat(iniciarEnTerminal(e).canal()).isEqualTo(CanalDePago.TERMINAL);
    }

    @Test
    void noCobraUnPedidoQueNoExisteOYaEstaPagado() {
        Escenario e = nuevo();
        assertThatThrownBy(() -> e.cobro.iniciar(99, CanalDePago.TERMINAL, 1, "admin"))
                .hasMessageContaining("No existe el pedido 99");
        e.pedidos.agregar(new PedidoACobrar(5, 50, TipoDeCobro.CONTADO, new BigDecimal("100"), true, false, false, null, List.of()));
        e.pedidos.agregar(new PedidoACobrar(6, 50, TipoDeCobro.CONTADO, new BigDecimal("100"), false, true, false, null, List.of()));
        assertThatThrownBy(() -> e.cobro.iniciar(6, CanalDePago.TERMINAL, 1, "admin")).hasMessageContaining("cancelado");
        assertThatThrownBy(() -> e.cobro.iniciar(5, CanalDePago.TERMINAL, 1, "admin")).hasMessageContaining("ya está pagado");
    }

    @Test
    void unSoloCobroActivoPorPedido_T1() {
        Escenario e = nuevo();
        iniciarEnTerminal(e);
        assertThatThrownBy(() -> iniciarEnTerminal(e)).hasMessageContaining("ya tiene un cobro");
        assertThat(e.pasarela.cobrosCreados).isEqualTo(1);
    }

    @Test
    void mesesSinInteresesSoloSiTodosLosArticulosLoAceptan_M1_M2() {
        Escenario e = nuevo();
        e.pedidos.agregar(Escenario.pedido(2, TipoDeCobro.CONTADO, "450", true, false));
        assertThat(e.cobro.iniciar(1, CanalDePago.TERMINAL, 6, "admin").cuotas()).isEqualTo(6);
        assertThatThrownBy(() -> e.cobro.iniciar(2, CanalDePago.TERMINAL, 6, "admin"))
                .hasMessageContaining("no acepta meses sin intereses");
    }

    @Test
    void mesesSinInteresesNoAplicanBajoElMinimoNiConPlanesQueNoExisten_M3_M4() {
        Escenario e = nuevo();
        e.pedidos.agregar(Escenario.pedido(2, TipoDeCobro.CONTADO, "250", true));
        assertThatThrownBy(() -> e.cobro.iniciar(2, CanalDePago.TERMINAL, 3, "admin")).hasMessageContaining("al menos $300.00");
        assertThatThrownBy(() -> e.cobro.iniciar(1, CanalDePago.TERMINAL, 12, "admin")).hasMessageContaining("12 meses");
    }

    @Test
    void elRechazoClaroDeLaPasarelaLiberaElPedido() {
        Escenario e = nuevo();
        e.pasarela.rechazaAlCrear = true;
        assertThatThrownBy(() -> iniciarEnTerminal(e)).isInstanceOf(PagoConTarjetaException.CobroNoCreado.class);
        assertThat(e.intentos.ultimo().estado()).isEqualTo(EstadoIntento.CANCELADO);
        e.pasarela.rechazaAlCrear = false;
        assertThat(iniciarEnTerminal(e).estado()).isEqualTo(EstadoIntento.ENVIADO);
    }

    @Test
    void tresRechazosEnDosHorasFrenanElPedido() {
        Escenario e = nuevo();
        for (int n = 0; n < 3; n++) {
            IntentoDePago i = iniciarEnTerminal(e);
            e.pasarela.respuesta = EstadoExterno.RECHAZADO;
            assertThat(e.cobro.confirmar(i.id()).estado()).isEqualTo(EstadoIntento.RECHAZADO);
        }
        assertThatThrownBy(() -> iniciarEnTerminal(e)).hasMessageContaining("Por seguridad");
        e.reloj.avanzarMinutos(121);
        e.pasarela.respuesta = EstadoExterno.PENDIENTE;
        assertThat(iniciarEnTerminal(e).estado()).isEqualTo(EstadoIntento.ENVIADO);
    }

    // ── Cobro dudoso ──────────────────────────────────────────────────────

    @Test
    void siSeCreaYSePierdeLaRespuesta_noSeCancelaNiSeLiberaYSeAvisaQueNoSeReintente() {
        Escenario e = nuevo();
        e.pasarela.creaYPierdeLaRespuesta = true;
        assertThatThrownBy(() -> iniciarEnTerminal(e)).hasMessageContaining("NO lo vuelvas a intentar");
        IntentoDePago dudoso = e.intentos.ultimo();
        assertThat(dudoso.estado()).isEqualTo(EstadoIntento.CREADO);
        e.pasarela.creaYPierdeLaRespuesta = false;
        assertThatThrownBy(() -> iniciarEnTerminal(e)).hasMessageContaining("ya tiene un cobro");
        assertThat(e.pasarela.cobrosCreados).isEqualTo(1);
    }

    @Test
    void elCobroPerdidoSeReencuentraPorReferenciaYSeAplica() {
        Escenario e = nuevo();
        e.pasarela.creaYPierdeLaRespuesta = true;
        assertThatThrownBy(() -> iniciarEnTerminal(e)).isInstanceOf(PagoConTarjetaException.class);
        Long id = e.intentos.ultimo().id();
        e.pasarela.respuesta = EstadoExterno.APROBADO;
        IntentoDePago resuelto = e.cobro.confirmar(id);
        assertThat(resuelto.estado()).isEqualTo(EstadoIntento.APROBADO);
        assertThat(resuelto.idExterno()).isEqualTo("ext-1");
        assertThat(e.pedidoActualizado.intentosPagados).containsExactly(id);
    }

    // ── Confirmar / expirar ───────────────────────────────────────────────

    @Test
    void confirmarEsIdempotente_T4() {
        Escenario e = nuevo();
        IntentoDePago i = aprobado(e);
        e.cobro.confirmar(i.id());
        e.cobro.confirmar(i.id());
        assertThat(e.pedidoActualizado.llamadasMarcarPagado).isEqualTo(1);
    }

    @Test
    void siMarcarPagadoFalla_elIntentoNoQuedaAprobadoYSeReparaDespues() {
        Escenario e = nuevo();
        IntentoDePago i = iniciarEnTerminal(e);
        e.pasarela.respuesta = EstadoExterno.APROBADO;
        e.pedidoActualizado.marcarPagadoFalla = true;
        assertThatThrownBy(() -> e.cobro.confirmar(i.id())).hasMessageContaining("no se pudo actualizar");
        assertThat(e.intentos.buscar(i.id()).orElseThrow().estado()).isEqualTo(EstadoIntento.ENVIADO);
        e.pedidoActualizado.marcarPagadoFalla = false;
        assertThat(e.cobro.confirmar(i.id()).estado()).isEqualTo(EstadoIntento.APROBADO);
        assertThat(e.pedidoActualizado.intentosPagados).containsExactly(i.id());
    }

    @Test
    void siSeVenceSinRespuestaSeCancelaEnLaPasarelaYSeLiberaElPedido_T7() {
        Escenario e = nuevo();
        IntentoDePago i = iniciarEnTerminal(e);
        e.reloj.avanzarMinutos(11);
        assertThat(e.cobro.confirmar(i.id()).estado()).isEqualTo(EstadoIntento.EXPIRADO);
        assertThat(e.pasarela.cancelaciones).isEqualTo(1);
        assertThat(iniciarEnTerminal(e).estado()).isEqualTo(EstadoIntento.ENVIADO);
    }

    @Test
    void elClientePagaJustoAlVencer_seAplicaEnLugarDeExpirar() {
        Escenario e = nuevo();
        IntentoDePago i = iniciarEnTerminal(e);
        e.reloj.avanzarMinutos(11);
        e.pasarela.respuesta = EstadoExterno.APROBADO;
        assertThat(e.cobro.confirmar(i.id()).estado()).isEqualTo(EstadoIntento.APROBADO);
    }

    @Test
    void siCancelarFallaYSigueVivo_noSeLiberaElPedido() {
        Escenario e = nuevo();
        IntentoDePago i = iniciarEnTerminal(e);
        e.reloj.avanzarMinutos(11);
        e.pasarela.cancelarFalla = true;
        assertThatThrownBy(() -> e.cobro.confirmar(i.id())).hasMessageContaining("No se pudo comunicar");
        assertThat(e.intentos.buscar(i.id()).orElseThrow().estado()).isEqualTo(EstadoIntento.ENVIADO);
    }

    @Test
    void aprobacionTardia_unCobroExpiradoQueLaPasarelaSiCobroSeAplica() {
        Escenario e = nuevo();
        IntentoDePago i = iniciarEnTerminal(e);
        e.reloj.avanzarMinutos(11);
        assertThat(e.cobro.confirmar(i.id()).estado()).isEqualTo(EstadoIntento.EXPIRADO);
        e.pasarela.respuesta = EstadoExterno.APROBADO;
        assertThat(e.cobro.confirmar(i.id()).estado()).isEqualTo(EstadoIntento.APROBADO);
    }

    @Test
    void elTrabajoProgramadoExpiraSoloLosVencidos() {
        Escenario e = nuevo();
        e.pedidos.agregar(Escenario.pedido(2, TipoDeCobro.CONTADO, "450", true));
        IntentoDePago viejo = iniciarEnTerminal(e);
        e.reloj.avanzarMinutos(11);
        IntentoDePago reciente = e.cobro.iniciar(2, CanalDePago.TERMINAL, 1, "admin");
        assertThat(e.cobro.expirarVencidos()).isEqualTo(1);
        assertThat(e.intentos.buscar(viejo.id()).orElseThrow().estado()).isEqualTo(EstadoIntento.EXPIRADO);
        assertThat(e.intentos.buscar(reciente.id()).orElseThrow().estado()).isEqualTo(EstadoIntento.ENVIADO);
    }

    // ── Cancelar ──────────────────────────────────────────────────────────

    @Test
    void cancelarLiberaElPedidoYNoCuentaParaElFreno() {
        Escenario e = nuevo();
        IntentoDePago i = iniciarEnTerminal(e);
        assertThat(e.cobro.cancelar(i.id()).estado()).isEqualTo(EstadoIntento.CANCELADO);
        assertThat(e.intentos.rechazadosDesde(1, Escenario.T0.minusSeconds(60))).isZero();
        assertThatThrownBy(() -> e.cobro.cancelar(i.id())).hasMessageContaining("no puede pasar");
    }

    @Test
    void siSeIntentaCancelarUnoYaCobrado_seAplicaYSeAvisa() {
        Escenario e = nuevo();
        IntentoDePago i = iniciarEnTerminal(e);
        e.pasarela.respuesta = EstadoExterno.APROBADO;
        assertThatThrownBy(() -> e.cobro.cancelar(i.id())).hasMessageContaining("ya se aprobó");
        assertThat(e.intentos.buscar(i.id()).orElseThrow().estado()).isEqualTo(EstadoIntento.APROBADO);
    }

    // ── Concurrencia ──────────────────────────────────────────────────────

    @Test
    void pollingYWebhookALaVez_unoGanaYElOtroDevuelveLoQueDejo() {
        Escenario e = nuevo();
        IntentoDePago i = iniciarEnTerminal(e);
        e.pasarela.respuesta = EstadoExterno.RECHAZADO;
        e.intentos.otroProcesoSeAdelanta = viejo -> e.intentos.porId.put(viejo.id(), IntentoDePago.reconstruir(
                viejo.id(), viejo.pedidoId(), viejo.clienteId(), viejo.canal(), viejo.monto(), viejo.cuotas(),
                EstadoIntento.CANCELADO, viejo.idExterno(), viejo.iniciadoPor(), viejo.creadoEn(), viejo.actualizadoEn(),
                viejo.devuelto(), viejo.version() + 1));
        assertThat(e.cobro.confirmar(i.id()).estado()).isEqualTo(EstadoIntento.CANCELADO);
    }

    // ── Devoluciones ──────────────────────────────────────────────────────

    @Test
    void devuelveParcialYLuegoElResto_T8() {
        Escenario e = nuevo();
        IntentoDePago i = aprobado(e);
        assertThat(e.devolucion.devolver(i.id(), new BigDecimal("100"), "admin").estado())
                .isEqualTo(EstadoIntento.DEVUELTO_PARCIAL);
        assertThat(e.devolucion.devolver(i.id(), null, "admin").estado()).isEqualTo(EstadoIntento.DEVUELTO);
        assertThat(e.pasarela.devoluciones).hasSize(2);
        assertThat(e.pedidoActualizado.devueltos).hasSize(2);
        assertThat(e.pasarela.devoluciones.get(1)).isEqualByComparingTo("350");
    }

    @Test
    void noDevuelveDeMasNiConMasDeDosDecimalesNiUnCobroQueNoSeAprobo() {
        Escenario e = nuevo();
        IntentoDePago i = aprobado(e);
        assertThatThrownBy(() -> e.devolucion.devolver(i.id(), new BigDecimal("500"), "admin")).hasMessageContaining("máximo");
        assertThatThrownBy(() -> e.devolucion.devolver(i.id(), new BigDecimal("0.001"), "admin")).hasMessageContaining("centavos");
        assertThat(e.pasarela.devoluciones).isEmpty();
        e.pedidos.agregar(Escenario.pedido(2, TipoDeCobro.CONTADO, "100", true));
        IntentoDePago pendiente = e.cobro.iniciar(2, CanalDePago.TERMINAL, 1, "admin");
        assertThatThrownBy(() -> e.devolucion.devolver(pendiente.id(), null, "admin")).hasMessageContaining("solo se devuelve un cobro aprobado");
        assertThatThrownBy(() -> e.devolucion.devolver(i.id(), new BigDecimal("10"), " ")).hasMessageContaining("quién");
    }

    // ── Planes ────────────────────────────────────────────────────────────

    @Test
    void ofreceSoloLosMesesQueAplican() {
        Escenario e = nuevo();
        assertThat(e.ofertas.mesesPara(1)).containsExactly(3, 6);
        e.pedidos.agregar(Escenario.pedido(3, TipoDeCobro.CONTADO, "450", true, false));
        assertThat(e.ofertas.mesesPara(3)).isEmpty();
        assertThatThrownBy(() -> e.ofertas.mesesPara(77)).hasMessageContaining("No existe el pedido");
    }
}
