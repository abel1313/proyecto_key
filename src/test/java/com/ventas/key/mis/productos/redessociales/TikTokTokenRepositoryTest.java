package com.ventas.key.mis.productos.redessociales;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// Regresion 2026-10-01: al conectar TikTok en prod el INSERT salia sin id ("Field 'id' doesn't
// have a default value") porque la entidad usaba id IDENTITY sobre una columna sin AUTO_INCREMENT.
// La fila tiene que ser siempre la id=1, tambien despues de desconectar y volver a conectar.
@DataJpaTest
@ActiveProfiles("test")
class TikTokTokenRepositoryTest {

    @Autowired private ITikTokTokenRepository repo;

    private void guardar(String accessToken) {
        TikTokToken token = repo.findById(1).orElseGet(TikTokToken::new);
        token.setId(1);
        token.setAccessToken(accessToken);
        token.setActualizadoEn(LocalDateTime.now());
        repo.saveAndFlush(token);
    }

    @Test
    void conectarGuardaLaFilaConId1() {
        guardar("a1");

        assertThat(repo.findById(1)).get().extracting(TikTokToken::getAccessToken).isEqualTo("a1");
        assertThat(repo.count()).isEqualTo(1);
    }

    @Test
    void refrescarActualizaLaMismaFila() {
        guardar("a1");
        guardar("a2");

        assertThat(repo.count()).isEqualTo(1);
        assertThat(repo.findById(1)).get().extracting(TikTokToken::getAccessToken).isEqualTo("a2");
    }

    @Test
    void desconectarYVolverAConectarVuelveASerId1() {
        guardar("a1");
        repo.deleteById(1);
        repo.flush();
        guardar("b1");

        assertThat(repo.count()).isEqualTo(1);
        assertThat(repo.findById(1)).get().extracting(TikTokToken::getAccessToken).isEqualTo("b1");
    }
}
