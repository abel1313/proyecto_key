package com.ventas.key.mis.productos.security;

import com.ventas.key.mis.productos.jwt.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Matriz de autorización con la seguridad REAL (noche del 2026-11-05).
 *
 * <p>Qué comprueba, sin tocar datos: (1) sin sesión, nada interno responde algo que no sea 401/403; (2) una clienta común
 * (ROLE_USUARIO, sin pantallas ni acciones) recibe 403 en todo lo que es del administrador o de una pantalla del panel.
 * La seguridad se evalúa antes de llegar al controlador, así que no importa que el recurso no exista.
 *
 * <p>NO sustituye a la prueba de "acceso entre cuentas" (una clienta pidiendo el pedido de otra), que vive en el
 * controlador y necesita datos: está en la lista de pendientes de {@code AUDITORIA_SEGURIDAD_2026-11-05.md}.
 */
@SpringBootTest(classes = com.ventas.key.mis.productos.MisProductosApplication.class)
@AutoConfigureMockMvc
class AutorizacionPorEndpointTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtUtil jwtUtil;
    @MockBean private com.ventas.key.mis.productos.service.UsuarioDetailsService userDetailsService;

    private record Ruta(HttpMethod metodo, String path) {
        @Override public String toString() { return metodo + " " + path; }
    }

    /** Del administrador o de una pantalla del panel: sin sesión 401/403 y con ROLE_USUARIO 403. */
    static Stream<Ruta> soloPanel() {
        return Stream.of(
                new Ruta(HttpMethod.GET, "/v1/reportes/diario"),
                new Ruta(HttpMethod.GET, "/v1/reportes/revision-ventas"),
                new Ruta(HttpMethod.GET, "/v1/reportes/corte-caja"),
                new Ruta(HttpMethod.GET, "/v1/reportes/ventas/por-categoria?desde=2026-01-01&hasta=2026-01-31"),
                new Ruta(HttpMethod.POST, "/v1/reportes/ventas/por-categoria/generar"),
                new Ruta(HttpMethod.GET, "/v1/reportes/ventas/por-categoria/generados"),
                new Ruta(HttpMethod.GET, "/v1/grupos-precio-cantidad"),
                new Ruta(HttpMethod.POST, "/v1/grupos-precio-cantidad"),
                new Ruta(HttpMethod.PUT, "/v1/grupos-precio-cantidad/1"),
                new Ruta(HttpMethod.PUT, "/v1/grupos-precio-cantidad/1/activo"),
                new Ruta(HttpMethod.DELETE, "/v1/grupos-precio-cantidad/1"),
                new Ruta(HttpMethod.GET, "/v1/grupos-precio-cantidad/producto/1"),
                new Ruta(HttpMethod.GET, "/v1/fletes"),
                new Ruta(HttpMethod.POST, "/v1/fletes"),
                new Ruta(HttpMethod.GET, "/v1/fletes/1"),
                new Ruta(HttpMethod.GET, "/v1/fletes/por-producto/1"),
                new Ruta(HttpMethod.GET, "/v1/fletes/1/reporte"),
                new Ruta(HttpMethod.GET, "/v1/fletes/reportes"),
                new Ruta(HttpMethod.GET, "/v1/fletes/1/reporte/archivo"),
                new Ruta(HttpMethod.GET, "/v1/fletes/ligas/sin-viaje"),
                new Ruta(HttpMethod.POST, "/v1/fletes/1/ligar"),
                new Ruta(HttpMethod.GET, "/v1/fletes/1/ligas"),
                new Ruta(HttpMethod.GET, "/v1/fletes/guardados"),
                new Ruta(HttpMethod.DELETE, "/v1/fletes/ligas/1"),
                new Ruta(HttpMethod.PUT, "/v1/fletes/1"),
                new Ruta(HttpMethod.DELETE, "/v1/fletes/1"),
                new Ruta(HttpMethod.GET, "/v1/campanas"),
                new Ruta(HttpMethod.POST, "/v1/campanas"),
                new Ruta(HttpMethod.GET, "/v1/campanas/activa"),
                new Ruta(HttpMethod.GET, "/v1/campanas/1"),
                new Ruta(HttpMethod.PUT, "/v1/campanas/1"),
                new Ruta(HttpMethod.PUT, "/v1/campanas/1/activar"),
                new Ruta(HttpMethod.PUT, "/v1/campanas/1/apagar"),
                new Ruta(HttpMethod.POST, "/v1/campanas/1/duplicar"),
                new Ruta(HttpMethod.DELETE, "/v1/campanas/1"),
                new Ruta(HttpMethod.POST, "/v1/campanas/personas/verificar"),
                new Ruta(HttpMethod.DELETE, "/v1/campanas/personas/1"),
                new Ruta(HttpMethod.GET, "/v1/campanas/1/reporte"),
                new Ruta(HttpMethod.POST, "/v1/campanas/pedidos/1/enviar-codigo"),
                new Ruta(HttpMethod.POST, "/v1/calculos-precio"),
                new Ruta(HttpMethod.GET, "/v1/calculos-precio"),
                new Ruta(HttpMethod.GET, "/v1/calculos-precio/producto/1/ultimo"),
                new Ruta(HttpMethod.GET, "/v1/calculos-precio/producto/1"),
                new Ruta(HttpMethod.GET, "/v1/calculos-precio/comisiones"),
                new Ruta(HttpMethod.GET, "/v1/dashboard/resumen"),
                new Ruta(HttpMethod.GET, "/v1/usuarios/roles"),
                new Ruta(HttpMethod.GET, "/v1/usuarios/permisos"),
                new Ruta(HttpMethod.PUT, "/v1/usuarios/1/rol/2"),
                new Ruta(HttpMethod.GET, "/v1/admin/cache/algo"),
                new Ruta(HttpMethod.POST, "/v1/admin/datos-prueba/generar"),
                new Ruta(HttpMethod.GET, "/actuator/caches"),
                new Ruta(HttpMethod.DELETE, "/actuator/caches"),
                new Ruta(HttpMethod.GET, "/v1/documentos/algo"),
                new Ruta(HttpMethod.POST, "/v1/acepta-msi/producto/1"),
                new Ruta(HttpMethod.GET, "/v1/pedidos/buscar"),
                new Ruta(HttpMethod.GET, "/v1/pedidos/buscarClientePedido"),
                new Ruta(HttpMethod.POST, "/v1/pedidos/save"),
                new Ruta(HttpMethod.PUT, "/v1/pedidos/update/1"),
                new Ruta(HttpMethod.DELETE, "/v1/pedidos/delete"),
                new Ruta(HttpMethod.POST, "/v1/pedidos/1/notificar"),
                new Ruta(HttpMethod.GET, "/v1/clientes/buscar"),
                new Ruta(HttpMethod.DELETE, "/v1/clientes/1"),
                new Ruta(HttpMethod.GET, "/v1/imagenes/cache/algo"),
                new Ruta(HttpMethod.POST, "/v1/imagenes/algo"),
                new Ruta(HttpMethod.GET, "/v1/chat/admin/conversaciones"),
                new Ruta(HttpMethod.POST, "/v1/pagos-tarjeta/1/devolver"),
                new Ruta(HttpMethod.POST, "/v1/escaner/canal/fotos"),
                new Ruta(HttpMethod.GET, "/v1/admin/reconciliacion/algo"),
                new Ruta(HttpMethod.GET, "/v1/productos/admin/categorias"),
                // revisión 2026-11-05: rutas que estaban públicas de más
                new Ruta(HttpMethod.GET, "/v1/flores/cualquier-ruta-nueva"),
                new Ruta(HttpMethod.DELETE, "/v1/flores/pedidos-borrar"));
    }

    /** Piden sesión iniciada (el controlador revisa luego que sea de la dueña): sin sesión 401/403. */
    static Stream<Ruta> soloConSesion() {
        return Stream.of(
                new Ruta(HttpMethod.GET, "/v1/pedidos/1"),
                new Ruta(HttpMethod.POST, "/v1/pedidos/savePedido"),
                new Ruta(HttpMethod.PUT, "/v1/pedidos/1/entrega"),
                new Ruta(HttpMethod.GET, "/v1/chat/historial/usuario/1"),
                new Ruta(HttpMethod.GET, "/v1/chat/historial/cliente/1"),
                new Ruta(HttpMethod.GET, "/v1/pagos-tarjeta/pedido/1/meses"),
                new Ruta(HttpMethod.POST, "/v1/pagos-tarjeta"),
                new Ruta(HttpMethod.POST, "/v1/precio-cantidad/calcular"),
                new Ruta(HttpMethod.GET, "/v1/clientes/1"),
                new Ruta(HttpMethod.POST, "/v1/escaner/canal/algo"),
                new Ruta(HttpMethod.GET, "/v1/usuarios/buscarClientePorIdUsuario/1"),
                new Ruta(HttpMethod.POST, "/v1/dipomex/getCodigoPostal/50700"));   // el código postal solo es público con GET
    }

    private String token(String usuario, String... roles) {
        UserDetails u = new User(usuario, "x", java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList());
        when(userDetailsService.loadUserByUsername(usuario)).thenReturn(u);
        return "Bearer " + jwtUtil.generateToken(u, 1L);
    }

    private int codigo(Ruta r, String auth) throws Exception {
        var req = request(r.metodo(), r.path()).contentType("application/json").content("{}");
        if (auth != null) req.header("Authorization", auth);
        return mvc.perform(req).andReturn().getResponse().getStatus();
    }

    @ParameterizedTest(name = "sin sesión: {0}")
    @MethodSource("soloPanel")
    @DisplayName("panel: sin sesión responde 401 o 403")
    void panelSinSesion(Ruta r) throws Exception {
        assertThat(codigo(r, null)).as(r.toString()).isIn(401, 403);
    }

    @ParameterizedTest(name = "clienta: {0}")
    @MethodSource("soloPanel")
    @DisplayName("panel: una clienta común recibe 403")
    void panelConClientaComun(Ruta r) throws Exception {
        assertThat(codigo(r, token("clienta", "ROLE_USUARIO"))).as(r.toString()).isEqualTo(403);
    }

    @ParameterizedTest(name = "sin sesión: {0}")
    @MethodSource("soloConSesion")
    @DisplayName("con sesión: sin sesión responde 401 o 403")
    void soloConSesionSinSesion(Ruta r) throws Exception {
        assertThat(codigo(r, null)).as(r.toString()).isIn(401, 403);
    }

    /** Solo comprueba la REGLA de seguridad (sin sesión no recibe 401/403). El comportamiento del endpoint (404 HTML, productos sombra, ids raros) se prueba en CompartirArticuloControllerTest. */
    @Test
    @DisplayName("la vista previa para compartir no exige sesión (solo la regla de seguridad)")
    void vistaPreviaNoExigeSesion() throws Exception {
        assertThat(codigo(new Ruta(HttpMethod.GET, "/v1/compartir/articulo/999999"), null)).isNotIn(401, 403);
    }

    @Test
    @DisplayName("la tienda sigue funcionando: el cálculo de flores (POST) y el código postal (GET) son públicos")
    void lasRutasPublicasLegitimasSiguenPublicas() throws Exception {
        assertThat(codigo(new Ruta(HttpMethod.POST, "/v1/flores/calcular-precio"), null)).isNotIn(401, 403);
        assertThat(codigo(new Ruta(HttpMethod.POST, "/v1/flores/validar-cantidad"), null)).isNotIn(401, 403);
        assertThat(codigo(new Ruta(HttpMethod.POST, "/v1/flores/fechas-disponibles"), null)).isNotIn(401, 403);
        assertThat(codigo(new Ruta(HttpMethod.GET, "/v1/dipomex/getCodigoPostal/50700"), null)).isNotIn(401, 403);
    }
}
