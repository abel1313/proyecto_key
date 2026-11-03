package com.ventas.key.mis.productos.chatbot;

import com.ventas.key.mis.productos.controller.ChatbotController;
import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.service.api.IChatMensajeService;
import com.ventas.key.mis.productos.service.api.IChatSesionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

/**
 * Hotfix 2026-11-03: (1) solo quien el negocio autoriza (por defecto el ADMIN) recibe nombre exacto + codigo de barras; al cliente no se le
 * manda ni en el catalogo del modelo ni en las tarjetas. (2) El bot no dice "aqui tienes la imagen" si el producto no tiene foto.
 */
class ChatbotInfoCompletaTest {

    private IVarianteRepository varianteRepository;
    private IVarianteImagenRepository imagenes;
    private ChatbotSitioWebService servicio;

    @BeforeEach
    void preparar() {
        varianteRepository = mock(IVarianteRepository.class);
        IPalabraClaveRepository palabras = mock(IPalabraClaveRepository.class);
        when(palabras.findAll()).thenReturn(List.of());
        imagenes = mock(IVarianteImagenRepository.class);
        servicio = new ChatbotSitioWebService(varianteRepository, palabras);
        ReflectionTestUtils.setField(servicio, "varianteImagenRepository", imagenes);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private Variantes variante(int id, String nombre, String codigo) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setPrecioVenta(250.0);
        CodigoBarra cb = new CodigoBarra();
        cb.setCodigoBarras(codigo);
        p.setCodigoBarras(cb);
        Variantes v = new Variantes();
        v.setId(id);
        v.setProducto(p);
        v.setStock(2);
        return v;
    }

    @Test
    @DisplayName("el catalogo del cliente NO lleva codigos de barras; el del admin sí")
    void catalogoSegunQuienPregunta() {
        when(varianteRepository.findByStockGreaterThanAndProductoHabilitado(anyInt(), org.mockito.ArgumentMatchers.anyChar(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(variante(1, "Short rojo", "7501234567890"))));
        assertThat(servicio.obtenerContextoVariantes(null)).contains("Short rojo").doesNotContain("7501234567890");
        assertThat(servicio.obtenerContextoVariantes(null, true)).contains("Short rojo").contains("7501234567890");
    }

    @Test
    @DisplayName("las tarjetas llevan el codigo solo si es info completa, y siempre dicen si hay foto")
    void tarjetas() {
        when(varianteRepository.buscarParaChatbot(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(variante(1, "Short rojo", "7501234567890"), variante(2, "Short rojo B", "7509999999999"))));
        when(imagenes.existsByVarianteId(1)).thenReturn(true);
        when(imagenes.existsByVarianteId(2)).thenReturn(false);

        List<Map<String, Object>> cliente = productos(servicio.buscarProductos("short", 0, false));
        assertThat(cliente).extracting(m -> m.get("codigoBarras")).containsOnlyNulls();
        assertThat(cliente).extracting(m -> m.get("tieneImagen")).containsExactly(true, false);

        List<Map<String, Object>> admin = productos(servicio.buscarProductos("short", 0, true));
        assertThat(admin).extracting(m -> m.get("codigoBarras")).containsExactly("7501234567890", "7509999999999");
    }

    @Test
    @DisplayName("un articulo recien creado entra al catalogo del bot por lo que escribio el cliente, aunque no este en los primeros 1000")
    void coincidenciasConLoPedido() {
        when(varianteRepository.buscarCoincidenciasParaChatbot(eq("producto1"), any(Pageable.class)))
                .thenReturn(List.of(variante(7, "producto1", "1027202661697")));
        String cliente = servicio.coincidenciasConLoPedido(List.of("tienes este producto producto1"), false);
        assertThat(cliente).contains("producto1").contains("disponible").doesNotContain("1027202661697");
        String admin = servicio.coincidenciasConLoPedido(List.of("tienes este producto producto1"), true);
        assertThat(admin).contains("1027202661697");
        verify(varianteRepository, org.mockito.Mockito.never()).buscarCoincidenciasParaChatbot(eq("tienes"), any(Pageable.class));
        verify(varianteRepository, org.mockito.Mockito.never()).buscarCoincidenciasParaChatbot(eq("producto"), any(Pageable.class));
    }

    @Test
    @DisplayName("sin coincidencias no agrega nada y una falla de la consulta no tumba al bot")
    void sinCoincidencias() {
        assertThat(servicio.coincidenciasConLoPedido(List.of("hola"), false)).isEmpty();
        when(varianteRepository.buscarCoincidenciasParaChatbot(anyString(), any(Pageable.class))).thenThrow(new RuntimeException("bd"));
        assertThat(servicio.coincidenciasConLoPedido(List.of("short rojo"), false)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> productos(Map<String, Object> busqueda) {
        return (List<Map<String, Object>>) busqueda.get("productos");
    }

    // ───────── controlador ─────────

    private ChatbotSitioWebService chatbot;
    private MockMvc mvc;

    private void controlador() {
        chatbot = mock(ChatbotSitioWebService.class);
        ChatbotController c = new ChatbotController(chatbot, mock(ChatbotBlockService.class), mock(IChatSesionService.class), mock(IChatMensajeService.class));
        ReflectionTestUtils.setField(c, "rolesConInfoCompleta", "ROLE_ADMIN");
        mvc = MockMvcBuilders.standaloneSetup(c).build();
    }

    private void entrarComo(String rol) {
        SecurityContextHolder.getContext().setAuthentication(new UsuarioToken(rol));
    }

    private static class UsuarioToken extends UsernamePasswordAuthenticationToken {
        UsuarioToken(String rol) {
            super("u", "t", List.of(new SimpleGrantedAuthority(rol)));
        }
    }

    private void preguntar(String mensaje) throws Exception {
        var r = mvc.perform(post("/v1/chatbot/mensaje").contentType(MediaType.APPLICATION_JSON)
                .content("{\"mensaje\":\"" + mensaje + "\"}")).andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(r));
        ultimaRespuesta = mvc.perform(asyncDispatch(r));
    }

    private org.springframework.test.web.servlet.ResultActions ultimaRespuesta;

    @Test
    @DisplayName("pide foto de un producto SIN foto: el bot dice la verdad y no manda tarjetas vacías")
    void productoSinFoto() throws Exception {
        controlador();
        when(chatbot.chat(any(), anyBoolean())).thenReturn(Mono.just("¡Aquí la tienes! 📸 ##BUSCAR[short,0]##"));
        when(chatbot.buscarProductos(eq("short"), eq(0), anyBoolean())).thenReturn(Map.of(
                "productos", List.of(Map.of("varianteId", 1, "nombre", "Short rojo", "tieneImagen", false)),
                "hayMas", false, "busquedaQuery", "short", "busquedaOffset", 1));
        preguntar("quiero ver la foto del short");
        ultimaRespuesta.andExpect(jsonPath("$.respuesta").value(ChatbotController_SIN_FOTO()))
                .andExpect(jsonPath("$.productos").isEmpty());
    }

    @Test
    @DisplayName("con foto: se mandan las tarjetas que sí tienen y el texto del modelo se respeta")
    void productoConFoto() throws Exception {
        controlador();
        when(chatbot.chat(any(), anyBoolean())).thenReturn(Mono.just("¡Aquí la tienes! 📸 ##BUSCAR[short,0]##"));
        when(chatbot.buscarProductos(eq("short"), eq(0), anyBoolean())).thenReturn(Map.of(
                "productos", List.of(Map.of("varianteId", 1, "nombre", "Short rojo", "tieneImagen", true),
                        Map.of("varianteId", 2, "nombre", "Short sin foto", "tieneImagen", false)),
                "hayMas", false, "busquedaQuery", "short", "busquedaOffset", 2));
        preguntar("quiero ver la foto del short");
        ultimaRespuesta.andExpect(jsonPath("$.respuesta").value("¡Aquí la tienes! 📸"))
                .andExpect(jsonPath("$.productos.length()").value(1))
                .andExpect(jsonPath("$.productos[0].varianteId").value(1));
    }

    @Test
    @DisplayName("solo el ADMIN llega al bot con info completa; un cliente o un visitante no")
    void quienRecibeInfoCompleta() throws Exception {
        controlador();
        when(chatbot.chat(any(), anyBoolean())).thenReturn(Mono.just("hola"));

        entrarComo("ROLE_ADMIN");
        preguntar("tienes short rojo");
        verify(chatbot).chat(any(), eq(true));

        entrarComo("ROLE_USUARIO");
        preguntar("tienes short rojo");
        verify(chatbot).chat(any(), eq(false));
    }

    private static String ChatbotController_SIN_FOTO() {
        return (String) ReflectionTestUtils.getField(ChatbotController.class, "SIN_FOTO");
    }
}
