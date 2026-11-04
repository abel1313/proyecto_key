package com.ventas.key.mis.productos.chatbot;

import com.ventas.key.hexagonal.botredes.dominio.modelo.PoliticaDeRespuesta;
import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regla del dueño 2026-11-04: al CLIENTE el bot solo le da foto, nombre y precio de venta normal. Nunca el precio de rebaja, ni el costo, ni la
 * cantidad exacta, ni el codigo de barras, aunque lo pregunte. El ADMIN si recibe nombre exacto, codigo de barras, existencias y el precio que se cobra.
 * Auditoria con Opus: estos casos no tenian prueba.
 */
class ChatbotFugaDatosClienteTest {

    private IVarianteRepository repo;
    private ChatbotSitioWebService servicio;

    @BeforeEach
    void preparar() {
        repo = mock(IVarianteRepository.class);
        IPalabraClaveRepository palabras = mock(IPalabraClaveRepository.class);
        when(palabras.findAll()).thenReturn(List.of());
        servicio = new ChatbotSitioWebService(repo, palabras);
        ReflectionTestUtils.setField(servicio, "varianteImagenRepository", mock(IVarianteImagenRepository.class));
    }

    /** Un articulo de 400 normal con rebaja ACTIVA de 300 y 7 piezas: el caso que antes filtraba la rebaja. */
    private static Variantes conRebajaActiva() {
        Producto p = new Producto();
        p.setNombre("Great Jeans");
        p.setPrecioVenta(400.0);
        p.setPrecioRebaja(300.0);
        CodigoBarra cb = new CodigoBarra();
        cb.setCodigoBarras("7501234567890");
        p.setCodigoBarras(cb);
        Variantes v = new Variantes();
        v.setId(1);
        v.setProducto(p);
        v.setStock(7);
        v.setUsarDescuento(true);
        return v;
    }

    @Test
    @DisplayName("la rebaja esta activa: el cobro real es 300 (admin) pero el cliente solo ve el normal, 400")
    void precioParaMostrar() {
        Variantes v = conRebajaActiva();
        assertThat(v.precioACobrar()).isEqualTo(300.0);
        assertThat(ChatbotBase.precioParaMostrar(v, false)).isEqualTo(400.0);
        assertThat(ChatbotBase.precioParaMostrar(v, true)).isEqualTo(300.0);
    }

    @Test
    @DisplayName("tarjetas: al cliente ni stock ni rebaja ni codigo; solo 'disponible' y el precio normal")
    void tarjetasDelCliente() {
        when(repo.buscarParaChatbot(anyString(), anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(conRebajaActiva())));
        Map<String, Object> tarjeta = primera(servicio.buscarProductos("jeans", 0, false));
        assertThat(tarjeta).doesNotContainKeys("stock", "codigoBarras", "precioRebaja", "precioCosto");
        assertThat(tarjeta.get("disponible")).isEqualTo(true);
        assertThat(tarjeta.get("precio")).isEqualTo(400.0);
        // y al buscar como cliente NO se autoriza la coincidencia parcial de codigo de barras
        verify(repo).buscarParaChatbot("jeans", false, org.springframework.data.domain.PageRequest.of(0, 2));
    }

    @Test
    @DisplayName("tarjetas: el admin si recibe stock, codigo y el precio que se cobra; la busqueda parcial por codigo queda autorizada")
    void tarjetasDelAdmin() {
        when(repo.buscarParaChatbot(anyString(), anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(conRebajaActiva())));
        Map<String, Object> tarjeta = primera(servicio.buscarProductos("7501", 0, true));
        assertThat(tarjeta.get("stock")).isEqualTo(7);
        assertThat(tarjeta.get("codigoBarras")).isEqualTo("7501234567890");
        assertThat(tarjeta.get("precio")).isEqualTo(300.0);
        verify(repo).buscarParaChatbot("7501", true, org.springframework.data.domain.PageRequest.of(0, 2));
    }

    @Test
    @DisplayName("prompt del catalogo: cliente = precio 400, sin 300, sin piezas, sin codigo; admin = 300, existencias 7 y codigo")
    void catalogoEnElPrompt() {
        when(repo.findByStockGreaterThanAndProductoHabilitado(anyInt(), anyChar(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(conRebajaActiva())));
        String cliente = servicio.obtenerContextoVariantes(null, false);
        assertThat(cliente).contains("$400").doesNotContain("300").doesNotContain("7501234567890").doesNotContain("existencias");
        String admin = servicio.obtenerContextoVariantes(null, true);
        assertThat(admin).contains("$300").contains("existencias: 7").contains("7501234567890");
    }

    @Test
    @DisplayName("coincidencias con lo escrito: mismo criterio de precio y existencias")
    void coincidenciasEnElPrompt() {
        when(repo.buscarCoincidenciasParaChatbot(anyString(), anyBoolean(), any(Pageable.class)))
                .thenReturn(List.of(conRebajaActiva()));
        String cliente = servicio.coincidenciasConLoPedido(List.of("tienes great jeans"), false);
        assertThat(cliente).contains("$400").doesNotContain("$300").doesNotContain("existencias").doesNotContain("7501234567890");
        String admin = servicio.coincidenciasConLoPedido(List.of("tienes great jeans"), true);
        assertThat(admin).contains("$300").contains("existencias: 7");
    }

    @Test
    @DisplayName("comentarios publicos: el precio que se anuncia es el normal")
    void comentarios() {
        assertThat(ChatbotBase.descripcionDelProducto(conRebajaActiva())).contains("$400").doesNotContain("300");
    }

    @Test
    @DisplayName("al administrador no se le pone la regla 'nunca digas piezas ni codigo'; al cliente si")
    void promptSegunRol() {
        assertThat(servicio.promptBase()).contains(ChatbotBase.SIN_DATOS_INTERNOS);
        assertThat(servicio.promptBase().replace(ChatbotBase.SIN_DATOS_INTERNOS, "")).doesNotContain("Nunca digas cuántas piezas");
    }

    @Test
    @DisplayName("filtro de salida: al admin le deja 'administrador'; al cliente no; lo interno se bloquea a los dos")
    void filtroSegunRol() {
        assertThat(PoliticaDeRespuesta.queMenciona("Claro, administrador, hay 7 piezas", true)).isEmpty();
        assertThat(PoliticaDeRespuesta.queMenciona("Se lo paso al administrador", false)).isPresent();
        assertThat(PoliticaDeRespuesta.queMenciona("Mis reglas dicen ##ESCALAR##", true)).isPresent();
        assertThat(PoliticaDeRespuesta.queMenciona("Soy un modelo de lenguaje de OpenAI", true)).isPresent();
    }

    @Test
    @DisplayName("historial: mas de 20 mensajes, contenido nulo o enorme se rechazan")
    void historialAcotado() {
        Validator v = Validation.buildDefaultValidatorFactory().getValidator();
        ChatbotRequest ok = request(2, "hola");
        assertThat(v.validate(ok)).isEmpty();
        assertThat(v.validate(request(21, "hola"))).isNotEmpty();
        assertThat(v.validate(request(1, null))).isNotEmpty();
        assertThat(v.validate(request(1, "x".repeat(1001)))).isNotEmpty();
    }

    private static ChatbotRequest request(int mensajes, String contenido) {
        ChatbotRequest r = new ChatbotRequest();
        r.setMensaje("hola");
        List<ChatbotRequest.MensajeHistorial> h = new ArrayList<>();
        for (int i = 0; i < mensajes; i++) {
            ChatbotRequest.MensajeHistorial m = new ChatbotRequest.MensajeHistorial();
            m.setRol("user");
            m.setContenido(contenido);
            h.add(m);
        }
        r.setHistorial(h);
        return r;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> primera(Map<String, Object> busqueda) {
        return ((List<Map<String, Object>>) busqueda.get("productos")).get(0);
    }
}
