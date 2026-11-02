package com.ventas.key.hexagonal.escanercodigo.infraestructura.seguridad;

import com.ventas.key.mis.productos.jwt.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El canal de la CUENTA con la cadena de seguridad REAL (filtro JWT, STATELESS, reglas de {@code SecurityConfig}) y el despacho asíncrono de
 * la pregunta abierta. Un {@code DeferredResult} pasa por los filtros una segunda vez (dispatch ASYNC) donde el filtro JWT no corre: sin una regla
 * que lo permita, el canal contestaba 401 aun con token válido y la PC nunca recibía el código (lo vio la revisión; las pruebas con
 * {@code standaloneSetup} no lo podían ver).
 */
@SpringBootTest(classes = com.ventas.key.mis.productos.MisProductosApplication.class)
@AutoConfigureMockMvc
class EscanerSeguridadRealTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtUtil jwtUtil;
    @MockBean private com.ventas.key.mis.productos.service.UsuarioDetailsService userDetailsService;

    private String tokenDe(String usuario) {
        UserDetails u = new User(usuario, "x", List.of());
        when(userDetailsService.loadUserByUsername(usuario)).thenReturn(u);
        return jwtUtil.generateToken(u, 1L);
    }

    @Test
    @DisplayName("sin sesión, el canal de la cuenta contesta 401")
    void sinSesion() throws Exception {
        mvc.perform(get("/v1/escaner/canal")).andExpect(status().isUnauthorized());
        mvc.perform(post("/v1/escaner/canal/latido")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("con sesión, la pregunta abierta de la PC recibe el código que manda el celular (también pasando por el despacho asíncrono)")
    void conSesionLaPreguntaAbiertaRecibeElCodigo() throws Exception {
        String auth = "Bearer " + tokenDe("ana");
        mvc.perform(get("/v1/escaner/canal").header("Authorization", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vigente").value(true));

        MvcResult pendiente = mvc.perform(get("/v1/escaner/canal/codigos?desde=0&esperar=true").header("Authorization", auth))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(post("/v1/escaner/canal/codigos").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"7501234567890\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));

        mvc.perform(asyncDispatch(pendiente)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codigos[0].codigo").value("7501234567890"));
    }

    @Test
    @DisplayName("sin esperar, la lectura del canal también pasa por el despacho asíncrono y contesta 200 con sesión")
    void lecturaSinEsperarConSesion() throws Exception {
        String auth = "Bearer " + tokenDe("beto");
        MvcResult r = mvc.perform(get("/v1/escaner/canal/codigos?desde=0").header("Authorization", auth))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(r)).andExpect(status().isOk());
    }
}
