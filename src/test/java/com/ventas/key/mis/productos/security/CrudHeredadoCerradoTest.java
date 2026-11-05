package com.ventas.key.mis.productos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.mis.productos.entity.Cliente;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auditoria de seguridad 2026-11-03: los endpoints que Pedidos y Clientes heredan de AbstractController (getAll, getOne, save, update, delete)
 * estaban abiertos a cualquier usuario con sesion. Con la seguridad REAL (no standaloneSetup) se comprueba que un cliente comun ya no entra,
 * y que el codigo de verificacion del correo no sale en el JSON.
 */
@SpringBootTest(classes = com.ventas.key.mis.productos.MisProductosApplication.class)
@AutoConfigureMockMvc
class CrudHeredadoCerradoTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private com.ventas.key.mis.productos.service.UsuarioDetailsService userDetailsService;

    private String clienteComun(String usuario) {
        UserDetails u = new User(usuario, "x", List.of());
        when(userDetailsService.loadUserByUsername(usuario)).thenReturn(u);
        return "Bearer " + jwtUtil.generateToken(u, 1L);
    }

    @Test
    @DisplayName("un cliente comun NO puede listar todos los pedidos ni todos los clientes")
    void noListaTodo() throws Exception {
        String auth = clienteComun("ana");
        mvc.perform(get("/v1/pedidos/getAll?page=0&size=100000").header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(get("/v1/pedidos/getOne/1").header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(get("/v1/clientes/getAll?page=0&size=100000").header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(get("/v1/clientes/getOne/1").header("Authorization", auth)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("nadie guarda un Pedido crudo por save/update/delete heredados (savePedido es la unica puerta, y valida precios)")
    void noGuardaPedidoCrudo() throws Exception {
        String auth = clienteComun("beto");
        String pedido = "{\"id\":1,\"estadoPedido\":\"Pagado\",\"totalPedido\":1,\"totalPagado\":1}";
        mvc.perform(post("/v1/pedidos/save").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isForbidden());
        mvc.perform(put("/v1/pedidos/update/1").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/v1/pedidos/delete").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content("1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un cliente comun no puede actualizar un Cliente crudo por update heredado")
    void noActualizaClienteCrudo() throws Exception {
        mvc.perform(put("/v1/clientes/update/1").header("Authorization", clienteComun("carla")).contentType(MediaType.APPLICATION_JSON).content("{\"id\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("sin sesion tampoco")
    void sinSesion() throws Exception {
        mvc.perform(get("/v1/pedidos/getAll?page=0&size=10")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v1/clientes/getAll?page=0&size=10")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("el codigo de verificacion del correo no sale en el JSON del Cliente")
    void codigoNoSale() throws Exception {
        Cliente c = new Cliente();
        c.setCodigoVerificacion("123456");
        c.setCodigoVerificacionExpira(LocalDateTime.now().plusMinutes(10));
        c.setCorreoPendiente("nuevo@correo.com");
        String json = objectMapper.writeValueAsString(c);
        assertThat(json).doesNotContain("123456").doesNotContain("codigoVerificacion");
        assertThat(json).contains("nuevo@correo.com");   // el front sigue viendo el correo pendiente
    }
}
