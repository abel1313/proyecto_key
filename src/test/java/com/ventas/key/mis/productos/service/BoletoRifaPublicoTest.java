package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.entity.ConfigurarRifaVariante;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.repository.IBoletoRifaRepository;
import com.ventas.key.mis.productos.repository.IConcursanteRepository;
import com.ventas.key.mis.productos.repository.IConfigurarRifaRepository;
import com.ventas.key.mis.productos.repository.IConfigurarRifaVarianteRepository;
import com.ventas.key.mis.productos.repository.IGanadorRifaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * La página pública de la ruleta se comparte por link (/ruleta/48) y el id va en la URL,
 * o sea que se puede tantear cambiando el número. Antes cualquier id servía: con el link
 * de la rifa de este mes se podía entrar a la del mes pasado y ver sus participantes.
 *
 * Estos tests fijan la regla: por la vía pública solo pasa la rifa que cumple LAS CUATRO --
 * es de PLATAFORMAS, el negocio la marcó como publicada, sigue activa y ya empezó su rango
 * de boletos. Cualquier otra responde como inexistente. El admin, que llega con sesión,
 * sigue viendo todas.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BoletoRifaPublicoTest {

    @Mock private IBoletoRifaRepository iBoletoRifaRepository;
    @Mock private IConcursanteRepository iConcursanteRepository;
    @Mock private IConfigurarRifaRepository iConfigurarRifaRepository;
    @Mock private IConfigurarRifaVarianteRepository iConfigurarRifaVarianteRepository;
    @Mock private IGanadorRifaRepository iGanadorRifaRepository;
    @Mock private ConfigurarRifaVarianteService configurarRifaVarianteService;
    @Mock private EmailService emailService;

    private BoletoRifaServiceImpl service;

    private static final int RIFA_PUBLICADA = 48;
    private static final int RIFA_DE_OTRO_MES = 47;

    @BeforeEach
    void setUp() {
        service = new BoletoRifaServiceImpl(iBoletoRifaRepository, iConcursanteRepository,
                iConfigurarRifaRepository, iConfigurarRifaVarianteRepository,
                iGanadorRifaRepository, configurarRifaVarianteService, emailService);

        when(iConfigurarRifaVarianteRepository.findByConfigurarRifaIdOrderByOrdenAsc(anyInt()))
                .thenReturn(List.of());
        when(iBoletoRifaRepository.findByRifaId(anyInt())).thenReturn(List.of());
        when(iGanadorRifaRepository.findGanadoresByRifaId(anyInt())).thenReturn(List.of());
    }

    /** Una rifa que cumple las cuatro condiciones; cada test rompe la que le toca. */
    private ConfigurarRifa rifaPublicable(int id) {
        ConfigurarRifa r = new ConfigurarRifa();
        r.setId(id);
        r.setTipo(ConfigurarRifa.TipoRifa.PLATAFORMAS);
        r.setPublica(true);
        r.setActiva(true);
        r.setFechaInicioBoletos(LocalDate.now().minusDays(3));
        return r;
    }

    private void existe(ConfigurarRifa r) {
        when(iConfigurarRifaRepository.findById(r.getId())).thenReturn(Optional.of(r));
    }

    @Test
    void laRifaPublicadaSeVeDesdeElLinkPublico() {
        existe(rifaPublicable(RIFA_PUBLICADA));

        assertThat(service.obtenerEstado(RIFA_PUBLICADA, true)).isNotNull();
    }

    @Test
    void cambiarElNumeroDelLinkNoAbreOtraRifaSoloPorEstarActiva() {
        // El caso que originó todo: la rifa de otro mes existe, está activa y es del mismo
        // tipo, pero nadie la publicó. Antes se abría entera con solo saber el id.
        ConfigurarRifa otra = rifaPublicable(RIFA_DE_OTRO_MES);
        otra.setPublica(false);
        existe(otra);

        assertThatThrownBy(() -> service.obtenerEstado(RIFA_DE_OTRO_MES, true))
                .isInstanceOf(ExceptionDataNotFound.class);
    }

    @Test
    void unaRifaYaTerminadaDejaDeAbrirse() {
        // activa se apaga sola al sortearse el último premio.
        ConfigurarRifa terminada = rifaPublicable(RIFA_PUBLICADA);
        terminada.setActiva(false);
        existe(terminada);

        assertThatThrownBy(() -> service.obtenerEstado(RIFA_PUBLICADA, true))
                .isInstanceOf(ExceptionDataNotFound.class);
    }

    @Test
    void unaRifaPublicadaQueTodaviaNoEmpiezaNoSeAsoma() {
        ConfigurarRifa futura = rifaPublicable(RIFA_PUBLICADA);
        futura.setFechaInicioBoletos(LocalDate.now().plusDays(2));
        existe(futura);

        assertThatThrownBy(() -> service.obtenerEstado(RIFA_PUBLICADA, true))
                .isInstanceOf(ExceptionDataNotFound.class);
    }

    @Test
    void elPrimerDiaDelRangoYaCuentaComoEmpezada() {
        ConfigurarRifa arranca = rifaPublicable(RIFA_PUBLICADA);
        arranca.setFechaInicioBoletos(LocalDate.now());
        existe(arranca);

        assertThat(service.obtenerEstado(RIFA_PUBLICADA, true)).isNotNull();
    }

    @Test
    void pasadaLaFechaDeBoletosLaPaginaSigueVivaParaVerElSorteo() {
        // El sorteo se hace DESPUÉS de que cierra el registro: si la página muriera con el
        // rango, nadie alcanzaría a ver la ruleta girar.
        ConfigurarRifa cerrada = rifaPublicable(RIFA_PUBLICADA);
        cerrada.setFechaInicioBoletos(LocalDate.now().minusMonths(1));
        cerrada.setFechaFinBoletos(LocalDate.now().minusDays(5));
        existe(cerrada);

        assertThat(service.obtenerEstado(RIFA_PUBLICADA, true)).isNotNull();
    }

    @Test
    void unaRifaDeOtroTipoTampocoSeAbrePorAhi() {
        // Las MENSUAL/DIARIA no tienen ruleta; servirlas solo delataría que existen.
        ConfigurarRifa mensual = rifaPublicable(RIFA_DE_OTRO_MES);
        mensual.setTipo(ConfigurarRifa.TipoRifa.MENSUAL);
        existe(mensual);

        assertThatThrownBy(() -> service.obtenerEstado(RIFA_DE_OTRO_MES, true))
                .isInstanceOf(ExceptionDataNotFound.class);
    }

    @Test
    void unaRifaNoPublicadaSeVeIgualQueUnaQueNoExiste() {
        ConfigurarRifa otra = rifaPublicable(RIFA_DE_OTRO_MES);
        otra.setPublica(false);
        existe(otra);
        when(iConfigurarRifaRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerEstado(RIFA_DE_OTRO_MES, true))
                .isInstanceOf(ExceptionDataNotFound.class)
                .hasMessage("Rifa no encontrada");
        assertThatThrownBy(() -> service.obtenerEstado(999, true))
                .isInstanceOf(ExceptionDataNotFound.class)
                .hasMessage("Rifa no encontrada");
    }

    @Test
    void elAdminSigueViendoUnaRifaNiPublicadaNiActiva() {
        ConfigurarRifa vieja = rifaPublicable(RIFA_DE_OTRO_MES);
        vieja.setPublica(false);
        vieja.setActiva(false);
        existe(vieja);

        assertThat(service.obtenerEstado(RIFA_DE_OTRO_MES)).isNotNull();
    }

    @Test
    void girarYReiniciarDesdeElLinkTambienRespetanElFiltro() {
        ConfigurarRifa otra = rifaPublicable(RIFA_DE_OTRO_MES);
        otra.setPublica(false);
        existe(otra);

        assertThatThrownBy(() -> service.sortear(RIFA_DE_OTRO_MES, true))
                .isInstanceOf(ExceptionDataNotFound.class);
        assertThatThrownBy(() -> service.reiniciar(RIFA_DE_OTRO_MES, true))
                .isInstanceOf(ExceptionDataNotFound.class);
    }

    @Test
    void unPremioDeOtraRifaNoSePuedeAbrirConElIdDeLaPublicada() {
        existe(rifaPublicable(RIFA_PUBLICADA));

        ConfigurarRifaVariante premioAjeno = new ConfigurarRifaVariante();
        premioAjeno.setId(7);
        premioAjeno.setConfigurarRifa(rifaPublicable(RIFA_DE_OTRO_MES));
        when(iConfigurarRifaVarianteRepository.findById(7)).thenReturn(Optional.of(premioAjeno));

        assertThatThrownBy(() -> service.detallePremioPublico(RIFA_PUBLICADA, 7))
                .isInstanceOf(ExceptionDataNotFound.class);
    }
}
