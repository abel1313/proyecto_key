package com.ventas.key.mis.productos.redessociales;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TikTokDesconectarTest {

    private final ITikTokTokenRepository repo = mock(ITikTokTokenRepository.class);

    private TikTokGraphClient cliente() {
        TikTokGraphClient c = new TikTokGraphClient(repo);
        ReflectionTestUtils.setField(c, "clientKey", "");
        ReflectionTestUtils.setField(c, "clientSecret", "");
        return c;
    }

    @Test
    void sinCuentaNoBorraNada() {
        when(repo.existsById(1)).thenReturn(false);

        assertFalse(cliente().desconectar());
        verify(repo, never()).deleteById(any());
    }

    @Test
    void conCuentaBorraElTokenAunqueTikTokNoConfirme() {
        // Sin client key/secret no se puede llamar a TikTok: igual se tiene que borrar aquí.
        when(repo.existsById(1)).thenReturn(true);

        assertFalse(cliente().desconectar());
        verify(repo).deleteById(1);
    }
}
