package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.pasarela;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CobroEnPasarela;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoExterno;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PasarelaDePagoPort;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [Hexagonal: Driven Adapter] [Clean: Interface Adapter]
 *
 * <p>Pasarela FALSA para probar en QA sin mover dinero. Solo existe en los perfiles {@code dev} y {@code qa}
 * Y con {@code pagos.tarjeta.pasarela=simulada}: en produccion no se puede activar con una sola variable.
 *
 * <p>El resultado depende de los centavos del total, para provocarlo desde la pantalla:
 * <ul>
 *   <li>{@code .01} → rechazada;</li>
 *   <li>{@code .02} → nunca contesta (prueba la expiracion);</li>
 *   <li>{@code .03} → cancelada en la terminal;</li>
 *   <li>cualquier otro → aprobada a los ~4 s ({@code pagos.tarjeta.simulada.segundos}).</li>
 * </ul>
 * Un cobro aprobado no se puede cancelar, y las devoluciones son idempotentes por clave, como en la real.
 * El estado vive en memoria: se pierde al reiniciar el pod.
 */
@Component
@Profile({"dev", "qa"})
@ConditionalOnProperty(name = "pagos.tarjeta.pasarela", havingValue = "simulada")
public class PasarelaSimuladaAdapter implements PasarelaDePagoPort {

    private static final Logger log = LoggerFactory.getLogger(PasarelaSimuladaAdapter.class);

    private final Clock reloj;
    private final Duration demora;
    private final Map<String, Cobro> cobros = new ConcurrentHashMap<>();
    private final Map<Long, String> porReferencia = new ConcurrentHashMap<>();
    private final Set<String> clavesDevolucion = ConcurrentHashMap.newKeySet();

    @Autowired
    public PasarelaSimuladaAdapter(Clock reloj, @Value("${pagos.tarjeta.simulada.segundos:4}") long segundos) {
        this(reloj, Duration.ofSeconds(segundos));
    }

    PasarelaSimuladaAdapter(Clock reloj, Duration demora) {
        this.reloj = reloj;
        this.demora = demora;
    }

    @PostConstruct
    void avisar() {
        log.warn("⚠️ PASARELA SIMULADA ACTIVA: los cobros con tarjeta son de mentira, no se mueve dinero");
    }

    @Override
    public String crearCobro(IntentoDePago intento) {
        String id = "sim-" + intento.id();
        cobros.putIfAbsent(id, new Cobro(resultadoPor(intento.monto()), reloj.instant()));
        porReferencia.put(intento.id(), id);
        return id;
    }

    @Override
    public EstadoExterno consultar(String idExterno) {
        Cobro c = cobros.get(idExterno);
        if (c == null) {
            // Tras reiniciar el pod la memoria se pierde. Como la real ante un cobro que no conoce: ya no se puede
            // cobrar, asi el pedido no queda bloqueado para siempre.
            return EstadoExterno.CANCELADO;
        }
        return c.estadoActual(reloj.instant(), demora);
    }

    @Override
    public Optional<CobroEnPasarela> buscarPorReferencia(Long intentoId) {
        String id = porReferencia.get(intentoId);
        return id == null ? Optional.empty() : Optional.of(new CobroEnPasarela(id, consultar(id)));
    }

    @Override
    public void cancelar(String idExterno) {
        Cobro c = cobro(idExterno);
        if (c.estadoActual(reloj.instant(), demora) == EstadoExterno.APROBADO) {
            throw new IllegalStateException("el cobro simulado ya está aprobado y no se puede cancelar");
        }
        c.cancelado = true;
    }

    @Override
    public void devolver(String idExterno, BigDecimal monto, String claveIdempotencia) {
        cobro(idExterno);
        if (clavesDevolucion.add(claveIdempotencia)) {
            log.info("[SIMULADA] devolución de {} del cobro {} (clave {})", monto, idExterno, claveIdempotencia);
        }
    }

    private Cobro cobro(String idExterno) {
        Cobro c = cobros.get(idExterno);
        if (c == null) {
            throw new IllegalStateException("cobro simulado desconocido: " + idExterno);
        }
        return c;
    }

    /** Por los centavos del total. */
    static EstadoExterno resultadoPor(BigDecimal monto) {
        int centavos = monto.movePointRight(2).abs().remainder(BigDecimal.valueOf(100)).intValue();
        return switch (centavos) {
            case 1 -> EstadoExterno.RECHAZADO;
            case 2 -> EstadoExterno.PENDIENTE;          // nunca contesta
            case 3 -> EstadoExterno.CANCELADO;
            default -> EstadoExterno.APROBADO;
        };
    }

    private static final class Cobro {
        private final EstadoExterno destino;
        private final Instant creado;
        private volatile boolean cancelado;

        Cobro(EstadoExterno destino, Instant creado) {
            this.destino = destino;
            this.creado = creado;
        }

        EstadoExterno estadoActual(Instant ahora, Duration demora) {
            if (cancelado) {
                return EstadoExterno.CANCELADO;         // solo se cancela si aun no estaba aprobado
            }
            if (destino == EstadoExterno.PENDIENTE || Duration.between(creado, ahora).compareTo(demora) < 0) {
                return EstadoExterno.PENDIENTE;
            }
            return destino;
        }
    }
}
