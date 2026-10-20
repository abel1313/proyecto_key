package com.ventas.key.hexagonal.iarespaldo.dominio.modelo;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recuerda qué proveedor está "descansando" para no esperarlo en cada mensaje: si OpenAI se quedó sin
 * saldo, los siguientes mensajes van directo al respaldo en lugar de esperar un error cada vez.
 * Seguro entre hilos; el reloj se pasa de fuera para poder probarlo.
 */
public final class CircuitoDeProveedores {

    private final Map<String, Instant> descansaHasta = new ConcurrentHashMap<>();

    public boolean disponible(ProveedorIa proveedor, Instant ahora) {
        Instant hasta = descansaHasta.get(proveedor.nombre());
        return hasta == null || !ahora.isBefore(hasta);
    }

    /**
     * Anota el fallo. Devuelve {@code true} si es la primera vez que este proveedor entra a descansar
     * (para avisar una sola vez y no por cada mensaje).
     */
    public boolean registrarFallo(ProveedorIa proveedor, TipoDeFallo fallo, Duration pausaLarga, Instant ahora) {
        Duration pausa = fallo.pausa(pausaLarga);
        if (pausa.isZero()) {
            return false;
        }
        boolean eraDisponible = disponible(proveedor, ahora);
        descansaHasta.put(proveedor.nombre(), ahora.plus(pausa));
        return eraDisponible;
    }

    /** Para las pruebas: olvida todo lo anotado. */
    public void reiniciar() {
        descansaHasta.clear();
    }

    /** Si contestó bien, deja de descansar. */
    public void registrarExito(ProveedorIa proveedor) {
        descansaHasta.remove(proveedor.nombre());
    }
}
