package com.ventas.key.hexagonal.rifa.aplicacion.servicio;

import com.ventas.key.hexagonal.rifa.dominio.excepcion.CargaSinParticipacionesException;
import com.ventas.key.hexagonal.rifa.dominio.excepcion.GrupoNoEncontradoException;
import com.ventas.key.hexagonal.rifa.dominio.excepcion.UrlParticipacionDuplicadaException;
import com.ventas.key.hexagonal.rifa.dominio.modelo.GrupoDeBoletos;
import com.ventas.key.hexagonal.rifa.dominio.modelo.ModoDeCarga;
import com.ventas.key.hexagonal.rifa.dominio.modelo.Participacion;
import com.ventas.key.hexagonal.rifa.dominio.modelo.PerfilEnRed;
import com.ventas.key.hexagonal.rifa.dominio.modelo.Plataforma;
import com.ventas.key.hexagonal.rifa.dominio.puerto.entrada.GestionarBoletosCasoUso.CargaDeParticipaciones;
import com.ventas.key.hexagonal.rifa.dominio.puerto.entrada.GestionarBoletosCasoUso.NuevaParticipacion;
import com.ventas.key.hexagonal.rifa.dominio.puerto.salida.BoletosDeRifaPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestionarBoletosServiceTest {

    private static final Integer RIFA_ID = 3;
    private static final Integer CONCURSANTE_ID = 7;

    @Mock
    private BoletosDeRifaPort boletos;

    private GestionarBoletosService service;

    private final PerfilEnRed perfil = new PerfilEnRed(Plataforma.FACEBOOK, "facebook.com/juan");

    @BeforeEach
    void setUp() {
        service = new GestionarBoletosService(boletos);
        when(boletos.buscarUrlEnLaRifa(anyInt(), anyString())).thenReturn(Optional.empty());
        when(boletos.buscarGrupo(anyInt(), any())).thenReturn(Optional.of(grupoCon(1)));
    }

    private GrupoDeBoletos grupoCon(int cuantas) {
        return new GrupoDeBoletos(CONCURSANTE_ID, "Juan Perez", perfil,
                java.util.stream.IntStream.rangeClosed(1, cuantas)
                        .mapToObj(i -> new Participacion(i, "facebook.com/post/" + i, "compartio",
                                LocalDate.now()))
                        .toList());
    }

    private NuevaParticipacion unica(String url) {
        return new NuevaParticipacion(url, "compartio", ModoDeCarga.UNICA);
    }

    private CargaDeParticipaciones carga(NuevaParticipacion... participaciones) {
        return new CargaDeParticipaciones(RIFA_ID, CONCURSANTE_ID, Plataforma.FACEBOOK,
                "facebook.com/juan", List.of(participaciones));
    }

    // ── La regla que mas importa: una participacion = una fila = una chance (D4) ──────────

    @Test
    @DisplayName("tres participaciones crean TRES filas, no una con tres urls adentro")
    void cadaParticipacionEsUnaFila() {
        service.cargar(carga(unica("facebook.com/post/1"), unica("facebook.com/post/2"),
                unica("facebook.com/post/3")));

        verify(boletos, times(3)).crearParticipacion(eq(CONCURSANTE_ID), any(), any());
    }

    @Test
    @DisplayName("la cabecera se repite en cada fila: el sorteo lee la fila, no el grupo")
    void cadaFilaLlevaSuPerfil() {
        service.cargar(carga(unica("facebook.com/post/1"), unica("facebook.com/post/2")));

        ArgumentCaptor<PerfilEnRed> capturado = ArgumentCaptor.forClass(PerfilEnRed.class);
        verify(boletos, times(2)).crearParticipacion(any(), capturado.capture(), any());

        assertThat(capturado.getAllValues())
                .allSatisfy(p -> assertThat(p.mismoQue(perfil)).isTrue());
    }

    @Test
    @DisplayName("la url de cada participacion viaja completa a su fila")
    void cadaFilaLlevaSuUrl() {
        service.cargar(carga(unica("facebook.com/post/1"), unica("facebook.com/post/2")));

        ArgumentCaptor<Participacion> capturado = ArgumentCaptor.forClass(Participacion.class);
        verify(boletos, times(2)).crearParticipacion(any(), any(), capturado.capture());

        assertThat(capturado.getAllValues())
                .extracting(Participacion::urlParticipacion)
                .containsExactly("facebook.com/post/1", "facebook.com/post/2");
    }

    // ── Regla D2: el modo de carga ────────────────────────────────────────────────────────

    @Test
    @DisplayName("modo UNICA rechaza una url que ya esta cargada, y dice de quien es")
    void modoUnicaRechazaDuplicada() {
        when(boletos.buscarUrlEnLaRifa(RIFA_ID, "facebook.com/post/1"))
                .thenReturn(Optional.of(new BoletosDeRifaPort.DuenoDeLaUrl(99, 42, "Pedro Lopez")));

        UrlParticipacionDuplicadaException e = catchThrowableOfType(
                () -> service.cargar(carga(unica("facebook.com/post/1"))),
                UrlParticipacionDuplicadaException.class);

        assertThat(e.getNombreDelDueno()).isEqualTo("Pedro Lopez");
        assertThat(e.getBoletoExistenteId()).isEqualTo(99);
        assertThat(e.getMessage()).contains("Pedro Lopez").contains("REPETIDA_PERMITIDA");
    }

    @Test
    @DisplayName("modo REPETIDA_PERMITIDA la acepta aunque ya exista")
    void modoRepetidaPermitidaLaDejaPasar() {
        when(boletos.buscarUrlEnLaRifa(RIFA_ID, "facebook.com/post/1"))
                .thenReturn(Optional.of(new BoletosDeRifaPort.DuenoDeLaUrl(99, 42, "Pedro Lopez")));

        service.cargar(carga(new NuevaParticipacion("facebook.com/post/1", "compartio",
                ModoDeCarga.REPETIDA_PERMITIDA)));

        verify(boletos).crearParticipacion(any(), any(), any());
    }

    @Test
    @DisplayName("sin modo se asume UNICA: el default protege el sorteo")
    void sinModoSeAsumeUnica() {
        when(boletos.buscarUrlEnLaRifa(RIFA_ID, "facebook.com/post/1"))
                .thenReturn(Optional.of(new BoletosDeRifaPort.DuenoDeLaUrl(99, 42, "Pedro Lopez")));

        assertThatThrownBy(() -> service.cargar(
                carga(new NuevaParticipacion("facebook.com/post/1", "compartio", null))))
                .isInstanceOf(UrlParticipacionDuplicadaException.class);
    }

    @Test
    @DisplayName("dos urls iguales dentro del MISMO alta tambien se rechazan")
    void duplicadasEntreSi() {
        assertThatThrownBy(() -> service.cargar(
                carga(unica("facebook.com/post/1"), unica("https://facebook.com/post/1/"))))
                .isInstanceOf(UrlParticipacionDuplicadaException.class);

        verify(boletos, never()).crearParticipacion(any(), any(), any());
    }

    // ── Todo o nada ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("si la tercera url choca, no se carga NINGUNA")
    void todoONada() {
        when(boletos.buscarUrlEnLaRifa(RIFA_ID, "facebook.com/post/3"))
                .thenReturn(Optional.of(new BoletosDeRifaPort.DuenoDeLaUrl(99, 42, "Pedro Lopez")));

        assertThatThrownBy(() -> service.cargar(carga(unica("facebook.com/post/1"),
                unica("facebook.com/post/2"), unica("facebook.com/post/3"))))
                .isInstanceOf(UrlParticipacionDuplicadaException.class);

        verify(boletos, never()).crearParticipacion(any(), any(), any());
    }

    // ── Cabecera vacia ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("un perfil sin ninguna url no se guarda: es una cabecera vacia")
    void sinParticipacionesNoSeGuarda() {
        assertThatThrownBy(() -> service.cargar(carga()))
                .isInstanceOf(CargaSinParticipacionesException.class);

        verify(boletos, never()).crearParticipacion(any(), any(), any());
    }

    @Test
    @DisplayName("las urls en blanco se descartan y no cuentan como boleto")
    void lasUrlsVaciasNoCuentan() {
        service.cargar(carga(unica("facebook.com/post/1"), unica("   "), unica(null)));

        verify(boletos, times(1)).crearParticipacion(any(), any(), any());
    }

    @Test
    @DisplayName("si TODAS vienen en blanco, falla en vez de guardar una cabecera sola")
    void todasVaciasFalla() {
        assertThatThrownBy(() -> service.cargar(carga(unica("  "), unica(null))))
                .isInstanceOf(CargaSinParticipacionesException.class);
    }

    // ── R3: sumar y restar sin recargar la cabecera ───────────────────────────────────────

    @Test
    @DisplayName("agregar una participacion no vuelve a pedir nombre ni perfil")
    void agregarUsaElGrupoQueYaEstaba() {
        service.agregarParticipacion(RIFA_ID, Plataforma.FACEBOOK, "facebook.com/juan",
                unica("facebook.com/post/9"));

        verify(boletos).crearParticipacion(eq(CONCURSANTE_ID), any(), any());
    }

    @Test
    @DisplayName("agregar a un perfil que no existe falla en vez de crearlo callado")
    void agregarAGrupoInexistenteFalla() {
        when(boletos.buscarGrupo(anyInt(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.agregarParticipacion(RIFA_ID, Plataforma.FACEBOOK,
                "facebook.com/nadie", unica("facebook.com/post/9")))
                .isInstanceOf(GrupoNoEncontradoException.class);

        verify(boletos, never()).crearParticipacion(any(), any(), any());
    }

    @Test
    @DisplayName("quitar una participacion borra su fila")
    void quitarBorraLaFila() {
        when(boletos.gruposDeLaRifa(RIFA_ID)).thenReturn(List.of(grupoCon(3)));

        service.quitarParticipacion(RIFA_ID, 2);

        verify(boletos).borrarParticipacion(2);
    }

    @Test
    @DisplayName("quitar un boleto que no es de esta rifa falla")
    void quitarBoletoAjenoFalla() {
        when(boletos.gruposDeLaRifa(RIFA_ID)).thenReturn(List.of(grupoCon(3)));

        assertThatThrownBy(() -> service.quitarParticipacion(RIFA_ID, 999))
                .isInstanceOf(GrupoNoEncontradoException.class);

        verify(boletos, never()).borrarParticipacion(anyInt());
    }

    @Test
    @DisplayName("quitar la ultima participacion devuelve el grupo vacio, no un error")
    void grupoPuedeQuedarVacio() {
        when(boletos.gruposDeLaRifa(RIFA_ID)).thenReturn(List.of(grupoCon(1)));
        when(boletos.buscarGrupo(anyInt(), any())).thenReturn(Optional.empty());

        GrupoDeBoletos resultado = service.quitarParticipacion(RIFA_ID, 1);

        assertThat(resultado.estaVacio()).isTrue();
        assertThat(resultado.totalBoletos()).isZero();
        assertThat(resultado.nombreConcursante()).isEqualTo("Juan Perez");
    }
}
