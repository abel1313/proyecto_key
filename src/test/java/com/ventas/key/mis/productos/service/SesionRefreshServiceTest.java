package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.SesionRefresh;
import com.ventas.key.mis.productos.repository.IHistorialAccesoRepository;
import com.ventas.key.mis.productos.repository.ISesionRefreshRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Rotación del refresh token: el refresh doble legítimo no cierra la sesión; el reuso tardío sí. */
class SesionRefreshServiceTest {

    private ISesionRefreshRepository repo;
    private SesionRefreshService service;
    private SesionRefresh sesion;

    @BeforeEach
    void preparar() {
        repo = mock(ISesionRefreshRepository.class);
        service = new SesionRefreshService(repo, mock(IHistorialAccesoRepository.class));
        sesion = new SesionRefresh();
        sesion.setUsuarioId(1);
        sesion.setSessionId("S1");
        sesion.setJti("jti-1");
        sesion.setSessionStart(LocalDateTime.now().minusDays(1));
        sesion.setExpira(LocalDateTime.now().plusDays(6));
        when(repo.findBySessionId("S1")).thenReturn(Optional.of(sesion));
    }

    @Test
    void rotaYGuardaElJtiAnterior() {
        Optional<String> nuevo = service.rotar("S1", "jti-1");
        assertTrue(nuevo.isPresent());
        assertNotEquals("jti-1", nuevo.get());
        assertEquals("jti-1", sesion.getJtiAnterior());
        assertNotNull(sesion.getRotadoEn());
    }

    @Test
    void segundoRefreshDentroDeLaGraciaSeAceptaSinRotarOtraVez() {
        String vigente = service.rotar("S1", "jti-1").orElseThrow();
        Optional<String> segundo = service.rotar("S1", "jti-1");
        assertEquals(Optional.of(vigente), segundo);
        assertEquals(vigente, sesion.getJti());
        verify(repo, never()).delete(any());
    }

    @Test
    void reusoFueraDeLaGraciaCierraLaSesion() {
        service.rotar("S1", "jti-1");
        sesion.setRotadoEn(LocalDateTime.now().minusSeconds(SesionRefreshService.GRACIA_REUSO_SEGUNDOS + 5));
        assertTrue(service.rotar("S1", "jti-1").isEmpty());
        verify(repo).delete(sesion);
    }

    @Test
    void unJtiAjenoCierraLaSesion() {
        assertTrue(service.rotar("S1", "otro").isEmpty());
        verify(repo).delete(sesion);
    }
}
