package com.ventas.key.hexagonal.pagotarjeta.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CanalDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.ConfirmarCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.pasarela.MercadoPagoCheckoutApi;
import com.ventas.key.mis.productos.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PagoTarjetaWebhookControllerTest {

    private static final String SECRETO = "secreto-de-prueba";
    private final ConfirmarCobroCasoUso confirmar = mock(ConfirmarCobroCasoUso.class);
    private final MercadoPagoCheckoutApi api = mock(MercadoPagoCheckoutApi.class);
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(
                new PagoTarjetaWebhookController(confirmar, api, mock(CacheService.class), SECRETO)).build();
        IntentoDePago intento = IntentoDePago.nuevo(40, 7, CanalDePago.EN_LINEA, new BigDecimal("450.00"), 1, "ana",
                Instant.parse("2026-10-29T12:00:00Z")).conId(15L);
        when(confirmar.confirmar(15L)).thenReturn(intento);
    }

    private static String firma(String dataId, String requestId, String ts) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        StringBuilder sb = new StringBuilder();
        for (byte b : mac.doFinal(("id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";").getBytes(StandardCharsets.UTF_8)))
            sb.append(String.format("%02x", b));
        return "ts=" + ts + ",v1=" + sb;
    }

    @Test
    @DisplayName("aviso con firma válida de un pago nuestro: se confirma el cobro")
    void firmaValidaConfirma() throws Exception {
        when(api.pago(555L)).thenReturn(Optional.of(new MercadoPagoCheckoutApi.Pago(555L, "approved", "15", new BigDecimal("450.00"))));
        mvc.perform(post("/v1/pagos-tarjeta/webhook?data.id=555&type=payment")
                        .header("x-signature", firma("555", "req-1", "1700000000000")).header("x-request-id", "req-1")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(confirmar).confirmar(15L);
    }

    @Test
    @DisplayName("aviso sin firma o con firma falsa: 401 y no se toca nada")
    void sinFirmaNoHaceNada() throws Exception {
        mvc.perform(post("/v1/pagos-tarjeta/webhook?data.id=555&type=payment")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/v1/pagos-tarjeta/webhook?data.id=555&type=payment")
                        .header("x-signature", "ts=1,v1=abc").header("x-request-id", "req-1")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verify(confirmar, never()).confirmar(anyLong());
        verify(api, never()).pago(anyLong());
    }

    @Test
    @DisplayName("pago que no es de un cobro nuestro (referencia ajena): 200 y no se confirma nada")
    void referenciaAjenaSeIgnora() throws Exception {
        when(api.pago(777L)).thenReturn(Optional.of(new MercadoPagoCheckoutApi.Pago(777L, "approved", "otra-cosa", BigDecimal.TEN)));
        mvc.perform(post("/v1/pagos-tarjeta/webhook?data.id=777&type=payment")
                        .header("x-signature", firma("777", "req-9", "1700000000001")).header("x-request-id", "req-9")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(confirmar, never()).confirmar(anyLong());
    }

    @Test
    @DisplayName("otro tipo de aviso con firma válida: 200 y no se consulta nada")
    void otroTipoSeIgnora() throws Exception {
        mvc.perform(post("/v1/pagos-tarjeta/webhook?data.id=555&type=merchant_order")
                        .header("x-signature", firma("555", "req-1", "1700000000000")).header("x-request-id", "req-1")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(api, never()).pago(anyLong());
    }
}
