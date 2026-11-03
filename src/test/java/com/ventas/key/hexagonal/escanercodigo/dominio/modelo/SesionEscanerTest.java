package com.ventas.key.hexagonal.escanercodigo.dominio.modelo;

import com.ventas.key.hexagonal.escanercodigo.dominio.excepcion.EscanerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SesionEscanerTest {

    private static final Instant T0 = Instant.parse("2026-11-02T12:00:00Z");

    private SesionEscaner nueva() {
        return new SesionEscaner("abc", "1.2.3.4", T0);
    }

    /** Mantiene viva la conexión como lo hace el celular (señal cada minuto) hasta ese momento. */
    private static void mantenerViva(SesionEscaner s, long hastaSegundos) {
        for (long seg = 60; seg <= hastaSegundos; seg += 60) {
            s.latido(T0.plusSeconds(seg));
        }
    }

    @Test
    @DisplayName("E1: dura como máximo 12 horas, aunque siga activa")
    void vigencia() {
        SesionEscaner s = nueva();
        mantenerViva(s, 12 * 3600 - 60);
        assertThat(s.estaVigente(T0.plusSeconds(12 * 3600 - 30))).isTrue();
        s.latido(T0.plusSeconds(12 * 3600 - 30));
        assertThat(s.estaVigente(T0.plusSeconds(12 * 3600))).isFalse();
        assertThatThrownBy(() -> s.registrar("7501234567890", T0.plusSeconds(12 * 3600 + 1))).isInstanceOf(EscanerException.SesionNoDisponible.class);
        assertThatThrownBy(() -> s.leer(0, T0.plusSeconds(12 * 3600 + 1))).isInstanceOf(EscanerException.SesionNoDisponible.class);
    }

    @Test
    @DisplayName("E2: se limpia el código y se rechaza lo que no parece código de barras")
    void validacion() {
        SesionEscaner s = nueva();
        assertThat(s.registrar("  7501234567890 \n", T0).codigo()).isEqualTo("7501234567890");
        for (String malo : new String[]{null, "", "  ", "ab", "a".repeat(101), "75012\n34567", "código-ñ"}) {
            assertThatThrownBy(() -> s.registrar(malo, T0)).as("«%s»", malo).isInstanceOf(EscanerException.CodigoInvalido.class);
        }
    }

    @Test
    @DisplayName("E3: la PC pide los códigos mayores al último que vio, y solo se guardan los últimos 20")
    void secuencia() {
        SesionEscaner s = nueva();
        s.registrar("AAA111", T0);
        s.registrar("BBB222", T0.plusSeconds(1));
        assertThat(s.leer(0, T0.plusSeconds(2)).codigos()).extracting(CodigoEscaneado::codigo).containsExactly("AAA111", "BBB222");
        assertThat(s.leer(1, T0.plusSeconds(2)).codigos()).extracting(CodigoEscaneado::codigo).containsExactly("BBB222");
        assertThat(s.leer(2, T0.plusSeconds(2)).codigos()).isEmpty();
        assertThat(s.leer(0, T0.plusSeconds(2)).ultimaSecuencia()).isEqualTo(2);

        SesionEscaner larga = nueva();
        for (int i = 0; i < 25; i++) {
            larga.registrar("COD" + i + "XX", T0.plusSeconds(i * 3L));
        }
        var lectura = larga.leer(0, T0.plusSeconds(80));
        assertThat(lectura.codigos()).hasSize(20);
        assertThat(lectura.ultimaSecuencia()).isEqualTo(25);
    }

    @Test
    @DisplayName("E4: máximo 30 códigos por minuto; pasado el minuto se puede de nuevo")
    void ritmo() {
        SesionEscaner s = nueva();
        for (int i = 0; i < 30; i++) {
            s.registrar("COD" + i + "XX", T0.plusMillis(i * 100L));
        }
        assertThatThrownBy(() -> s.registrar("DEMASIADO", T0.plusSeconds(5))).isInstanceOf(EscanerException.DemasiadasSolicitudes.class);
        assertThat(s.registrar("YA-PUEDE", T0.plusSeconds(65)).codigo()).isEqualTo("YA-PUEDE");
    }

    @Test
    @DisplayName("E7: sin ninguna actividad en 5 minutos se libera; cualquier señal (PC o celular) la mantiene")
    void inactividad() {
        SesionEscaner abandonada = nueva();
        assertThat(abandonada.estaVigente(T0.plusSeconds(299))).isTrue();
        assertThat(abandonada.estaVigente(T0.plusSeconds(301))).isFalse();

        SesionEscaner conLatidos = nueva();
        mantenerViva(conLatidos, 1800);
        assertThat(conLatidos.estaVigente(T0.plusSeconds(1800 + 200))).isTrue();
        assertThat(conLatidos.estaVigente(T0.plusSeconds(1800 + 301))).isFalse();     // dejó de dar señal

        SesionEscaner laPcPregunta = nueva();
        for (int seg = 60; seg <= 900; seg += 60) laPcPregunta.leer(0, T0.plusSeconds(seg));
        assertThat(laPcPregunta.estaVigente(T0.plusSeconds(1000))).isTrue();
    }

    @Test
    @DisplayName("E8: el celular está «conectado» 45 segundos después de su última señal; mandar un código también cuenta")
    void celularConectado() {
        SesionEscaner s = nueva();
        assertThat(s.estado(T0).celularConectado()).isFalse();
        s.latido(T0.plusSeconds(10));
        assertThat(s.estado(T0.plusSeconds(54)).celularConectado()).isTrue();
        assertThat(s.estado(T0.plusSeconds(56)).celularConectado()).isFalse();
        s.registrar("7501234567890", T0.plusSeconds(100));
        assertThat(s.leer(0, T0.plusSeconds(110)).celularConectado()).isTrue();
    }

    @Test
    @DisplayName("E9: la pregunta abierta se contesta en cuanto llega el código, y solo con los más nuevos que su «desde»")
    void preguntaAbierta() throws Exception {
        SesionEscaner s = nueva();
        s.registrar("VIEJO-1", T0);                                     // ya estaba antes de preguntar
        var espera = s.esperar(1, T0.plusSeconds(1));
        assertThat(espera.futuro()).isNotDone();

        s.registrar("NUEVO-2", T0.plusSeconds(2));

        var lectura = espera.futuro().get();
        assertThat(lectura.codigos()).extracting(CodigoEscaneado::codigo).containsExactly("NUEVO-2");
        assertThat(lectura.ultimaSecuencia()).isEqualTo(2);
    }

    @Test
    @DisplayName("E9: si ya hay algo más nuevo, se contesta al instante; cancelar quita la pregunta; una pregunta no se cuela con códigos viejos")
    void preguntaAbiertaCasos() throws Exception {
        SesionEscaner s = nueva();
        s.registrar("UNO-111", T0);
        assertThat(s.esperar(0, T0.plusSeconds(1)).futuro()).isCompleted();

        var cancelada = s.esperar(1, T0.plusSeconds(2));
        cancelada.cancelar().run();
        s.registrar("DOS-222", T0.plusSeconds(3));
        assertThat(cancelada.futuro()).isNotDone();                         // la quitaron: no se le avisa

        var dosEsperas = new CompletableFuture<?>[]{s.esperar(2, T0.plusSeconds(4)).futuro(), s.esperar(1, T0.plusSeconds(4)).futuro()};
        assertThat(dosEsperas[1]).isCompleted();                          // «desde 1» ya tiene el DOS-222
        s.registrar("TRES-333", T0.plusSeconds(5));
        assertThat(dosEsperas[0]).isCompleted();
    }

    @Test
    @DisplayName("E9: cuando el celular se conecta (primer latido) la pregunta abierta se contesta sin códigos pero con «conectado»; los latidos siguientes no")
    void latidoAvisaAlConectarse() throws Exception {
        SesionEscaner s = nueva();
        var espera = s.esperar(0, T0.plusSeconds(1));
        assertThat(espera.futuro()).isNotDone();

        s.latido(T0.plusSeconds(5));                                    // el celular acaba de abrir la página
        var lectura = espera.futuro().get();
        assertThat(lectura.celularConectado()).isTrue();
        assertThat(lectura.codigos()).isEmpty();

        var otra = s.esperar(0, T0.plusSeconds(6));
        s.latido(T0.plusSeconds(20));                                   // ya estaba conectado: no hay nada nuevo que avisar
        assertThat(otra.futuro()).isNotDone();
    }

    @Test
    @DisplayName("E10: máximo 3 preguntas abiertas; con una más, la más vieja se contesta vacía")
    void esperasMaximas() {
        SesionEscaner s = nueva();
        var primera = s.esperar(0, T0.plusSeconds(1));
        s.esperar(0, T0.plusSeconds(2));
        s.esperar(0, T0.plusSeconds(3));
        assertThat(primera.futuro()).isNotDone();

        var cuarta = s.esperar(0, T0.plusSeconds(4));
        assertThat(primera.futuro()).isCompleted();                     // la más vieja se liberó (vacía)
        assertThat(primera.futuro().join().codigos()).isEmpty();
        assertThat(cuarta.futuro()).isNotDone();
    }

    @Test
    @DisplayName("E11: al mandar un código se sabe si la PC lo estaba esperando (pregunta abierta, o una hace menos de 3 s)")
    void laPcLoEsperaba() {
        SesionEscaner nadiePidio = nueva();
        assertThat(nadiePidio.enviar("AAA111", T0.plusSeconds(1)).laPcLoEsperaba()).isFalse();

        SesionEscaner conPregunta = nueva();
        conPregunta.esperar(0, T0.plusSeconds(1));
        assertThat(conPregunta.enviar("AAA111", T0.plusSeconds(10)).laPcLoEsperaba()).isTrue();          // había una pregunta abierta

        SesionEscaner entreDosPreguntas = nueva();
        var p = entreDosPreguntas.esperar(0, T0.plusSeconds(1));
        p.cancelar().run();                                                                               // acaba una pregunta y aún no empieza la siguiente
        assertThat(entreDosPreguntas.enviar("AAA111", T0.plusSeconds(2)).laPcLoEsperaba()).isTrue();      // dentro del margen de 3 s

        SesionEscaner laPcSeFue = nueva();
        laPcSeFue.esperar(0, T0.plusSeconds(1)).cancelar().run();
        assertThat(laPcSeFue.enviar("AAA111", T0.plusSeconds(30)).laPcLoEsperaba()).isFalse();            // pasó el margen: nadie lo pide
    }

    @Test
    @DisplayName("E12: si la PC pide «después del 5» pero esta conexión (nueva, de la misma cuenta) va en 1, se le dan sus códigos desde el principio")
    void desdeMayorQueLaSecuenciaSeNormaliza() throws Exception {
        SesionEscaner nueva = nueva();
        nueva.registrar("PRIMERO-1", T0);
        assertThat(nueva.leer(5, T0.plusSeconds(1)).codigos()).extracting(CodigoEscaneado::codigo).containsExactly("PRIMERO-1");
        assertThat(nueva.esperar(5, T0.plusSeconds(2)).futuro().get().codigos()).hasSize(1);        // no se queda esperando para siempre
    }

    @Test
    @DisplayName("E5: cerrar avisa a quien esperaba (conexión terminada) y ya no acepta códigos")
    void cerrada() {
        SesionEscaner s = nueva();
        var espera = s.esperar(0, T0);
        s.cerrar();
        assertThat(s.estaVigente(T0)).isFalse();
        assertThatThrownBy(() -> espera.futuro().get()).isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(EscanerException.SesionNoDisponible.class);
        assertThatThrownBy(() -> s.registrar("7501234567890", T0)).isInstanceOf(EscanerException.SesionNoDisponible.class);
    }

    // ───────── E13: fotos ─────────

    private static String foto(int caracteres) {
        return "data:image/jpeg;base64," + "A".repeat(caracteres);
    }

    @Test
    @DisplayName("E13: una foto válida llega a la pregunta que sabe recibir fotos y no a la que solo recibe códigos")
    void fotoSoloALaQueLaAcepta() throws Exception {
        SesionEscaner s = nueva();
        EsperaDeCodigos soloCodigos = s.esperar(0, T0, Acepta.SOLO_CODIGOS);
        EsperaDeCodigos conFotos = s.esperar(0, T0, new Acepta(true, true));
        assertThat(s.enviarFoto(foto(1000), T0.plusSeconds(1))).isTrue();
        assertThat(soloCodigos.futuro()).isNotDone();
        LecturaDeCodigos l = conFotos.futuro().get();
        assertThat(l.fotos()).hasSize(1);
        assertThat(l.fotos().get(0).foto()).isEqualTo(foto(1000));
        assertThat(l.codigos()).isEmpty();
    }

    @Test
    @DisplayName("E13: sin pantalla que reciba fotos, se guarda pero avisa que nadie la esperaba")
    void fotoSinNadieEsperando() {
        SesionEscaner s = nueva();
        s.esperar(0, T0, Acepta.SOLO_CODIGOS);
        assertThat(s.enviarFoto(foto(100), T0.plusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("E13: la pregunta que llega después recibe la foto guardada (máximo 3, 5 minutos)")
    void fotoGuardadaYVence() throws Exception {
        SesionEscaner s = nueva();
        for (int i = 0; i < 4; i++) {
            s.enviarFoto(foto(100 + i), T0.plusSeconds(i));
        }
        LecturaDeCodigos l = s.esperar(0, T0.plusSeconds(10), new Acepta(false, true)).futuro().get();
        assertThat(l.fotos()).hasSize(3);
        assertThat(l.fotos().get(0).foto()).isEqualTo(foto(101));
        s.latido(T0.plusSeconds(200));
        s.latido(T0.plusSeconds(400));
        LecturaDeCodigos vieja = s.esperar(0, T0.plusSeconds(400), new Acepta(false, true)).futuro().getNow(null);
        assertThat(vieja).as("pasaron más de 5 minutos desde las fotos").isNull();
    }

    @Test
    @DisplayName("E13: rechaza lo que no es JPEG en base64, o pasa de 700 000 caracteres")
    void fotoInvalida() {
        SesionEscaner s = nueva();
        for (String mala : new String[]{null, "", "hola", "data:image/png;base64,AAAA", "data:image/jpeg;base64,", "data:image/jpeg;base64,@@@@",
                foto(700_000)}) {
            assertThatThrownBy(() -> s.enviarFoto(mala, T0)).as("«%.30s»", mala).isInstanceOf(EscanerException.FotoInvalida.class);
        }
    }

    @Test
    @DisplayName("E13: máximo 10 fotos por minuto")
    void fotoLimitePorMinuto() {
        SesionEscaner s = nueva();
        for (int i = 0; i < 10; i++) {
            s.enviarFoto(foto(100), T0.plusSeconds(i));
        }
        assertThatThrownBy(() -> s.enviarFoto(foto(100), T0.plusSeconds(11))).isInstanceOf(EscanerException.DemasiadasSolicitudes.class);
        s.enviarFoto(foto(100), T0.plusSeconds(75));
    }

    @Test
    @DisplayName("E13: los envíos inválidos también cuentan para el límite por minuto")
    void fotoInvalidaTambienCuenta() {
        SesionEscaner s = nueva();
        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> s.enviarFoto("basura", T0)).isInstanceOf(EscanerException.FotoInvalida.class);
        }
        assertThatThrownBy(() -> s.enviarFoto("basura", T0)).isInstanceOf(EscanerException.DemasiadasSolicitudes.class);
    }

    @Test
    @DisplayName("E13: leer respetando «acepta» devuelve la foto y no adelanta el contador sin ella")
    void leerConAceptaFotos() {
        SesionEscaner s = nueva();
        s.enviarFoto(foto(100), T0);
        assertThat(s.leer(0, T0.plusSeconds(1), new Acepta(false, true)).fotos()).hasSize(1);
        assertThat(s.leer(0, T0.plusSeconds(1)).fotos()).isEmpty();
    }
}
