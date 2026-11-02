package com.ventas.key.hexagonal.escanercodigo.aplicacion.servicio;

import com.ventas.key.hexagonal.escanercodigo.dominio.excepcion.EscanerException;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.LecturaDeCodigos;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.SesionAbierta;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.SesionEscaner;
import com.ventas.key.hexagonal.escanercodigo.dominio.puerto.salida.SesionesEscanerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EscanerRemotoServiceTest {

    /** Port falso en memoria. */
    static class Falso implements SesionesEscanerPort {
        final Map<String, SesionEscaner> mapa = new HashMap<>();
        @Override public void guardar(SesionEscaner s) { mapa.put(s.id(), s); }
        @Override public Optional<SesionEscaner> buscar(String id) { return Optional.ofNullable(mapa.get(id)); }
        @Override public void eliminar(String id) { mapa.remove(id); }
        @Override public int vigentes(Instant ahora) { return (int) mapa.values().stream().filter(s -> s.estaVigente(ahora)).count(); }
        @Override public int vigentesDe(String origen, Instant ahora) {
            return (int) mapa.values().stream().filter(s -> s.origen().equals(origen) && s.estaVigente(ahora)).count();
        }
        @Override public int limpiar(Instant ahora) { return 0; }
    }

    private Falso puerto;
    private EscanerRemotoService servicio;

    @BeforeEach
    void preparar() {
        puerto = new Falso();
        AtomicInteger n = new AtomicInteger();
        servicio = new EscanerRemotoService(puerto, Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), ZoneOffset.UTC), () -> "id" + n.incrementAndGet());
    }

    @Test
    @DisplayName("el flujo completo: la PC abre, el celular manda, la PC lee solo lo nuevo y cierra")
    void flujo() {
        SesionAbierta s = servicio.abrir("1.1.1.1");
        servicio.enviar(s.id(), "7501234567890");

        LecturaDeCodigos primera = servicio.leer(s.id(), 0);
        assertThat(primera.codigos()).extracting(c -> c.codigo()).containsExactly("7501234567890");
        assertThat(servicio.leer(s.id(), primera.ultimaSecuencia()).codigos()).isEmpty();

        servicio.cerrar(s.id());
        assertThatThrownBy(() -> servicio.enviar(s.id(), "7501234567890")).isInstanceOf(EscanerException.SesionNoDisponible.class);
        servicio.cerrar(s.id());   // cerrar de nuevo no es error
    }

    @Test
    @DisplayName("una conexión que no existe: ni se manda ni se lee")
    void noExiste() {
        assertThatThrownBy(() -> servicio.enviar("nada", "7501234567890")).isInstanceOf(EscanerException.SesionNoDisponible.class);
        assertThatThrownBy(() -> servicio.leer(null, 0)).isInstanceOf(EscanerException.SesionNoDisponible.class);
    }

    @Test
    @DisplayName("E6: máximo 10 conexiones por PC; otra PC sí puede")
    void limitePorOrigen() {
        for (int i = 0; i < EscanerRemotoService.MAXIMO_POR_ORIGEN; i++) {
            servicio.abrir("1.1.1.1");
        }
        assertThatThrownBy(() -> servicio.abrir("1.1.1.1")).isInstanceOf(EscanerException.DemasiadasSolicitudes.class);
        assertThat(servicio.abrir("2.2.2.2").id()).isNotBlank();
    }

    @Test
    @DisplayName("E6: máximo 2000 conexiones en total")
    void limiteTotal() {
        for (int i = 0; i < EscanerRemotoService.MAXIMO_TOTAL; i++) {
            puerto.guardar(new SesionEscaner("x" + i, "o" + i, Instant.parse("2026-11-02T12:00:00Z")));
        }
        assertThatThrownBy(() -> servicio.abrir("9.9.9.9")).isInstanceOf(EscanerException.DemasiadasSolicitudes.class);
    }

    @Test
    @DisplayName("sin origen conocido igual se limita (se agrupa como «desconocido»)")
    void sinOrigen() {
        for (int i = 0; i < EscanerRemotoService.MAXIMO_POR_ORIGEN; i++) {
            servicio.abrir(null);
        }
        assertThatThrownBy(() -> servicio.abrir("")).isInstanceOf(EscanerException.DemasiadasSolicitudes.class);
    }
}
