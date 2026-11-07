package com.ventas.key.hexagonal.preciocantidad.aplicacion;

import com.ventas.key.hexagonal.preciocantidad.aplicacion.servicio.AdministrarGruposService;
import com.ventas.key.hexagonal.preciocantidad.aplicacion.servicio.CalcularPreciosPorCantidadService;
import com.ventas.key.hexagonal.preciocantidad.dominio.excepcion.GrupoDePrecioInvalidoException;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.GrupoPrecioCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.LineaPorCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.MiembroDeGrupo;
import com.ventas.key.hexagonal.preciocantidad.dominio.puerto.salida.GruposDePrecioPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Los casos de uso contra un puerto en memoria: G2 (un producto, un grupo) y el cálculo con la fecha de hoy. */
class AdministrarGruposServiceTest {

    /** Puerto en memoria. */
    static class EnMemoria implements GruposDePrecioPort {
        final List<GrupoPrecioCantidad> datos = new ArrayList<>();
        int siguiente = 1;

        public List<GrupoPrecioCantidad> listar() { return List.copyOf(datos); }
        public Optional<GrupoPrecioCantidad> buscar(int id) { return datos.stream().filter(g -> g.id() == id).findFirst(); }
        public List<GrupoPrecioCantidad> activosConProductos(Set<Integer> ids) {
            return datos.stream().filter(GrupoPrecioCantidad::activo).filter(g -> g.miembros().stream().anyMatch(m -> ids.contains(m.productoId()))).toList();
        }
        public Optional<GrupoPrecioCantidad> grupoDelProducto(int p) { return datos.stream().filter(g -> g.miembro(p).isPresent()).findFirst(); }
        public GrupoPrecioCantidad guardar(GrupoPrecioCantidad g) {
            int id = g.id() == null ? siguiente++ : g.id();
            datos.removeIf(x -> x.id() == id);
            var guardado = new GrupoPrecioCantidad(id, g.nombre(), g.minimoPiezas(), g.fechaInicio(), g.fechaFin(), g.activo(), g.miembros());
            datos.add(guardado);
            return guardado;
        }
        public void eliminar(int id) { datos.removeIf(g -> g.id() == id); }
        public void cambiarActivo(int id, boolean activo) {
            var g = buscar(id).orElseThrow();
            datos.remove(g);
            datos.add(new GrupoPrecioCantidad(g.id(), g.nombre(), g.minimoPiezas(), g.fechaInicio(), g.fechaFin(), activo, g.miembros()));
        }
    }

    private EnMemoria puerto;
    private AdministrarGruposService admin;

    private static GrupoPrecioCantidad nuevo(String nombre, int minimo, int... productos) {
        List<MiembroDeGrupo> m = new ArrayList<>();
        for (int p : productos) m.add(new MiembroDeGrupo(p, "P" + p, new BigDecimal("100")));
        return new GrupoPrecioCantidad(null, nombre, minimo, null, null, true, m);
    }

    @BeforeEach
    void preparar() {
        puerto = new EnMemoria();
        admin = new AdministrarGruposService(puerto);
    }

    @Test
    void unProductoSoloPuedeEstarEnUnGrupo_yElMensajeDiceEnCual() {
        admin.crear(nuevo("Navidad bolsas", 3, 1, 2));
        assertThatThrownBy(() -> admin.crear(nuevo("Otro", 2, 2, 3)))
                .isInstanceOf(GrupoDePrecioInvalidoException.class)
                .hasMessageContaining("«P2» ya está en el grupo «Navidad bolsas»");
    }

    @Test
    void alActualizar_elGrupoPuedeConservarSusPropiosProductos() {
        var g = admin.crear(nuevo("G", 3, 1, 2));
        var cambiado = admin.actualizar(g.id(), nuevo("G renombrado", 4, 1, 2, 3));
        assertThat(cambiado.nombre()).isEqualTo("G renombrado");
        assertThat(cambiado.miembros()).hasSize(3);
    }

    @Test
    void sinProductosNoSeGuarda() {
        assertThatThrownBy(() -> admin.crear(new GrupoPrecioCantidad(null, "Vacío", 2, null, null, true, List.of())))
                .hasMessageContaining("al menos un producto");
    }

    @Test
    void apagarYBorrar_yUnGrupoQueNoExisteSeAvisa() {
        var g = admin.crear(nuevo("G", 2, 1));
        admin.cambiarActivo(g.id(), false);
        assertThat(admin.obtener(g.id()).activo()).isFalse();
        admin.eliminar(g.id());
        assertThatThrownBy(() -> admin.obtener(g.id())).hasMessageContaining("ya no existe");
        assertThat(admin.grupoDelProducto(1)).isEmpty();
    }

    @Test
    void elCalculoUsaLaFechaDeHoy_yUnGrupoApagadoNoCuenta() {
        Clock reloj = Clock.fixed(Instant.parse("2026-12-10T15:00:00Z"), ZoneId.of("UTC"));
        var calculo = new CalcularPreciosPorCantidadService(puerto, reloj);
        var g = admin.crear(new GrupoPrecioCantidad(null, "G", 2, java.time.LocalDate.of(2026, 12, 1), java.time.LocalDate.of(2026, 12, 31), true,
                List.of(new MiembroDeGrupo(1, "P1", new BigDecimal("200")))));
        assertThat(calculo.calcular(List.of(new LineaPorCantidad(1, 2)))).containsOnlyKeys(1);
        assertThat(calculo.calcular(List.of(new LineaPorCantidad(1, 1)))).isEmpty();
        assertThat(calculo.calcular(List.of())).isEmpty();
        admin.cambiarActivo(g.id(), false);
        assertThat(calculo.calcular(List.of(new LineaPorCantidad(1, 2)))).isEmpty();
    }
}
