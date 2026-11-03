package com.ventas.key.hexagonal.busquedapedido.infraestructura.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EscriturasWebInterceptorTest {

    private void despues(EscriturasWebInterceptor i, String metodo, String ruta, int estado) {
        MockHttpServletRequest req = new MockHttpServletRequest(metodo, ruta);
        MockHttpServletResponse res = new MockHttpServletResponse();
        res.setStatus(estado);
        i.afterCompletion(req, res, new Object(), null);
    }

    @Test
    @DisplayName("una escritura normal sube la versión del cache de pedidos; el escáner, las preferencias, el login y el chatbot no")
    void soloLasEscriturasDePedidos() {
        VersionDeListasDePedidos version = mock(VersionDeListasDePedidos.class);
        EscriturasWebInterceptor i = new EscriturasWebInterceptor(version);

        despues(i, "POST", "/mis-productos/v1/pedidos/5/articulos", 200);
        verify(version).subir(anyString());

        org.mockito.Mockito.clearInvocations(version);
        for (String ruta : new String[]{"/mis-productos/v1/escaner/sesiones", "/mis-productos/v1/escaner/sesiones/abc/codigos",
                "/mis-productos/v1/escaner/sesiones/abc", "/mis-productos/v1/escaner/sesiones/abc/cerrar",
                "/mis-productos/v1/preferencias-filtro/x", "/mis-productos/auth/login", "/mis-productos/chatbot/mensaje"}) {
            despues(i, ruta.contains("/sesiones/abc") && !ruta.endsWith("codigos") && !ruta.endsWith("cerrar") ? "DELETE" : "POST", ruta, 200);
        }
        verify(version, never()).subir(anyString());
    }
}
