package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class TikTokUrlAutorizacionTest {

    private TikTokGraphClient cliente(String clientKey) {
        TikTokGraphClient c = new TikTokGraphClient(mock(ITikTokTokenRepository.class));
        ReflectionTestUtils.setField(c, "clientKey", clientKey);
        return c;
    }

    @Test
    void armaLaUrlConClientKeyScopesYRedirectCodificados() {
        String url = cliente("abc123").urlAutorizacion("https://qa.shop.novedades-jade.com.mx/tiktok/callback", "xyz");

        assertTrue(url.startsWith("https://www.tiktok.com/v2/auth/authorize/?client_key=abc123"));
        assertTrue(url.contains("&scope=user.info.basic%2Cvideo.upload"));
        assertTrue(url.contains("&response_type=code"));
        assertTrue(url.contains("&redirect_uri=https%3A%2F%2Fqa.shop.novedades-jade.com.mx%2Ftiktok%2Fcallback"));
        assertTrue(url.endsWith("&state=xyz"));
    }

    @Test
    void aceptaLocalhostParaDesarrollo() {
        assertDoesNotThrow(() -> cliente("k").urlAutorizacion("http://localhost:4200/tiktok/callback", "s"));
    }

    @Test
    void rechazaUnRedirectQueNoEsElCallback() {
        TikTokGraphClient c = cliente("k");
        assertThrows(ExceptionErrorInesperado.class, () -> c.urlAutorizacion("https://otro.com/robar", "s"));
        assertThrows(ExceptionErrorInesperado.class, () -> c.urlAutorizacion("http://shop.com/tiktok/callback", "s"));
        assertThrows(ExceptionErrorInesperado.class, () -> c.urlAutorizacion(null, "s"));
    }

    @Test
    void sinClientKeyAvisaQueFaltaConfigurar() {
        ExceptionErrorInesperado e = assertThrows(ExceptionErrorInesperado.class,
                () -> cliente("").urlAutorizacion("https://shop.com/tiktok/callback", "s"));
        assertTrue(e.getMessage().contains("TIKTOK_CLIENT_KEY"));
    }
}
