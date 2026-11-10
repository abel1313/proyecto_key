package com.ventas.key.hexagonal.escanercodigo.dominio.modelo;

import com.ventas.key.hexagonal.escanercodigo.dominio.excepcion.EscanerException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * La conexión entre una PC que pide un código de barras y el celular que lo escanea.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities] Java puro, sin Spring.
 *
 * <p>Una conexión es de TRABAJO: se enlaza una vez (QR) y el celular se queda abierto y escaneando; la PC pide un código cada vez que
 * hace falta, sin QR de por medio.
 *
 * <p>Reglas (cada una con su prueba):
 * <ul>
 *   <li>E1: la conexión dura como máximo 12 horas.</li>
 *   <li>E2: el código se limpia (espacios de los lados) y debe tener de 3 a 100 caracteres imprimibles, sin saltos de línea.</li>
 *   <li>E3: se guardan solo los últimos 20 códigos; la PC pide los que sean mayores al último que ya vio.</li>
 *   <li>E4: máximo 30 códigos por minuto en una conexión (un celular que escanea sin parar no la inunda).</li>
 *   <li>E5: cuando la PC la cierra, deja de aceptar códigos y avisa a quien esté esperando.</li>
 *   <li>E7: si en 5 minutos no hay ninguna actividad (ni la PC pregunta, ni el celular manda señal o códigos) la conexión se libera
 *       sola: así nadie llena el cupo con conexiones que nadie usa.</li>
 *   <li>E8: el celular cuenta como «conectado» si mandó señal de vida en los últimos 45 segundos.</li>
 *   <li>E10: máximo 3 preguntas abiertas a la vez por conexión (la más vieja se contesta vacía).</li>
 *   <li>E11: al mandar un código se sabe si la PC lo estaba esperando (pregunta abierta, o una hace menos de 3 s); si no, el celular avisa
 *       «la computadora no lo está pidiendo» en lugar de decir «enviado».</li>
 *   <li>E13: FOTOS (solo en el canal de la cuenta): JPEG en base64 de hasta 700 000 caracteres (~500 KB), máximo 10 por minuto; se guardan las últimas 3 y
 *       duran 5 minutos (nadie las recibe después). Una foto solo se entrega a una pregunta que diga que sabe recibir fotos ({@link Acepta}).</li>
 *   <li>E14 (2026-11-10): un código con forma de UUID (el QR del cliente de una campaña) solo se entrega a una pregunta cuyo {@link Acepta} diga
 *       {@code uuid} (Venta directa con el campo «Código del cliente» abierto). Si ninguna lo dice, no se entrega y el envío responde
 *       {@code false} («la computadora no lo está pidiendo»). Los demás códigos van como siempre a las preguntas que dicen {@code codigo}.</li>
 *   <li>E9: la PC puede dejar una pregunta abierta y se le contesta en cuanto llega un código (sin esperar a la siguiente consulta),
 *       o en cuanto el celular se conecta (para que la PC cambie de «QR» a «escanea ahora»).</li>
 * </ul>
 * Es la única pieza con estado: se protege con {@code synchronized} porque el celular escribe y la PC lee a la vez. Las preguntas
 * abiertas se completan FUERA del candado, para no ejecutar código ajeno con la sesión bloqueada.
 */
public final class SesionEscaner {

    public static final Duration VIGENCIA_MAXIMA = Duration.ofHours(12);
    public static final Duration SIN_ACTIVIDAD = Duration.ofMinutes(5);
    public static final Duration CELULAR_CONECTADO = Duration.ofSeconds(45);
    /** E10: lo más que puede tener abiertas la PC a la vez; con una más, la más vieja se contesta vacía (no se acumulan preguntas huérfanas). */
    public static final int ESPERAS_MAXIMAS = 3;
    /** Entre una pregunta abierta y la siguiente hay unos milisegundos sin ninguna: ese margen todavía cuenta como «la PC está pidiendo». */
    public static final Duration MARGEN_ENTRE_PREGUNTAS = Duration.ofSeconds(3);
    public static final int CODIGOS_GUARDADOS = 20;
    public static final int CODIGOS_POR_MINUTO = 30;
    public static final int FOTOS_GUARDADAS = 3;
    public static final int FOTOS_POR_MINUTO = 10;
    public static final int FOTO_MAXIMA_CARACTERES = 700_000;
    public static final Duration FOTO_VIGENCIA = Duration.ofMinutes(5);
    private static final String ENCABEZADO_FOTO = "data:image/jpeg;base64,";
    private static final Pattern BASE64 = Pattern.compile("^[A-Za-z0-9+/]+={0,2}$");
    private static final Pattern CODIGO_VALIDO = Pattern.compile("^[\\x20-\\x7E]{3,100}$");
    /** El mismo UUID exacto que reconoce el front (escaner-receptor.service.ts). */
    private static final Pattern UUID_EXACTO = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /** ¿Es un código de cliente de campaña (UUID)? (E14) */
    public static boolean esUuid(String codigo) {
        return codigo != null && UUID_EXACTO.matcher(codigo.trim()).matches();
    }

    /** Una pregunta abierta de la PC. */
    private record Espera(long desde, Acepta acepta, CompletableFuture<LecturaDeCodigos> futuro) {
    }

    /** Una foto guardada con su hora, para que caduque. */
    private record FotoGuardada(FotoRecibida foto, Instant creada) {
    }

    private final String id;
    private final String origen;
    private final Instant venceEn;
    private Instant ultimaActividad;
    private Instant ultimoLatido;
    private Instant ultimaPreguntaConCodigos;
    private Instant ultimaPreguntaConFotos;
    private Instant ultimaPreguntaConUuid;
    private final Deque<CodigoEscaneado> codigos = new ArrayDeque<>();
    private final Deque<Instant> recientes = new ArrayDeque<>();
    private final Deque<Instant> fotosRecientes = new ArrayDeque<>();
    private final Deque<FotoGuardada> fotos = new ArrayDeque<>();
    private final List<Espera> esperando = new ArrayList<>();
    private long secuencia;
    private boolean cerrada;

    public SesionEscaner(String id, String origen, Instant creadaEn) {
        this.id = id;
        this.origen = origen;
        this.venceEn = creadaEn.plus(VIGENCIA_MAXIMA);
        this.ultimaActividad = creadaEn;
    }

    public String id() { return id; }

    /** Cambia cuando la conexión se rehace (otra conexión, otro id); la PC la usa para saber que debe pedir desde el principio. */
    public int epoca() { return id.hashCode(); }

    /** De dónde se abrió (la IP o el usuario de la PC), solo para limitar cuántas conexiones abiertas tiene cada uno. */
    public String origen() { return origen; }

    public Instant venceEn() { return venceEn; }

    public synchronized boolean estaVigente(Instant ahora) {
        return !cerrada && ahora.isBefore(venceEn) && ahora.isBefore(ultimaActividad.plus(SIN_ACTIVIDAD));
    }

    public void cerrar() {
        List<Espera> aAvisar;
        synchronized (this) {
            cerrada = true;
            aAvisar = new ArrayList<>(esperando);
            esperando.clear();
        }
        aAvisar.forEach(e -> e.futuro().completeExceptionally(new EscanerException.SesionNoDisponible()));
    }

    /** Lo que pasó al mandar un código: el código guardado y si la PC lo estaba esperando. */
    public record Envio(CodigoEscaneado codigo, boolean laPcLoEsperaba) {
    }

    /** El celular manda un código. */
    public CodigoEscaneado registrar(String crudo, Instant ahora) {
        return enviar(crudo, ahora).codigo();
    }

    /** El celular manda un código y se sabe si alguien lo esperaba (E11). */
    public Envio enviar(String crudo, Instant ahora) {
        CodigoEscaneado nuevo;
        boolean esperaba;
        List<Espera> aAvisar = new ArrayList<>();
        List<LecturaDeCodigos> lecturas = new ArrayList<>();
        synchronized (this) {
            exigirVigente(ahora);
            String limpio = crudo == null ? "" : crudo.trim();
            if (!CODIGO_VALIDO.matcher(limpio).matches()) {
                throw new EscanerException.CodigoInvalido();
            }
            while (!recientes.isEmpty() && recientes.peekFirst().isBefore(ahora.minusSeconds(60))) {
                recientes.removeFirst();
            }
            if (recientes.size() >= CODIGOS_POR_MINUTO) {
                throw new EscanerException.DemasiadasSolicitudes("Se están mandando códigos demasiado rápido. Espera un momento.");
            }
            recientes.addLast(ahora);
            ultimaActividad = ahora;
            ultimoLatido = ahora;
            nuevo = new CodigoEscaneado(++secuencia, limpio);
            codigos.addLast(nuevo);
            while (codigos.size() > CODIGOS_GUARDADOS) {
                codigos.removeFirst();
            }
            for (Iterator<Espera> it = esperando.iterator(); it.hasNext(); ) {
                Espera e = it.next();
                if (hayNuevo(e.desde(), e.acepta(), ahora)) {
                    aAvisar.add(e);
                    lecturas.add(lecturaDespuesDe(e.desde(), e.acepta(), ahora));
                    it.remove();
                }
            }
            // E14: un UUID solo cuenta como esperado si alguna pregunta (abierta o de hace un momento) dijo «uuid».
            esperaba = !aAvisar.isEmpty() || dentroDelMargen(esUuid(limpio) ? ultimaPreguntaConUuid : ultimaPreguntaConCodigos, ahora);
        }
        for (int i = 0; i < aAvisar.size(); i++) {
            aAvisar.get(i).futuro().complete(lecturas.get(i));
        }
        return new Envio(nuevo, esperaba);
    }

    /** La PC pregunta qué códigos llegaron después del {@code desde} que ya vio (0 la primera vez), sin esperar. */
    public synchronized LecturaDeCodigos leer(long desde, Instant ahora) {
        return leer(desde, ahora, Acepta.SOLO_CODIGOS);
    }

    /** Igual, respetando lo que la pantalla sabe recibir (así una respuesta por tiempo agotado no adelanta el contador sin las fotos). */
    public synchronized LecturaDeCodigos leer(long desde, Instant ahora, Acepta acepta) {
        exigirVigente(ahora);
        ultimaActividad = ahora;
        return lecturaDespuesDe(normalizar(desde), acepta, ahora);
    }

    /**
     * La PC deja la pregunta abierta. Si ya hay un código más nuevo que {@code desde} se contesta al instante; si no, se contesta en
     * cuanto el celular mande uno. Preguntar cuenta como actividad (E7).
     */
    public EsperaDeCodigos esperar(long desdePedido, Instant ahora) {
        return esperar(desdePedido, ahora, Acepta.SOLO_CODIGOS);
    }

    /** Igual, diciendo qué sabe recibir la pantalla de la PC (códigos, fotos o las dos). */
    public EsperaDeCodigos esperar(long desdePedido, Instant ahora, Acepta acepta) {
        CompletableFuture<LecturaDeCodigos> futuro = new CompletableFuture<>();
        long desde;
        synchronized (this) {
            desde = normalizar(desdePedido);
        }
        Espera espera = new Espera(desde, acepta, futuro);
        LecturaDeCodigos yaHay = null;
        Espera desplazada = null;
        LecturaDeCodigos lecturaDesplazada = null;
        synchronized (this) {
            exigirVigente(ahora);
            ultimaActividad = ahora;
            if (acepta.codigos()) ultimaPreguntaConCodigos = ahora;
            if (acepta.fotos()) ultimaPreguntaConFotos = ahora;
            if (acepta.uuid()) ultimaPreguntaConUuid = ahora;
            if (hayNuevo(desde, acepta, ahora)) {
                yaHay = lecturaDespuesDe(desde, acepta, ahora);
            } else {
                if (esperando.size() >= ESPERAS_MAXIMAS) {
                    desplazada = esperando.remove(0);                       // E10: la más vieja se libera
                    lecturaDesplazada = lecturaDespuesDe(desplazada.desde(), desplazada.acepta(), ahora);
                }
                esperando.add(espera);
            }
        }
        if (desplazada != null) {
            desplazada.futuro().complete(lecturaDesplazada);
        }
        if (yaHay != null) {
            futuro.complete(yaHay);
        }
        return new EsperaDeCodigos(futuro, () -> {
            synchronized (this) {
                esperando.remove(espera);
            }
        });
    }

    /**
     * El celular avisa que sigue ahí. Si hasta ahora no estaba conectado (primer latido, o volvió después de desconectarse), a quien
     * esté esperando se le contesta de inmediato «sin códigos, pero el celular ya está conectado»: así la PC pasa sola del QR a «escanea
     * ahora» sin esperar a que se acabe su pregunta.
     */
    public void latido(Instant ahora) {
        List<Espera> aAvisar = new ArrayList<>();
        List<LecturaDeCodigos> lecturas = new ArrayList<>();
        synchronized (this) {
            exigirVigente(ahora);
            boolean yaEstabaConectado = celularConectado(ahora);
            ultimoLatido = ahora;
            ultimaActividad = ahora;
            if (!yaEstabaConectado) {
                for (Espera e : esperando) {
                    aAvisar.add(e);
                    lecturas.add(lecturaDespuesDe(e.desde(), e.acepta(), ahora));
                }
                esperando.clear();
            }
        }
        for (int i = 0; i < aAvisar.size(); i++) {
            aAvisar.get(i).futuro().complete(lecturas.get(i));
        }
    }

    public synchronized EstadoDeSesion estado(Instant ahora) {
        return new EstadoDeSesion(estaVigente(ahora), celularConectado(ahora), secuencia, venceEn, epoca());
    }

    /**
     * Si la PC pide «después del N» y esta conexión va en un número menor (la anterior venció y se abrió otra de la misma cuenta, que
     * empieza de nuevo), el N ya no sirve: se pide desde el principio de la nueva, o se saltaría sus primeros códigos.
     */
    private long normalizar(long desde) {
        return desde > secuencia ? 0 : desde;
    }

    private boolean celularConectado(Instant ahora) {
        return ultimoLatido != null && ahora.isBefore(ultimoLatido.plus(CELULAR_CONECTADO));
    }

    /** ¿Hay algo más nuevo que {@code desde} de lo que esa pantalla sabe recibir? (una foto no despierta a quien solo recibe códigos, ni al revés). */
    private boolean hayNuevo(long desde, Acepta acepta, Instant ahora) {
        if (codigos.stream().anyMatch(c -> c.secuencia() > desde && acepta.recibe(c))) {
            return true;
        }
        return acepta.fotos() && fotos.stream().anyMatch(f -> f.foto().secuencia() > desde && vigente(f, ahora));
    }

    private LecturaDeCodigos lecturaDespuesDe(long desde, Acepta acepta, Instant ahora) {
        List<CodigoEscaneado> nuevos = new ArrayList<>();
        for (CodigoEscaneado c : codigos) {
            if (c.secuencia() > desde && acepta.recibe(c)) {   // E14: un UUID solo a quien dice «uuid»
                nuevos.add(c);
            }
        }
        List<FotoRecibida> nuevasFotos = new ArrayList<>();
        if (acepta.fotos()) {
            for (FotoGuardada f : fotos) {
                if (f.foto().secuencia() > desde && vigente(f, ahora)) {
                    nuevasFotos.add(f.foto());
                }
            }
        }
        return new LecturaDeCodigos(secuencia, nuevos, celularConectado(ahora), epoca(), nuevasFotos);
    }

    private static boolean vigente(FotoGuardada f, Instant ahora) {
        return ahora.isBefore(f.creada().plus(FOTO_VIGENCIA));
    }

    private static boolean dentroDelMargen(Instant ultima, Instant ahora) {
        return ultima != null && ahora.isBefore(ultima.plus(MARGEN_ENTRE_PREGUNTAS));
    }

    /**
     * El celular manda una FOTO (solo el canal de la cuenta). Se valida que sea un JPEG en base64 de tamaño razonable (E13); se guardan las últimas 3 y
     * se avisa a la pregunta abierta que sepa recibir fotos. Devuelve si la PC la estaba esperando (si no, el celular avisa «esa pantalla no recibe fotos»).
     */
    public boolean enviarFoto(String contenido, Instant ahora) {
        FotoRecibida nueva;
        List<Espera> aAvisar = new ArrayList<>();
        List<LecturaDeCodigos> lecturas = new ArrayList<>();
        boolean esperaba;
        synchronized (this) {
            exigirVigente(ahora);
            while (!fotosRecientes.isEmpty() && fotosRecientes.peekFirst().isBefore(ahora.minusSeconds(60))) {
                fotosRecientes.removeFirst();
            }
            if (fotosRecientes.size() >= FOTOS_POR_MINUTO) {
                throw new EscanerException.DemasiadasSolicitudes("Se están mandando fotos demasiado rápido. Espera un momento.");
            }
            fotosRecientes.addLast(ahora);          // el intento cuenta aunque la foto resulte inválida (no se puede abusar con envíos rechazados)
            if (!fotoValida(contenido)) {
                throw new EscanerException.FotoInvalida();
            }
            fotos.removeIf(f -> !vigente(f, ahora));   // las vencidas se liberan (no esperan a que alguien lea)
            ultimaActividad = ahora;
            ultimoLatido = ahora;
            nueva = new FotoRecibida(++secuencia, contenido);
            fotos.addLast(new FotoGuardada(nueva, ahora));
            while (fotos.size() > FOTOS_GUARDADAS) {
                fotos.removeFirst();
            }
            for (Iterator<Espera> it = esperando.iterator(); it.hasNext(); ) {
                Espera e = it.next();
                if (e.acepta().fotos() && hayNuevo(e.desde(), e.acepta(), ahora)) {
                    aAvisar.add(e);
                    lecturas.add(lecturaDespuesDe(e.desde(), e.acepta(), ahora));
                    it.remove();
                }
            }
            esperaba = !aAvisar.isEmpty() || dentroDelMargen(ultimaPreguntaConFotos, ahora);
        }
        for (int i = 0; i < aAvisar.size(); i++) {
            aAvisar.get(i).futuro().complete(lecturas.get(i));
        }
        return esperaba;
    }

    private static boolean fotoValida(String contenido) {
        if (contenido == null || contenido.length() > FOTO_MAXIMA_CARACTERES || !contenido.startsWith(ENCABEZADO_FOTO)) {
            return false;
        }
        String datos = contenido.substring(ENCABEZADO_FOTO.length());
        return datos.length() >= 100 && BASE64.matcher(datos).matches();
    }

    private void exigirVigente(Instant ahora) {
        if (!estaVigente(ahora)) {
            throw new EscanerException.SesionNoDisponible();
        }
    }
}
