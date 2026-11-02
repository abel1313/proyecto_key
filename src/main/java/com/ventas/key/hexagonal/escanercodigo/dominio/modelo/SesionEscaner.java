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
    private static final Pattern CODIGO_VALIDO = Pattern.compile("^[\\x20-\\x7E]{3,100}$");

    /** Una pregunta abierta de la PC. */
    private record Espera(long desde, CompletableFuture<LecturaDeCodigos> futuro) {
    }

    private final String id;
    private final String origen;
    private final Instant venceEn;
    private Instant ultimaActividad;
    private Instant ultimoLatido;
    private Instant ultimaPregunta;
    private final Deque<CodigoEscaneado> codigos = new ArrayDeque<>();
    private final Deque<Instant> recientes = new ArrayDeque<>();
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
                if (secuencia > e.desde()) {
                    aAvisar.add(e);
                    lecturas.add(lecturaDespuesDe(e.desde(), ahora));
                    it.remove();
                }
            }
            esperaba = !aAvisar.isEmpty() || (ultimaPregunta != null && ahora.isBefore(ultimaPregunta.plus(MARGEN_ENTRE_PREGUNTAS)));
        }
        for (int i = 0; i < aAvisar.size(); i++) {
            aAvisar.get(i).futuro().complete(lecturas.get(i));
        }
        return new Envio(nuevo, esperaba);
    }

    /** La PC pregunta qué códigos llegaron después del {@code desde} que ya vio (0 la primera vez), sin esperar. */
    public synchronized LecturaDeCodigos leer(long desde, Instant ahora) {
        exigirVigente(ahora);
        ultimaActividad = ahora;
        return lecturaDespuesDe(desde, ahora);
    }

    /**
     * La PC deja la pregunta abierta. Si ya hay un código más nuevo que {@code desde} se contesta al instante; si no, se contesta en
     * cuanto el celular mande uno. Preguntar cuenta como actividad (E7).
     */
    public EsperaDeCodigos esperar(long desde, Instant ahora) {
        CompletableFuture<LecturaDeCodigos> futuro = new CompletableFuture<>();
        Espera espera = new Espera(desde, futuro);
        LecturaDeCodigos yaHay = null;
        Espera desplazada = null;
        LecturaDeCodigos lecturaDesplazada = null;
        synchronized (this) {
            exigirVigente(ahora);
            ultimaActividad = ahora;
            ultimaPregunta = ahora;
            if (secuencia > desde) {
                yaHay = lecturaDespuesDe(desde, ahora);
            } else {
                if (esperando.size() >= ESPERAS_MAXIMAS) {
                    desplazada = esperando.remove(0);                       // E10: la más vieja se libera
                    lecturaDesplazada = lecturaDespuesDe(desplazada.desde(), ahora);
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
                    lecturas.add(lecturaDespuesDe(e.desde(), ahora));
                }
                esperando.clear();
            }
        }
        for (int i = 0; i < aAvisar.size(); i++) {
            aAvisar.get(i).futuro().complete(lecturas.get(i));
        }
    }

    public synchronized EstadoDeSesion estado(Instant ahora) {
        return new EstadoDeSesion(estaVigente(ahora), celularConectado(ahora), secuencia, venceEn);
    }

    private boolean celularConectado(Instant ahora) {
        return ultimoLatido != null && ahora.isBefore(ultimoLatido.plus(CELULAR_CONECTADO));
    }

    private LecturaDeCodigos lecturaDespuesDe(long desde, Instant ahora) {
        List<CodigoEscaneado> nuevos = new ArrayList<>();
        for (CodigoEscaneado c : codigos) {
            if (c.secuencia() > desde) {
                nuevos.add(c);
            }
        }
        return new LecturaDeCodigos(secuencia, nuevos, celularConectado(ahora));
    }

    private void exigirVigente(Instant ahora) {
        if (!estaVigente(ahora)) {
            throw new EscanerException.SesionNoDisponible();
        }
    }
}
