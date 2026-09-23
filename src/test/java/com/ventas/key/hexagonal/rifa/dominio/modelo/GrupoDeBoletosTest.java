package com.ventas.key.hexagonal.rifa.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GrupoDeBoletosTest {

    private Participacion participacion(int id, String url) {
        return new Participacion(id, url, "compartio", LocalDate.now());
    }

    private GrupoDeBoletos grupoCon(Participacion... participaciones) {
        return new GrupoDeBoletos(7, "Juan Perez",
                new PerfilEnRed(Plataforma.FACEBOOK, "facebook.com/juan"),
                List.of(participaciones));
    }

    @Test
    @DisplayName("el total de boletos es la cantidad de participaciones, no 1 por ser un grupo (R1)")
    void cadaParticipacionEsUnBoleto() {
        GrupoDeBoletos grupo = grupoCon(
                participacion(1, "facebook.com/post/1"),
                participacion(2, "facebook.com/post/2"),
                participacion(3, "facebook.com/post/3"));

        assertThat(grupo.totalBoletos()).isEqualTo(3);
    }

    @Test
    @DisplayName("un grupo sin participaciones suma cero, no uno")
    void grupoVacioNoSumaBoletos() {
        GrupoDeBoletos grupo = grupoCon();

        assertThat(grupo.totalBoletos()).isZero();
        assertThat(grupo.estaVacio()).isTrue();
    }

    @Test
    @DisplayName("encuentra una url ya cargada aunque este escrita distinto")
    void buscaLaUrlNormalizada() {
        GrupoDeBoletos grupo = grupoCon(participacion(1, "https://facebook.com/post/1"));

        assertThat(grupo.yaTieneLaUrl("facebook.com/post/1/")).isTrue();
        assertThat(grupo.buscarPorUrl("FACEBOOK.COM/post/1").boletoId()).isEqualTo(1);
    }

    @Test
    @DisplayName("no encuentra una url que no esta")
    void noInventaCoincidencias() {
        GrupoDeBoletos grupo = grupoCon(participacion(1, "facebook.com/post/1"));

        assertThat(grupo.yaTieneLaUrl("facebook.com/post/999")).isFalse();
        assertThat(grupo.buscarPorUrl("facebook.com/post/999")).isNull();
    }

    @Test
    @DisplayName("agregar una participacion devuelve un grupo nuevo y no toca el original")
    void conDevuelveOtroGrupo() {
        GrupoDeBoletos original = grupoCon(participacion(1, "facebook.com/post/1"));

        GrupoDeBoletos ampliado = original.con(participacion(2, "facebook.com/post/2"));

        assertThat(original.totalBoletos()).isEqualTo(1);
        assertThat(ampliado.totalBoletos()).isEqualTo(2);
    }

    @Test
    @DisplayName("la ultima participacion es la fecha mas nueva del grupo")
    void ultimaParticipacionEsLaMasNueva() {
        GrupoDeBoletos grupo = grupoCon(
                new Participacion(1, "url/1", "like", LocalDate.of(2026, 9, 1)),
                new Participacion(2, "url/2", "compartio", LocalDate.of(2026, 9, 20)),
                new Participacion(3, "url/3", "comento", LocalDate.of(2026, 9, 10)));

        assertThat(grupo.ultimaParticipacion()).isEqualTo(LocalDate.of(2026, 9, 20));
    }

    @Test
    @DisplayName("un grupo vacio no tiene ultima participacion, y no explota al preguntarle")
    void grupoVacioNoTieneUltima() {
        assertThat(grupoCon().ultimaParticipacion()).isNull();
    }

    @Test
    @DisplayName("las participaciones sin fecha no rompen el calculo")
    void toleraFechasNulas() {
        GrupoDeBoletos grupo = grupoCon(
                new Participacion(1, "url/1", "like", null),
                new Participacion(2, "url/2", "compartio", LocalDate.of(2026, 9, 5)));

        assertThat(grupo.ultimaParticipacion()).isEqualTo(LocalDate.of(2026, 9, 5));
    }

    @Test
    @DisplayName("la lista de participaciones no se puede modificar por fuera")
    void laListaEsInmutable() {
        GrupoDeBoletos grupo = grupoCon(participacion(1, "facebook.com/post/1"));

        assertThat(grupo.participaciones()).isUnmodifiable();
    }
}
