package com.ventas.key.mis.productos.security;

import com.ventas.key.mis.productos.jwt.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El corte de caja lleva costo y ganancia: es SOLO del administrador (regla del dueño, 2026-11-04). Con la seguridad real: sin sesión y un
 * cliente común no entran; el administrador sí.
 */
@SpringBootTest(classes = com.ventas.key.mis.productos.MisProductosApplication.class)
@AutoConfigureMockMvc
class CorteDeCajaSoloAdminTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtUtil jwtUtil;
    @MockBean private com.ventas.key.mis.productos.service.UsuarioDetailsService userDetailsService;

    private String token(String usuario, String... roles) {
        UserDetails u = new User(usuario, "x", java.util.Arrays.stream(roles)
                .map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority(r)).toList());
        when(userDetailsService.loadUserByUsername(usuario)).thenReturn(u);
        return "Bearer " + jwtUtil.generateToken(u, 1L);
    }

    @Test
    @DisplayName("sin sesion: no entra")
    void sinSesion() throws Exception {
        mvc.perform(get("/v1/reportes/corte-caja")).andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("un cliente comun recibe 403")
    void clienteComun() throws Exception {
        mvc.perform(get("/v1/reportes/corte-caja").header("Authorization", token("ana", "ROLE_USUARIO")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/v1/reportes/corte-caja?zonaId=1").header("Authorization", token("beto", "ROLE_USUARIO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("el administrador no recibe 401 ni 403")
    void admin() throws Exception {
        int codigo = mvc.perform(get("/v1/reportes/corte-caja").header("Authorization", token("admin", "ROLE_ADMIN")))
                .andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(codigo).isNotIn(401, 403);
    }
}
