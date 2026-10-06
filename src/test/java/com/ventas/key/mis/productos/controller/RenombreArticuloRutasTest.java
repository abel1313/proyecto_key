package com.ventas.key.mis.productos.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Renombre variante -> articulo (2026-10-01): cada ruta nueva tiene que llegar <b>al mismo metodo</b>
 * que la vieja, y la vieja tiene que seguir existiendo (el front de produccion la usa).
 *
 * <p>Se arma el mapa de rutas real de Spring con los controllers tocados (sus dependencias no
 * importan: solo se lee a que metodo llega cada ruta, no se ejecuta nada).
 */
class RenombreArticuloRutasTest {

    private static RequestMappingHandlerMapping rutas;

    @BeforeAll
    static void mapaDeRutas() {
        GenericWebApplicationContext ctx = new GenericWebApplicationContext(new MockServletContext());
        ctx.registerBean(VarianteController.class, () -> mock(VarianteController.class));
        ctx.registerBean(ConfigurarRifaVarianteController.class, () -> mock(ConfigurarRifaVarianteController.class));
        ctx.registerBean(ResenaController.class, () -> mock(ResenaController.class));
        ctx.registerBean(GanadorRifaControllerImpl.class, () -> mock(GanadorRifaControllerImpl.class));
        ctx.registerBean(ProductosControllerImpl.class, () -> mock(ProductosControllerImpl.class));
        ctx.refresh();

        rutas = new RequestMappingHandlerMapping();
        rutas.setApplicationContext(ctx);
        rutas.afterPropertiesSet();
    }

    private static Method metodoQueContesta(String metodoHttp, String ruta) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(metodoHttp, ruta);
        HandlerExecutionChain cadena = rutas.getHandler(req);
        assertThat(cadena).as("%s %s no llega a ningun metodo", metodoHttp, ruta).isNotNull();
        return ((HandlerMethod) cadena.getHandler()).getMethod();
    }

    private static void mismoMetodo(String metodoHttp, String vieja, String nueva) throws Exception {
        Method antes = metodoQueContesta(metodoHttp, vieja);
        Method despues = metodoQueContesta(metodoHttp, nueva);
        assertThat(despues).as("%s %s y %s llegan a metodos distintos", metodoHttp, vieja, nueva)
                .isEqualTo(antes);
    }

    @Test
    void todo_lo_de_v1_variantes_existe_igual_en_v2_articulos() {
        // Barrido completo: si mañana alguien agrega un endpoint a VarianteController, esta prueba
        // exige que tambien salga en /v2/articulos (lo da el @RequestMapping de la clase).
        List<String> faltan = new ArrayList<>();
        Map<RequestMappingInfo, HandlerMethod> todas = rutas.getHandlerMethods();
        todas.forEach((info, metodo) -> info.getPatternValues().stream()
                .filter(p -> p.startsWith("/v1/variantes/") || p.equals("/v1/variantes"))
                .forEach(p -> {
                    String nueva = "/v2/articulos" + p.substring("/v1/variantes".length());
                    if (!info.getPatternValues().contains(nueva)) {
                        faltan.add(info.getMethodsCondition() + " " + p);
                    }
                }));
        assertThat(faltan).as("rutas de /v1/variantes sin su par en /v2/articulos").isEmpty();
        assertThat(todas.keySet().stream().flatMap(i -> i.getPatternValues().stream())
                .filter(p -> p.startsWith("/v1/variantes"))).hasSizeGreaterThan(20);
    }

    @Test
    void las_rutas_que_usa_el_front_llegan_al_mismo_metodo() throws Exception {
        mismoMetodo("GET", "/v1/variantes/buscar", "/v2/articulos/buscar");
        mismoMetodo("GET", "/v1/variantes/buscar-filtrado", "/v2/articulos/buscar-filtrado");
        mismoMetodo("GET", "/v1/variantes/filtros-disponibles", "/v2/articulos/filtros-disponibles");
        mismoMetodo("GET", "/v1/variantes/porProducto/7", "/v2/articulos/porProducto/7");
        mismoMetodo("GET", "/v1/variantes/porProducto/7/paginado/resumen", "/v2/articulos/porProducto/7/paginado/resumen");
        mismoMetodo("GET", "/v1/variantes/imagenes/7", "/v2/articulos/imagenes/7");
        mismoMetodo("GET", "/v1/variantes/imagenes/7/paginado", "/v2/articulos/imagenes/7/paginado");
        mismoMetodo("GET", "/v1/variantes/admin/filtrar", "/v2/articulos/admin/filtrar");
        mismoMetodo("GET", "/v1/variantes/admin/diagnostico-imagenes/7", "/v2/articulos/admin/diagnostico-imagenes/7");
        mismoMetodo("GET", "/v1/variantes/getOne/7", "/v2/articulos/getOne/7");
        mismoMetodo("POST", "/v1/variantes/guardarConImagenes", "/v2/articulos/guardarConImagenes");
        mismoMetodo("POST", "/v1/variantes/inicializarDesdeProducto", "/v2/articulos/inicializarDesdeProducto");
        mismoMetodo("POST", "/v1/variantes/7/independizar", "/v2/articulos/7/independizar");
        mismoMetodo("PUT", "/v1/variantes/7/habilitar", "/v2/articulos/7/habilitar");
        mismoMetodo("PUT", "/v1/variantes/admin/habilitar-lote", "/v2/articulos/admin/habilitar-lote");
        mismoMetodo("PUT", "/v1/variantes/imagenes/7/principal", "/v2/articulos/imagenes/7/principal");
        mismoMetodo("DELETE", "/v1/variantes/deleteBy/7", "/v2/articulos/deleteBy/7");
        mismoMetodo("DELETE", "/v1/variantes/7/imagenes", "/v2/articulos/7/imagenes");
        mismoMetodo("DELETE", "/v1/variantes/imagenes", "/v2/articulos/imagenes");
        // La unica que cambia de forma en v2: sin repetir la palabra.
        mismoMetodo("GET", "/v1/variantes/variante/7/producto-id", "/v2/articulos/7/producto-id");
    }

    @Test
    void rifas_resenas_y_productos_llegan_al_mismo_metodo() throws Exception {
        mismoMetodo("POST", "/v1/configurarRifaVariante/save", "/v2/configurarRifaArticulo/save");
        mismoMetodo("GET", "/v1/configurarRifaVariante/porRifa/3", "/v2/configurarRifaArticulo/porRifa/3");
        mismoMetodo("GET", "/v1/configurarRifaVariante/palabrasClave/3", "/v2/configurarRifaArticulo/palabrasClave/3");
        mismoMetodo("DELETE", "/v1/configurarRifaVariante/3", "/v2/configurarRifaArticulo/3");
        mismoMetodo("PUT", "/v1/configurarRifaVariante/3", "/v2/configurarRifaArticulo/3");
        mismoMetodo("PUT", "/v1/configurarRifaVariante/3/palabraClave", "/v2/configurarRifaArticulo/3/palabraClave");
        mismoMetodo("POST", "/v1/ganadorRifa/continuarVariante/3", "/v1/ganadorRifa/continuarArticulo/3");
        mismoMetodo("GET", "/v1/resenas/variante/7", "/v1/resenas/articulo/7");
        mismoMetodo("GET", "/v1/resenas/variante/7/resumen", "/v1/resenas/articulo/7/resumen");
        mismoMetodo("GET", "/v1/productos/admin/sin-variantes/reporte", "/v1/productos/admin/sin-articulos/reporte");
        mismoMetodo("POST", "/v1/productos/compartir-imagenes-variantes", "/v1/productos/compartir-imagenes-articulos");
    }
}
