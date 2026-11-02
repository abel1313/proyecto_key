package com.ventas.key.hexagonal.escanercodigo.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.escanercodigo.aplicacion.servicio.EscanerRemotoService;
import com.ventas.key.hexagonal.escanercodigo.infraestructura.salida.memoria.SesionesEscanerEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El flujo por HTTP con el servicio y el almacén en memoria de verdad (sin Spring completo). */
class EscanerRemotoControllerTest {

    private MockMvc mvc;

    /** El GET de códigos responde con DeferredResult: se hace la petición y luego el «despacho asíncrono» con su resultado. */
    private org.springframework.test.web.servlet.ResultActions leer(String ruta) throws Exception {
        var r = mvc.perform(get(ruta)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request().asyncStarted()).andReturn();
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(r));
    }

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.systemUTC();
        AtomicInteger n = new AtomicInteger();
        var servicio = new EscanerRemotoService(new SesionesEscanerEnMemoria(reloj), reloj, () -> "sesion" + n.incrementAndGet());
        mvc = MockMvcBuilders.standaloneSetup(new EscanerRemotoController(servicio)).build();
    }

    @Test
    @DisplayName("abrir → mandar → leer → cerrar")
    void flujo() throws Exception {
        mvc.perform(post("/v1/escaner/sesiones")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value("sesion1"));
        mvc.perform(post("/v1/escaner/sesiones/sesion1/codigos").contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"7501234567890\"}"))
                .andExpect(status().isOk());
        leer("/v1/escaner/sesiones/sesion1/codigos?desde=0").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codigos[0].codigo").value("7501234567890"))
                .andExpect(jsonPath("$.data.ultimaSecuencia").value(1));
        leer("/v1/escaner/sesiones/sesion1/codigos?desde=1").andExpect(jsonPath("$.data.codigos").isEmpty());
        mvc.perform(delete("/v1/escaner/sesiones/sesion1")).andExpect(status().isOk());
        leer("/v1/escaner/sesiones/sesion1/codigos").andExpect(status().isGone());
    }

    @Test
    @DisplayName("pregunta abierta (esperar=true): se contesta en cuanto el celular manda el código; el celular avisa que está conectado")
    void preguntaAbierta() throws Exception {
        mvc.perform(post("/v1/escaner/sesiones")).andExpect(status().isOk());
        mvc.perform(get("/v1/escaner/sesiones/sesion1")).andExpect(jsonPath("$.data.vigente").value(true))
                .andExpect(jsonPath("$.data.celularConectado").value(false));
        mvc.perform(post("/v1/escaner/sesiones/sesion1/latido")).andExpect(status().isOk());
        mvc.perform(get("/v1/escaner/sesiones/sesion1")).andExpect(jsonPath("$.data.celularConectado").value(true));

        var pendiente = mvc.perform(get("/v1/escaner/sesiones/sesion1/codigos?desde=0&esperar=true"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request().asyncStarted()).andReturn();
        mvc.perform(post("/v1/escaner/sesiones/sesion1/codigos").contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"7501234567890\"}"))
                .andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(pendiente)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codigos[0].codigo").value("7501234567890"))
                .andExpect(jsonPath("$.data.celularConectado").value(true));
    }

    @Test
    @DisplayName("la pregunta abierta que se queda sin código contesta vacía (200) al cumplirse el tiempo, y se puede volver a preguntar")
    void preguntaAbiertaVenceSinCodigo() throws Exception {
        mvc.perform(post("/v1/escaner/sesiones")).andExpect(status().isOk());
        var pendiente = mvc.perform(get("/v1/escaner/sesiones/sesion1/codigos?desde=0&esperar=true"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request().asyncStarted()).andReturn();
        // El servidor avisa que se acabó el tiempo de la pregunta: se simula como lo haría el contenedor.
        var contexto = (org.springframework.mock.web.MockAsyncContext) pendiente.getRequest().getAsyncContext();
        for (var oyente : contexto.getListeners()) {
            oyente.onTimeout(new jakarta.servlet.AsyncEvent(contexto));
        }
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(pendiente)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codigos").isEmpty());
        // y no quedó ninguna pregunta huérfana: el código que llega después no se «entrega» a nadie (data = false)
        mvc.perform(post("/v1/escaner/sesiones/sesion1/codigos").contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"7501234567890\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("pregunta abierta sobre una conexión que no existe: 410 de inmediato; si se cierra mientras espera, también 410")
    void preguntaAbiertaSinConexion() throws Exception {
        leer("/v1/escaner/sesiones/nada/codigos?desde=0&esperar=true").andExpect(status().isGone());

        mvc.perform(post("/v1/escaner/sesiones")).andExpect(status().isOk());
        var pendiente = mvc.perform(get("/v1/escaner/sesiones/sesion1/codigos?desde=0&esperar=true"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request().asyncStarted()).andReturn();
        mvc.perform(delete("/v1/escaner/sesiones/sesion1")).andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(pendiente)).andExpect(status().isGone());
    }

    @Test
    @DisplayName("la IP de la PC: la de más a la derecha de X-Forwarded-For, y las IPv6 agrupadas por /48")
    void ip() {
        var req = new org.springframework.mock.web.MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "6.6.6.6, 10.1.2.3");
        org.assertj.core.api.Assertions.assertThat(EscanerRemotoController.ipDe(req)).isEqualTo("10.1.2.3");
        org.assertj.core.api.Assertions.assertThat(EscanerRemotoController.agruparIpv6("2001:db8:1:2:aaaa:bbbb:cccc:dddd")).isEqualTo("2001:db8:1::/48");
        org.assertj.core.api.Assertions.assertThat(EscanerRemotoController.agruparIpv6("2001:db8:1:9:1:2:3:4")).isEqualTo("2001:db8:1::/48");   // otro /64 del mismo /48
        org.assertj.core.api.Assertions.assertThat(EscanerRemotoController.agruparIpv6("203.0.113.9")).isEqualTo("203.0.113.9");
        org.assertj.core.api.Assertions.assertThat(EscanerRemotoController.agruparIpv6("no-es-ip")).isEqualTo("no-es-ip");
    }

    @Test
    @DisplayName("errores: código inválido 400, conexión que no existe 410")
    void errores() throws Exception {
        mvc.perform(post("/v1/escaner/sesiones")).andExpect(status().isOk());
        mvc.perform(post("/v1/escaner/sesiones/sesion1/codigos").contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"ab\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje", containsString("no es válido")));
        mvc.perform(post("/v1/escaner/sesiones/otra/codigos").contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"7501234567890\"}"))
                .andExpect(status().isGone());
    }
}
