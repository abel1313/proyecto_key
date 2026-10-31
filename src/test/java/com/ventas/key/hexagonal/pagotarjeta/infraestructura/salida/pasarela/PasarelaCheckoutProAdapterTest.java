package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.pasarela;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CanalDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoExterno;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PasarelaDePagoPort;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.pasarela.MercadoPagoCheckoutApi.Pago;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.pasarela.MercadoPagoCheckoutApi.Preferencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasarelaCheckoutProAdapterTest {

    private static final Instant AHORA = Instant.parse("2026-10-29T12:00:00Z");

    private MercadoPagoCheckoutApi api;
    private PasarelaCheckoutProAdapter pasarela;

    @BeforeEach
    void setUp() {
        api = mock(MercadoPagoCheckoutApi.class);
        pasarela = new PasarelaCheckoutProAdapter(api, Clock.fixed(AHORA, ZoneOffset.UTC),
                "https://qa.shop.test/", "https://qa.back.test/mis-productos/v1/pagos-tarjeta/webhook",
                "https://mp.test/redirect?pref_id=", false);
    }

    private static IntentoDePago intento(long id, int cuotas) {
        return IntentoDePago.nuevo(40, 7, CanalDePago.EN_LINEA, new BigDecimal("450.00"), cuotas, "ana", AHORA).conId(id);
    }

    private static Pago pago(String estado) {
        return new Pago(900L, estado, "15", new BigDecimal("450.00"));
    }

    @Test
    @DisplayName("crear: manda el id del intento como referencia, el monto, los meses, el vencimiento y las urls")
    void creaLaPreferenciaConLosDatosDelIntento() {
        when(api.crearPreferencia(any())).thenReturn("PREF-1");

        String idExterno = pasarela.crearCobro(intento(15, 3));

        assertThat(idExterno).isEqualTo("co:15:PREF-1");
        ArgumentCaptor<Preferencia> enviada = ArgumentCaptor.forClass(Preferencia.class);
        verify(api).crearPreferencia(enviada.capture());
        assertThat(enviada.getValue().referencia()).isEqualTo("15");
        assertThat(enviada.getValue().monto()).isEqualByComparingTo("450.00");
        assertThat(enviada.getValue().cuotasMaximas()).isEqualTo(3);
        assertThat(enviada.getValue().venceEn().toInstant()).isEqualTo(AHORA.plus(CanalDePago.EN_LINEA.vigencia()));
        assertThat(enviada.getValue().urlRegreso()).isEqualTo("https://qa.shop.test/pedidos/pago-resultado");
        assertThat(enviada.getValue().urlNotificacion()).endsWith("/v1/pagos-tarjeta/webhook");
    }

    @Test
    @DisplayName("crear: un rechazo claro de Mercado Pago (CobroNoCreado) se deja pasar tal cual")
    void unRechazoClaroSeDejaPasar() {
        when(api.crearPreferencia(any())).thenThrow(new PagoConTarjetaException.CobroNoCreado("no aceptó"));
        assertThatThrownBy(() -> pasarela.crearCobro(intento(15, 1)))
                .isInstanceOf(PagoConTarjetaException.CobroNoCreado.class);
    }

    @Test
    @DisplayName("crear: sin id de preferencia no se da por creado")
    void sinIdNoSeDaPorCreado() {
        when(api.crearPreferencia(any())).thenReturn(" ");
        assertThatThrownBy(() -> pasarela.crearCobro(intento(15, 1))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("estado: aprobado si algún pago está aprobado, aunque otro se haya rechazado")
    void aprobadoGanaSobreRechazado() {
        when(api.pagosPorReferencia("15")).thenReturn(List.of(pago("rejected"), pago("approved")));
        assertThat(pasarela.consultar("co:15:PREF-1")).isEqualTo(EstadoExterno.APROBADO);
    }

    @Test
    @DisplayName("estado: sin pagos sigue pendiente; en proceso sigue pendiente; solo rechazados = rechazado")
    void pendienteYRechazado() {
        when(api.pagosPorReferencia("15")).thenReturn(List.of());
        assertThat(pasarela.consultar("co:15:PREF-1")).isEqualTo(EstadoExterno.PENDIENTE);
        when(api.pagosPorReferencia("15")).thenReturn(List.of(pago("in_process")));
        assertThat(pasarela.consultar("co:15:PREF-1")).isEqualTo(EstadoExterno.PENDIENTE);
        when(api.pagosPorReferencia("15")).thenReturn(List.of(pago("rejected"), pago("cancelled")));
        assertThat(pasarela.consultar("co:15:PREF-1")).isEqualTo(EstadoExterno.RECHAZADO);
    }

    @Test
    @DisplayName("buscar por referencia: vacío si Mercado Pago no conoce nada; si conoce la preferencia, la devuelve")
    void buscarPorReferencia() {
        when(api.pagosPorReferencia("15")).thenReturn(List.of());
        when(api.preferenciaPorReferencia("15")).thenReturn(Optional.empty());
        assertThat(pasarela.buscarPorReferencia(15L)).isEmpty();

        when(api.preferenciaPorReferencia("15")).thenReturn(Optional.of("PREF-1"));
        var encontrado = pasarela.buscarPorReferencia(15L);
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().idExterno()).isEqualTo("co:15:PREF-1");
        assertThat(encontrado.get().estado()).isEqualTo(EstadoExterno.PENDIENTE);
    }

    @Test
    @DisplayName("cancelar: vence la página de pago; si ya está aprobado no se puede")
    void cancelar() {
        when(api.pagosPorReferencia("15")).thenReturn(List.of());
        pasarela.cancelar("co:15:PREF-1");
        verify(api).vencerPreferencia("PREF-1");

        when(api.pagosPorReferencia("15")).thenReturn(List.of(pago("approved")));
        assertThatThrownBy(() -> pasarela.cancelar("co:15:PREF-1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("devolver: reembolsa el pago aprobado con la clave de idempotencia; sin pago aprobado falla")
    void devolver() {
        when(api.pagosPorReferencia("15")).thenReturn(List.of(pago("rejected"), pago("approved")));
        pasarela.devolver("co:15:PREF-1", new BigDecimal("100.00"), "clave-1");
        verify(api).reembolsar(900L, new BigDecimal("100.00"), "clave-1");

        when(api.pagosPorReferencia("15")).thenReturn(List.of(pago("rejected")));
        assertThatThrownBy(() -> pasarela.devolver("co:15:PREF-1", BigDecimal.TEN, "clave-2"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("url de pago: la que da Mercado Pago; si no la da, la de configuración; un cobro simulado no tiene")
    void urlDePago() {
        when(api.urlDePago("PREF-1", false)).thenReturn(Optional.of("https://mp.test/init?pref_id=PREF-1"));
        assertThat(pasarela.urlDePago("co:15:PREF-1")).contains("https://mp.test/init?pref_id=PREF-1");

        when(api.urlDePago("PREF-2", false)).thenReturn(Optional.empty());
        assertThat(pasarela.urlDePago("co:16:PREF-2")).contains("https://mp.test/redirect?pref_id=PREF-2");

        assertThat(pasarela.urlDePago("sim-15")).isEmpty();
    }

    @Test
    @DisplayName("el enrutador manda EN_LINEA a Checkout Pro, TERMINAL a la otra pasarela, y opera según el id")
    void enrutador() {
        PasarelaDePagoPort terminal = mock(PasarelaDePagoPort.class);
        PasarelaPorCanalAdapter router = new PasarelaPorCanalAdapter(pasarela, terminal);
        when(api.crearPreferencia(any())).thenReturn("PREF-1");
        when(terminal.crearCobro(any())).thenReturn("sim-9");

        assertThat(router.crearCobro(intento(15, 1))).startsWith("co:15:");
        IntentoDePago enTerminal = IntentoDePago.nuevo(40, 7, CanalDePago.TERMINAL, new BigDecimal("450.00"), 1, "ana", AHORA).conId(9L);
        assertThat(router.crearCobro(enTerminal)).isEqualTo("sim-9");

        router.cancelar("sim-9");
        verify(terminal).cancelar("sim-9");
        verify(api, never()).vencerPreferencia("sim-9");
    }

    @Test
    @DisplayName("firma del webhook: acepta la firma correcta y rechaza cualquier otra cosa")
    void firmaDelWebhook() throws Exception {
        String secreto = "secreto-de-prueba";
        String ts = "1742505638683";
        String esperada = hmac(secreto, "id:123456;request-id:req-1;ts:" + ts + ";");
        FirmaWebhookMercadoPago firma = new FirmaWebhookMercadoPago(secreto);

        assertThat(firma.esValida("ts=" + ts + ",v1=" + esperada, "req-1", "123456")).isTrue();
        assertThat(firma.esValida("ts=" + ts + ",v1=" + esperada, "req-1", "999999")).isFalse();   // otro pago
        assertThat(firma.esValida("ts=" + ts + ",v1=" + esperada, "req-2", "123456")).isFalse();   // otra petición
        assertThat(firma.esValida("ts=" + ts + ",v1=00ff", "req-1", "123456")).isFalse();
        assertThat(firma.esValida(null, "req-1", "123456")).isFalse();
        assertThat(firma.esValida("basura", "req-1", "123456")).isFalse();
        assertThat(new FirmaWebhookMercadoPago("").esValida("ts=" + ts + ",v1=" + esperada, "req-1", "123456")).isFalse();
    }

    private static String hmac(String secreto, String texto) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        StringBuilder sb = new StringBuilder();
        for (byte b : mac.doFinal(texto.getBytes(StandardCharsets.UTF_8))) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
