package com.ventas.key.hexagonal.escanercodigo.infraestructura.salida.memoria;

import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.SesionEscaner;
import com.ventas.key.hexagonal.escanercodigo.dominio.puerto.salida.SesionesEscanerPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 *
 * <p>Las conexiones viven en la memoria del servidor: duran 30 minutos y no hace falta guardarlas. Funciona con UNA copia de
 * la aplicación (igual que el chat en vivo); con varias habría que guardarlas en Redis.
 */
@Component
public class SesionesEscanerEnMemoria implements SesionesEscanerPort {

    private final ConcurrentMap<String, SesionEscaner> porId = new ConcurrentHashMap<>();
    private final Clock reloj;

    public SesionesEscanerEnMemoria(Clock reloj) {
        this.reloj = reloj;
    }

    @Override
    public void guardar(SesionEscaner sesion) {
        porId.put(sesion.id(), sesion);
    }

    @Override
    public Optional<SesionEscaner> buscar(String id) {
        return Optional.ofNullable(porId.get(id));
    }

    @Override
    public void eliminar(String id) {
        porId.remove(id);
    }

    @Override
    public int vigentes(Instant ahora) {
        return (int) porId.values().stream().filter(s -> s.estaVigente(ahora)).count();
    }

    @Override
    public int vigentesAnonimas(Instant ahora, String prefijoDeUsuario) {
        return (int) porId.values().stream().filter(s -> !s.origen().startsWith(prefijoDeUsuario) && s.estaVigente(ahora)).count();
    }

    @Override
    public int vigentesDe(String origen, Instant ahora) {
        return (int) porId.values().stream().filter(s -> s.origen().equals(origen) && s.estaVigente(ahora)).count();
    }

    @Override
    public int limpiar(Instant ahora) {
        int antes = porId.size();
        porId.values().removeIf(s -> !s.estaVigente(ahora));
        return antes - porId.size();
    }

    @Scheduled(fixedDelay = 300_000)
    void limpiarVencidas() {
        limpiar(reloj.instant());
    }
}
