package com.ventas.key.mis.productos.security;

import com.ventas.key.mis.productos.filter.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Renombre variante -> articulo (2026-10-01): cada ruta nueva tiene que estar protegida
 * <b>exactamente igual</b> que la vieja. Si a una le falta su regla en {@link SecurityConfig}, cae
 * en el {@code anyRequest().authenticated()} del final y cambia lo que ve alguien: una ruta publica
 * de la tienda empieza a pedir sesion, o una de admin queda abierta a cualquiera con sesion.
 *
 * <p>Solo se prueba la seguridad, sin controllers: si la seguridad deja pasar, la respuesta es 404
 * (no hay quien conteste); si no, 401 o 403. La ruta vieja y la nueva tienen que dar el mismo
 * numero para el mismo usuario.
 *
 * <p>El filtro de JWT se simula: en vez de leer un token, pone en la sesion los permisos que traiga
 * el encabezado {@code X-Prueba-Permisos}. Es lo mismo que hace el filtro real con el token.
 */
@WebMvcTest(controllers = RenombreArticuloSecurityTest.SinControladores.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "seguridad.cors.origenes-permitidos=http://localhost:4200")
class RenombreArticuloSecurityTest {

    /** Nadie contesta: lo unico que se mide es si la seguridad deja pasar. */
    @RestController
    static class SinControladores { }

    private static final String ENCABEZADO = "X-Prueba-Permisos";

    @Autowired private MockMvc mockMvc;
    @MockBean private JwtAuthenticationFilter jwtFilter;

    @BeforeEach
    void filtroQuePoneLosPermisosDelEncabezado() throws Exception {
        doAnswer(inv -> {
            HttpServletRequest req = inv.getArgument(0);
            String permisos = req.getHeader(ENCABEZADO);
            if (permisos != null) {
                List<SimpleGrantedAuthority> auth = Arrays.stream(permisos.split(","))
                        .filter(p -> !p.isBlank()).map(SimpleGrantedAuthority::new).toList();
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken("prueba", null, auth));
            }
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(req, inv.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    // ── Quienes llaman ──────────────────────────────────────────────────────────────────────

    /** Sin sesion (encabezado ausente), con sesion sin permisos, y los permisos que deciden. */
    private static final String[] USUARIOS = {
            null,
            "",
            "ROLE_ADMIN",
            "PANTALLA_productos/buscar",
            "PANTALLA_productos/buscar_ESCRIBIR",
            "PANTALLA_productos/buscar_ACCION_crear-variantes",
            "PANTALLA_productos/buscar_ACCION_descargar-excel",
            "PANTALLA_tienda/buscar_ESCRIBIR",
            "PANTALLA_tienda/buscar_ACCION_habilitar",
            "PANTALLA_tienda/buscar_ACCION_eliminar",
            "PANTALLA_admin/promociones",
            "PANTALLA_flores/catalogos_ESCRIBIR",
            "PANTALLA_rifas/agregar",
            "PANTALLA_rifas/agregar_ESCRIBIR",
    };

    // ── Ruta vieja -> ruta nueva ────────────────────────────────────────────────────────────

    /** {metodo, ruta vieja, ruta nueva}. Una por cada regla de SecurityConfig que se toco. */
    private static final String[][] RUTAS = {
            // /v1/variantes -> /v2/articulos (todo el recurso)
            {"GET", "/v1/variantes/buscar", "/v2/articulos/buscar"},
            {"GET", "/v1/variantes/buscar-filtrado", "/v2/articulos/buscar-filtrado"},
            {"GET", "/v1/variantes/filtros-disponibles", "/v2/articulos/filtros-disponibles"},
            {"GET", "/v1/variantes/porProducto/7", "/v2/articulos/porProducto/7"},
            {"GET", "/v1/variantes/imagenes/7", "/v2/articulos/imagenes/7"},
            {"GET", "/v1/variantes/variante/7/producto-id", "/v2/articulos/7/producto-id"},
            {"GET", "/v1/variantes/admin/filtrar", "/v2/articulos/admin/filtrar"},
            {"GET", "/v1/variantes/admin/diagnostico-imagenes/7", "/v2/articulos/admin/diagnostico-imagenes/7"},
            {"GET", "/v1/variantes/getAll", "/v2/articulos/getAll"},
            {"GET", "/v1/variantes/getOne/7", "/v2/articulos/getOne/7"},
            {"POST", "/v1/variantes/inicializarDesdeProducto", "/v2/articulos/inicializarDesdeProducto"},
            {"POST", "/v1/variantes/guardarConImagenes", "/v2/articulos/guardarConImagenes"},
            {"POST", "/v1/variantes/7/independizar", "/v2/articulos/7/independizar"},
            {"PUT", "/v1/variantes/7/habilitar", "/v2/articulos/7/habilitar"},
            {"PUT", "/v1/variantes/admin/habilitar-lote", "/v2/articulos/admin/habilitar-lote"},
            {"PUT", "/v1/variantes/imagenes/7/principal", "/v2/articulos/imagenes/7/principal"},
            {"DELETE", "/v1/variantes/deleteBy/7", "/v2/articulos/deleteBy/7"},
            {"DELETE", "/v1/variantes/7/imagenes", "/v2/articulos/7/imagenes"},
            {"DELETE", "/v1/variantes/imagenes", "/v2/articulos/imagenes"},
            // /v1/configurarRifaVariante -> /v2/configurarRifaArticulo (todo el recurso)
            {"GET", "/v1/configurarRifaVariante/porRifa/3", "/v2/configurarRifaArticulo/porRifa/3"},
            {"POST", "/v1/configurarRifaVariante/save", "/v2/configurarRifaArticulo/save"},
            {"PUT", "/v1/configurarRifaVariante/3", "/v2/configurarRifaArticulo/3"},
            {"DELETE", "/v1/configurarRifaVariante/3", "/v2/configurarRifaArticulo/3"},
            // Subrutas: el recurso no cambia de nombre, solo la palabra
            {"GET", "/v1/resenas/variante/7", "/v1/resenas/articulo/7"},
            {"GET", "/v1/resenas/variante/7/resumen", "/v1/resenas/articulo/7/resumen"},
            {"POST", "/v1/ganadorRifa/continuarVariante/3", "/v1/ganadorRifa/continuarArticulo/3"},
            {"GET", "/v1/productos/admin/sin-variantes/reporte", "/v1/productos/admin/sin-articulos/reporte"},
            {"POST", "/v1/productos/compartir-imagenes-variantes", "/v1/productos/compartir-imagenes-articulos"},
    };

    static Stream<String[]> casos() {
        List<String[]> casos = new ArrayList<>();
        for (String[] ruta : RUTAS) {
            for (String usuario : USUARIOS) {
                casos.add(new String[] {ruta[0], ruta[1], ruta[2], usuario});
            }
        }
        return casos.stream();
    }

    @ParameterizedTest(name = "{0} {1} = {2} para [{3}]")
    @MethodSource("casos")
    void la_ruta_nueva_esta_protegida_igual_que_la_vieja(String metodo, String vieja, String nueva,
                                                         String permisos) throws Exception {
        int antes = estado(metodo, vieja, permisos);
        int despues = estado(metodo, nueva, permisos);

        assertThat(despues)
                .as("%s %s daba %d y %s da %d (permisos: %s)", metodo, vieja, antes, nueva, despues, permisos)
                .isEqualTo(antes);
    }

    private int estado(String metodo, String ruta, String permisos) throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequestBuilder req = request(HttpMethod.valueOf(metodo), ruta);
        if (permisos != null) {
            req.header(ENCABEZADO, permisos);
        }
        return mockMvc.perform(req).andReturn().getResponse().getStatus();
    }
}
