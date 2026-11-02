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
        @Override public Optional<SesionEscaner> buscarVigentePorOrigen(String origen, Instant ahora) {
            return mapa.values().stream().filter(s -> s.origen().equals(origen) && s.estaVigente(ahora)).findFirst();
        }
        @Override public int vigentesAnonimas(Instant ahora, String prefijo) {
            return (int) mapa.values().stream().filter(s -> !s.origen().startsWith(prefijo) && s.estaVigente(ahora)).count();
        }
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
    @DisplayName("E6 con solicitudes simultáneas: 50 hilos de la misma PC abren exactamente 10 conexiones")
    void limitePorOrigenConcurrente() throws Exception {
        var pool = java.util.concurrent.Executors.newFixedThreadPool(50);
        var salida = new java.util.concurrent.CountDownLatch(1);
        var buenas = new AtomicInteger();
        var rechazadas = new AtomicInteger();
        java.util.List<java.util.concurrent.Future<?>> tareas = new java.util.ArrayList<>();
        for (int i = 0; i < 50; i++) {
            tareas.add(pool.submit(() -> {
                salida.await();
                try { servicio.abrir("7.7.7.7"); buenas.incrementAndGet(); }
                catch (EscanerException.DemasiadasSolicitudes e) { rechazadas.incrementAndGet(); }
                return null;
            }));
        }
        salida.countDown();
        for (var t : tareas) t.get();
        pool.shutdown();
        assertThat(buenas.get()).isEqualTo(EscanerRemotoService.MAXIMO_POR_ORIGEN);
        assertThat(rechazadas.get()).isEqualTo(40);
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
    @DisplayName("E6: el cupo total es de las conexiones anónimas; el personal con sesión (usuario:…) sigue pudiendo abrir")
    void cupoTotalNoBloqueaAlPersonal() {
        for (int i = 0; i < EscanerRemotoService.MAXIMO_TOTAL; i++) {
            puerto.guardar(new SesionEscaner("x" + i, "anonimo" + i, Instant.parse("2026-11-02T12:00:00Z")));
        }
        assertThatThrownBy(() -> servicio.abrir("9.9.9.9")).isInstanceOf(EscanerException.DemasiadasSolicitudes.class);
        assertThat(servicio.abrir("usuario:ana").id()).isNotBlank();
    }

    @Test
    @DisplayName("enviar dice si la PC lo estaba esperando")
    void enviarDiceSiLoEsperaban() {
        SesionAbierta s = servicio.abrir("1.1.1.1");
        assertThat(servicio.enviar(s.id(), "7501234567890")).isFalse();
        servicio.esperar(s.id(), 1);
        assertThat(servicio.enviar(s.id(), "7501234567891")).isTrue();
    }

    @Test
    @DisplayName("canal de la cuenta: la PC y el celular de la misma persona caen en la misma conexión; otra cuenta tiene la suya")
    void canalDeLaCuenta() {
        String pc = servicio.idDelCanal("ana");
        String celular = servicio.idDelCanal("ana");
        assertThat(celular).isEqualTo(pc);
        assertThat(servicio.idDelCanal("beto")).isNotEqualTo(pc);

        servicio.esperar(pc, 0);
        assertThat(servicio.enviar(celular, "7501234567890")).isTrue();
        assertThatThrownBy(() -> servicio.idDelCanal(null)).isInstanceOf(EscanerException.SesionNoDisponible.class);
        assertThatThrownBy(() -> servicio.idDelCanal(" ")).isInstanceOf(EscanerException.SesionNoDisponible.class);
    }

    @Test
    @DisplayName("canal de la cuenta: si la conexión se cierra, la siguiente llamada abre otra y los dos lados vuelven a coincidir (sin QR)")
    void canalSeRecuperaSolo() {
        String vieja = servicio.idDelCanal("ana");
        servicio.cerrar(vieja);
        String pc = servicio.idDelCanal("ana");
        assertThat(pc).isNotEqualTo(vieja);
        assertThat(servicio.idDelCanal("ana")).isEqualTo(pc);
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
