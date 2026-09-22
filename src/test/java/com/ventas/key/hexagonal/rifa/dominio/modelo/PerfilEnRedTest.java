package com.ventas.key.hexagonal.rifa.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El agrupamiento entero depende de que dos formas de escribir el mismo perfil se
 * reconozcan como la misma persona. Si esto falla, cada variacion de tipeo abre un grupo
 * nuevo y la pantalla vuelve a las filas sueltas.
 */
class PerfilEnRedTest {

    private PerfilEnRed facebook(String url) {
        return new PerfilEnRed(Plataforma.FACEBOOK, url);
    }

    @Test
    @DisplayName("el esquema https no separa a la persona de si misma")
    void ignoraElEsquema() {
        assertThat(facebook("https://facebook.com/juan").mismoQue(facebook("facebook.com/juan")))
                .isTrue();
    }

    @Test
    @DisplayName("la barra final tampoco")
    void ignoraLaBarraFinal() {
        assertThat(facebook("facebook.com/juan/").mismoQue(facebook("facebook.com/juan"))).isTrue();
    }

    @Test
    @DisplayName("ni el www, ni las mayusculas, ni los espacios de los costados")
    void ignoraWwwMayusculasYEspacios() {
        assertThat(facebook("  https://WWW.Facebook.com/Juan/  ").mismoQue(facebook("facebook.com/juan")))
                .isTrue();
    }

    @Test
    @DisplayName("la misma url en dos plataformas son dos personas distintas (R2 y D3)")
    void laPlataformaFormaParteDeLaClave() {
        PerfilEnRed enFacebook = new PerfilEnRed(Plataforma.FACEBOOK, "red.com/juan");
        PerfilEnRed enInstagram = new PerfilEnRed(Plataforma.INSTAGRAM, "red.com/juan");

        assertThat(enFacebook.mismoQue(enInstagram)).isFalse();
    }

    @Test
    @DisplayName("dos perfiles distintos de la misma red no se confunden")
    void perfilesDistintosNoSeMezclan() {
        assertThat(facebook("facebook.com/juan").mismoQue(facebook("facebook.com/pedro"))).isFalse();
    }

    @Test
    @DisplayName("guarda la url tal como se escribio, aunque compare normalizada")
    void conservaLoQueEscribioElAdmin() {
        PerfilEnRed perfil = facebook("https://www.Facebook.com/Juan/");

        assertThat(perfil.urlPerfil()).isEqualTo("https://www.Facebook.com/Juan/");
        assertThat(perfil.clave()).isEqualTo("FACEBOOK|facebook.com/juan");
    }
}
