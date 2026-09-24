package com.ventas.key.hexagonal.botredes.aplicacion.servicio;

import com.ventas.key.hexagonal.botredes.dominio.modelo.Canal;
import com.ventas.key.hexagonal.botredes.dominio.modelo.ComentarioOriginal;
import com.ventas.key.hexagonal.botredes.dominio.modelo.Interaccion;
import com.ventas.key.hexagonal.botredes.dominio.modelo.RedSocial;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.AvisoAdminPort;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.CerebroPort;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.ControlDeAbusoPort;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.CuentaDelNegocioPort;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.PausasPort;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.ProductoDePublicacionPort;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.RegistroInteraccionesPort;
import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.RespuestaEnRedPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// Reglas acordadas con el dueño el 2026-09-24 (botredes/README.md).
class AtenderInteraccionServiceTest {

    private static final String PAGINA = "pagina-1";
    private static final String CLIENTA = "clienta-1";
    private static final String POST = "post-1";

    private final RelojManual reloj = new RelojManual(LocalDateTime.of(2026, 9, 24, 15, 0));
    private FakeRegistro registro;
    private FakePausas pausas;
    private FakeCerebro cerebro;
    private Map<String, Integer> productos;
    private FakeRed red;
    private List<String> avisos;
    private AtenderInteraccionService service;

    @BeforeEach
    void setUp() {
        registro = new FakeRegistro();
        pausas = new FakePausas();
        cerebro = new FakeCerebro();
        productos = new HashMap<>();
        red = new FakeRed();
        avisos = new ArrayList<>();
        ProductoDePublicacionPort productoPort = id -> Optional.ofNullable(productos.get(id));
        AvisoAdminPort avisoPort = (i, motivo) -> avisos.add(i.id() + ":" + motivo);
        CuentaDelNegocioPort cuenta = (r, autor) -> PAGINA.equals(autor);
        service = new AtenderInteraccionService(registro, pausas, cerebro, productoPort, red, avisoPort,
                new SinAbuso(), cuenta, Duration.ofMinutes(30), reloj);
    }

    // ---------- Comentarios en publicaciones subidas directo (sin producto) ----------

    @Test
    void siPreguntaAlgoEnUnaPublicacionSinProductoSaludaTeAvisaYSePausa() {
        cerebro.respuesta = "##ESCALAR##";

        service.atender(comentario("c1", "¿Qué precio tiene?"));

        assertThat(cerebro.llamadas).containsExactly("sinProducto:facebook:primera");
        assertThat(red.enviados).hasSize(1);
        assertThat(red.enviados.get(0)).startsWith("comentario:facebook:c1:¡Hola!")
                .doesNotContain("asistente automático").contains("En un momento te compartimos la información");
        assertThat(avisos).hasSize(1);
        assertThat(pausas.desde(Canal.COMENTARIO, CLIENTA, POST)).contains(reloj.ahora());
        assertThat(registro.guardadas).containsKey("c1");
    }

    @Test
    void siSoloEsUnHalagoAgradeceSinAvisarNiPausar() {
        cerebro.respuesta = "¡Hola! Muchas gracias 💖";

        service.atender(comentario("c1", "Qué bonita"));

        assertThat(red.enviados).containsExactly("comentario:facebook:c1:¡Hola! Muchas gracias 💖");
        assertThat(avisos).isEmpty();
        assertThat(pausas.guardadas).isEmpty();
    }

    @Test
    void siNoEntiendeElComentarioIgualSaludaConCortesia() {
        cerebro.respuesta = "asdf ##FAREWELL##";

        service.atender(comentario("c1", "asdfgh"));

        assertThat(red.enviados.get(0)).contains("Gracias por tu comentario");
        assertThat(avisos).isEmpty();
    }

    // ---------- Comentarios en publicaciones subidas desde el panel (con producto) ----------

    @Test
    void enUnaPublicacionDelPanelContestaSobreEseProducto() {
        productos.put(POST, 7);
        cerebro.respuesta = "¡Hola! Cuesta $300 😊";

        service.atender(comentario("c1", "¿Precio?"));

        assertThat(cerebro.llamadas).containsExactly("producto:7:facebook:primera");
        assertThat(red.enviados).containsExactly("comentario:facebook:c1:¡Hola! Cuesta $300 😊");
        assertThat(avisos).isEmpty();
    }

    // ---------- Pausa de 30 minutos ----------

    @Test
    void mientrasLaAtiendeUnaPersonaElBotNoContestaNiAvisa() {
        pausas.pausar(Canal.COMENTARIO, CLIENTA, POST, reloj.ahora().minusMinutes(10));

        service.atender(comentario("c1", "¿Y en negro?"));

        assertThat(red.enviados).isEmpty();
        assertThat(avisos).isEmpty();
        assertThat(cerebro.llamadas).isEmpty();
    }

    @Test
    void pasadosLosTreintaMinutosElBotLaRetoma() {
        pausas.pausar(Canal.COMENTARIO, CLIENTA, POST, reloj.ahora().minusMinutes(31));
        cerebro.respuesta = "##ESCALAR##";

        service.atender(comentario("c1", "¿Sigue disponible?"));

        assertThat(red.enviados).hasSize(1);
        assertThat(avisos).hasSize(1);
        assertThat(pausas.desde(Canal.COMENTARIO, CLIENTA, POST)).contains(reloj.ahora());
    }

    @Test
    void cadaRespuestaDelAdminEnUnComentarioReiniciaLaPausa() {
        registro.originales.put("c1", new ComentarioOriginal(CLIENTA, POST));

        service.atender(Interaccion.comentario(RedSocial.FACEBOOK, "r1", POST, "c1", PAGINA, "Cuesta $300"));
        reloj.avanzar(Duration.ofMinutes(20));
        service.atender(Interaccion.comentario(RedSocial.FACEBOOK, "r2", POST, "c1", PAGINA, "Sí hay en negro"));

        assertThat(pausas.desde(Canal.COMENTARIO, CLIENTA, POST)).contains(reloj.ahora());
        assertThat(red.enviados).isEmpty();
    }

    @Test
    void laRespuestaDelPropioBotNoCuentaComoDelAdmin() {
        registro.originales.put("c1", new ComentarioOriginal(CLIENTA, POST));
        registro.respuestasDelBot.add("r1");

        service.atender(Interaccion.comentario(RedSocial.FACEBOOK, "r1", POST, "c1", PAGINA, "¡Hola!"));

        assertThat(pausas.guardadas).isEmpty();
    }

    // ---------- Mensajes directos ----------

    @Test
    void unMensajeDirectoSeContestaConElCatalogoPorLaRedQueLlego() {
        cerebro.respuesta = "¡Hola! Sí tenemos bolsas negras 😊";

        service.atender(Interaccion.mensajeDirecto(RedSocial.FACEBOOK, "m1", CLIENTA, "¿Tienen bolsas negras?", false));

        assertThat(cerebro.llamadas).containsExactly("mensaje:facebook:primera");
        assertThat(red.enviados).containsExactly("mensaje:facebook:" + CLIENTA + ":¡Hola! Sí tenemos bolsas negras 😊");
    }

    @Test
    void unaFotoSinTextoSeEscalaSinConsultarAlChatbot() {
        service.atender(Interaccion.mensajeDirecto(RedSocial.INSTAGRAM, "m1", CLIENTA, null, true));

        assertThat(cerebro.llamadas).isEmpty();
        assertThat(red.enviados.get(0)).contains("En un momento te atendemos");
        assertThat(avisos).containsExactly("m1:mandó una foto, audio o sticker");
        assertThat(pausas.desde(Canal.MENSAJE_DIRECTO, CLIENTA, null)).contains(reloj.ahora());
    }

    @Test
    void siNoEntiendeUnMensajeDirectoLoEscala() {
        cerebro.respuesta = "No te entendí ##FAREWELL##";

        service.atender(Interaccion.mensajeDirecto(RedSocial.INSTAGRAM, "m1", CLIENTA, "xyz", false));

        assertThat(avisos).hasSize(1);
        assertThat(pausas.desde(Canal.MENSAJE_DIRECTO, CLIENTA, null)).isPresent();
    }

    @Test
    void siElChatbotFallaSeEscalaEnVezDeDejarAlClienteSinRespuesta() {
        cerebro.falla = true;

        service.atender(Interaccion.mensajeDirecto(RedSocial.INSTAGRAM, "m1", CLIENTA, "Hola", false));

        assertThat(red.enviados).hasSize(1);
        assertThat(avisos).containsExactly("m1:el chatbot no respondió");
    }

    @Test
    void elEcoDeLaRespuestaDelBotNoPausaAunqueLlegueAntesDeGuardarla() {
        cerebro.respuesta = "¡Hola! Sí hay 😊";
        red.alEnviar = () -> service.registrarEcoDeMensaje(RedSocial.INSTAGRAM, "eco-1", CLIENTA);

        service.atender(Interaccion.mensajeDirecto(RedSocial.INSTAGRAM, "m1", CLIENTA, "¿Hay?", false));

        assertThat(pausas.guardadas).isEmpty();
    }

    @Test
    void siElAdminContestaAManoPorMensajeElBotSePausa() {
        service.registrarEcoDeMensaje(RedSocial.INSTAGRAM, "manual-1", CLIENTA);

        assertThat(pausas.desde(Canal.MENSAJE_DIRECTO, CLIENTA, null)).contains(reloj.ahora());
    }

    // ---------- Generales ----------

    @Test
    void soloEnElPrimerMensajeDirectoSePresentaComoAsistenteAutomatico() {
        cerebro.respuesta = "##ESCALAR##";
        service.atender(Interaccion.mensajeDirecto(RedSocial.INSTAGRAM, "m1", CLIENTA, "¿Precio?", false));
        assertThat(red.enviados.get(0)).contains("asistente automático");

        registro.autoresConRespuesta.add(CLIENTA);
        pausas.guardadas.clear();
        red.enviados.clear();
        service.atender(Interaccion.mensajeDirecto(RedSocial.INSTAGRAM, "m2", CLIENTA, "¿Y en rojo?", false));
        assertThat(red.enviados).allSatisfy(t -> assertThat(t).doesNotContain("asistente automático"));
    }

    @Test
    void unEventoRepetidoNoSeContestaDosVeces() {
        registro.procesadas.add("c1");

        service.atender(comentario("c1", "Hola"));

        assertThat(red.enviados).isEmpty();
    }

    @Test
    void nuncaSeContestaASiMismo() {
        service.atender(Interaccion.comentario(RedSocial.FACEBOOK, "p1", POST, null, PAGINA, "Nueva colección"));

        assertThat(red.enviados).isEmpty();
        assertThat(cerebro.llamadas).isEmpty();
    }

    private Interaccion comentario(String id, String texto) {
        return Interaccion.comentario(RedSocial.FACEBOOK, id, POST, null, CLIENTA, texto);
    }

    // ---------- Puertos falsos ----------

    static class RelojManual extends Clock {
        private Instant ahora;

        RelojManual(LocalDateTime inicio) {
            this.ahora = inicio.toInstant(ZoneOffset.UTC);
        }

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        LocalDateTime ahora() {
            return LocalDateTime.ofInstant(ahora, ZoneOffset.UTC);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    static class FakeRegistro implements RegistroInteraccionesPort {
        final Set<String> procesadas = new HashSet<>();
        final Set<String> autoresConRespuesta = new HashSet<>();
        final Set<String> respuestasDelBot = new HashSet<>();
        final Map<String, ComentarioOriginal> originales = new HashMap<>();
        final Map<String, String> guardadas = new HashMap<>();

        @Override
        public boolean yaProcesada(Canal canal, String id) {
            return procesadas.contains(id);
        }

        @Override
        public boolean yaLeHabiamosEscrito(Canal canal, String autorId) {
            return autoresConRespuesta.contains(autorId);
        }

        @Override
        public void guardar(Interaccion i, String respuesta, String idRespuesta) {
            guardadas.put(i.id(), respuesta);
            procesadas.add(i.id());
            if (idRespuesta != null) {
                respuestasDelBot.add(idRespuesta);
            }
        }

        @Override
        public boolean esRespuestaDelBot(Canal canal, String id) {
            return respuestasDelBot.contains(id);
        }

        @Override
        public Optional<ComentarioOriginal> comentarioOriginal(String commentId) {
            return Optional.ofNullable(originales.get(commentId));
        }
    }

    static class FakePausas implements PausasPort {
        final Map<String, LocalDateTime> guardadas = new HashMap<>();

        @Override
        public Optional<LocalDateTime> desde(Canal canal, String autorId, String publicacionId) {
            return Optional.ofNullable(guardadas.get(canal + ":" + autorId + ":" + publicacionId));
        }

        @Override
        public void pausar(Canal canal, String autorId, String publicacionId, LocalDateTime cuando) {
            guardadas.put(canal + ":" + autorId + ":" + publicacionId, cuando);
        }
    }

    static class FakeCerebro implements CerebroPort {
        final List<String> llamadas = new ArrayList<>();
        String respuesta = "¡Hola! 😊";
        boolean falla;

        private String responder(String llamada) {
            llamadas.add(llamada);
            if (falla) {
                throw new IllegalStateException("OpenAI no contestó");
            }
            return respuesta;
        }

        @Override
        public String responderSobreProducto(RedSocial red, String texto, Integer varianteId, boolean esPrimeraVez) {
            return responder("producto:" + varianteId + ":" + red.codigo() + ":" + (esPrimeraVez ? "primera" : "no-primera"));
        }

        @Override
        public String responderComentarioSinProducto(RedSocial red, String texto, boolean esPrimeraVez) {
            return responder("sinProducto:" + red.codigo() + ":" + (esPrimeraVez ? "primera" : "no-primera"));
        }

        @Override
        public String responderMensajeDirecto(RedSocial red, String texto, boolean esPrimeraVez) {
            return responder("mensaje:" + red.codigo() + ":" + (esPrimeraVez ? "primera" : "no-primera"));
        }
    }

    static class FakeRed implements RespuestaEnRedPort {
        final List<String> enviados = new ArrayList<>();
        Runnable alEnviar = () -> { };

        @Override
        public String responderComentario(RedSocial red, String commentId, String texto) {
            enviados.add("comentario:" + red.codigo() + ":" + commentId + ":" + texto);
            alEnviar.run();
            return "resp-" + enviados.size();
        }

        @Override
        public String enviarMensaje(RedSocial red, String destinatarioId, String texto) {
            enviados.add("mensaje:" + red.codigo() + ":" + destinatarioId + ":" + texto);
            alEnviar.run();
            return "resp-" + enviados.size();
        }
    }

    static class SinAbuso implements ControlDeAbusoPort {
        @Override
        public boolean bloqueado(String clave) {
            return false;
        }

        @Override
        public boolean limiteExcedido(String clave) {
            return false;
        }

        @Override
        public void registrarMensaje(String clave) {
        }

        @Override
        public void registrarNoEntendido(String clave) {
        }

        @Override
        public void registrarEntendido(String clave) {
        }
    }
}
