package com.ventas.key.hexagonal.escanercodigo.dominio.modelo;

import com.ventas.key.hexagonal.escanercodigo.dominio.excepcion.EscanerException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

/**
 * La conexión entre una PC que pide un código de barras y el celular que lo escanea.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities] Java puro, sin Spring.
 *
 * <p>Reglas (cada una con su prueba):
 * <ul>
 *   <li>E1: la conexión dura 30 minutos y listo; después ya no recibe ni entrega códigos.</li>
 *   <li>E2: el código se limpia (espacios de los lados) y debe tener de 3 a 100 caracteres imprimibles, sin saltos de línea.</li>
 *   <li>E3: se guardan solo los últimos 20 códigos; la PC pide los que sean mayores al último que ya vio.</li>
 *   <li>E4: máximo 30 códigos por minuto en una conexión (un celular que escanea sin parar no la inunda).</li>
 *   <li>E5: cuando la PC la cierra, deja de aceptar códigos.</li>
 *   <li>E7: si la PC no pregunta en 2 minutos (cerró o recargó la pestaña) la conexión se libera sola: así nadie puede
 *       llenar el cupo con conexiones que nadie consulta.</li>
 * </ul>
 * Es la única pieza con estado: los métodos son {@code synchronized} porque el celular escribe y la PC lee a la vez.
 */
public final class SesionEscaner {

    public static final Duration VIGENCIA = Duration.ofMinutes(30);
    /** E7: si la PC deja de preguntar (cerró o recargó la pestaña) la conexión se libera sola a los 2 minutos. */
    public static final Duration SIN_CONSULTAR = Duration.ofMinutes(2);
    public static final int CODIGOS_GUARDADOS = 20;
    public static final int CODIGOS_POR_MINUTO = 30;
    private static final Pattern CODIGO_VALIDO = Pattern.compile("^[\\x20-\\x7E]{3,100}$");

    private final String id;
    private final String origen;
    private final Instant venceEn;
    private Instant ultimaConsulta;
    private final Deque<CodigoEscaneado> codigos = new ArrayDeque<>();
    private final Deque<Instant> recientes = new ArrayDeque<>();
    private long secuencia;
    private boolean cerrada;

    public SesionEscaner(String id, String origen, Instant creadaEn) {
        this.id = id;
        this.origen = origen;
        this.venceEn = creadaEn.plus(VIGENCIA);
        this.ultimaConsulta = creadaEn;
    }

    public String id() { return id; }

    /** De dónde se abrió (la IP de la PC), solo para limitar cuántas conexiones abiertas tiene cada una. */
    public String origen() { return origen; }

    public Instant venceEn() { return venceEn; }

    public synchronized boolean estaVigente(Instant ahora) {
        return !cerrada && ahora.isBefore(venceEn) && ahora.isBefore(ultimaConsulta.plus(SIN_CONSULTAR));
    }

    public synchronized void cerrar() {
        cerrada = true;
    }

    /** El celular manda un código. */
    public synchronized CodigoEscaneado registrar(String crudo, Instant ahora) {
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
        CodigoEscaneado nuevo = new CodigoEscaneado(++secuencia, limpio);
        codigos.addLast(nuevo);
        while (codigos.size() > CODIGOS_GUARDADOS) {
            codigos.removeFirst();
        }
        return nuevo;
    }

    /**
     * La PC pregunta qué códigos llegaron después del {@code desde} que ya vio (0 la primera vez). La lista y la secuencia salen
     * bajo el mismo candado: si el celular escribe justo en medio, la PC no se salta ningún código. Preguntar cuenta como «sigue ahí» (E7).
     */
    public synchronized LecturaDeCodigos leer(long desde, Instant ahora) {
        exigirVigente(ahora);
        ultimaConsulta = ahora;
        List<CodigoEscaneado> nuevos = new ArrayList<>();
        for (CodigoEscaneado c : codigos) {
            if (c.secuencia() > desde) {
                nuevos.add(c);
            }
        }
        return new LecturaDeCodigos(secuencia, nuevos);
    }

    private void exigirVigente(Instant ahora) {
        if (!estaVigente(ahora)) {
            throw new EscanerException.SesionNoDisponible();
        }
    }
}
