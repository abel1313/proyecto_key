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
        mvc.perform(get("/v1/escaner/sesiones/sesion1/codigos?desde=0")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codigos[0].codigo").value("7501234567890"))
                .andExpect(jsonPath("$.data.ultimaSecuencia").value(1));
        mvc.perform(get("/v1/escaner/sesiones/sesion1/codigos?desde=1")).andExpect(jsonPath("$.data.codigos").isEmpty());
        mvc.perform(delete("/v1/escaner/sesiones/sesion1")).andExpect(status().isOk());
        mvc.perform(get("/v1/escaner/sesiones/sesion1/codigos")).andExpect(status().isGone());
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
