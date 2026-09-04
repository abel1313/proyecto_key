package com.ventas.key.mis.productos.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockPart;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

// Cubre el criterio del usuario en las 3 zonas de la peticion (path, query, body JSON) mas el
// caso multipart -- prueba explicita de que el binario de un archivo NUNCA se revisa, solo los
// campos de texto, y que un dato legitimo con un caracter especial suelto (apostrofe en un
// nombre) NO se bloquea -- eso era un requisito explicito para no romper altas reales.
class InputSanitizationFilterTest {

    private final InputSanitizationFilter filter = nuevoFiltro();

    private InputSanitizationFilter nuevoFiltro() {
        InputSanitizationFilter f = new InputSanitizationFilter();
        ReflectionTestUtils.setField(f, "objectMapper", new ObjectMapper());
        return f;
    }

    private MockHttpServletResponse ejecutar(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void dejaPasarUnaPeticionLimpia() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/productos/buscarNombreOrCodigoBarra");
        request.setParameter("nombre", "O'Brien");
        MockHttpServletResponse response = ejecutar(request);
        assertThat(response.getStatus()).isEqualTo(200); // MockHttpServletResponse arranca en 200
    }

    @Test
    void rechazaScriptEnQueryParam() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/productos/buscarNombreOrCodigoBarra");
        request.setParameter("nombre", "<script>alert(1)</script>");
        MockHttpServletResponse response = ejecutar(request);
        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    void rechazaPathTraversalEnLaUrl() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/imagen/../../etc/passwd");
        MockHttpServletResponse response = ejecutar(request);
        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    void rechazaInyeccionSqlEnBodyJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/clientes/save");
        request.setContentType("application/json");
        request.setContent("{\"nombre\":\"a' OR '1'='1\"}".getBytes());
        MockHttpServletResponse response = ejecutar(request);
        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    void noBloqueaUnaComillaSueltaEnBodyJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/clientes/save");
        request.setContentType("application/json");
        request.setContent("{\"nombre\":\"O'Brien\"}".getBytes());
        MockHttpServletResponse response = ejecutar(request);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rechazaXssEnCampoDeTextoMultipartPeroIgnoraElBinarioDelArchivo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/tienda/v1/guardarConImagenes");
        request.setContentType("multipart/form-data; boundary=x");
        request.addPart(new MockPart("nombre", "<img src=x onerror=alert(1)>".getBytes()));
        MockHttpServletResponse response = ejecutar(request);
        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    void dejaPasarMultipartConArchivoBinarioSospechosoPorqueNoSeRevisaElBinario() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/tienda/v1/guardarConImagenes");
        request.setContentType("multipart/form-data; boundary=x");
        MockPart archivo = new MockPart("imagen", "foto.png", "<script>alert(1)</script>".getBytes());
        request.addPart(archivo);
        MockHttpServletResponse response = ejecutar(request);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
