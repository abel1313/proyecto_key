package com.ventas.key.hexagonal.campana;

import com.ventas.key.hexagonal.campana.aplicacion.servicio.AdministrarCampanasService;
import com.ventas.key.hexagonal.campana.aplicacion.servicio.ConsultarCampanaActivaService;
import com.ventas.key.hexagonal.campana.dominio.excepcion.CampanaInvalidaException;
import com.ventas.key.hexagonal.campana.dominio.modelo.Campana;
import com.ventas.key.hexagonal.campana.dominio.modelo.PrecioDeCampana;
import com.ventas.key.hexagonal.campana.dominio.puerto.salida.CampanasPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas de la campaña (dominio) y de quien las administra, con un puerto en memoria. */
class CampanaDominioYServiciosTest {

    private static final LocalDate INI = LocalDate.of(2026, 12, 20);
    private static final LocalDate FIN = LocalDate.of(2027, 1, 6);

    private static PrecioDeCampana precio(int id, String p) {
        return new PrecioDeCampana(id, "Juguete " + id, new BigDecimal(p));
    }

    private static Campana campana(Integer id, boolean activa, LocalDate ini, LocalDate fin, PrecioDeCampana... precios) {
        return new Campana(id, "Reyes", ini, fin, activa, new BigDecimal("1500"), List.of(precios));
    }

    /** Puerto en memoria. */
    public static class Memoria implements CampanasPort {
        final List<Campana> datos = new ArrayList<>();
        int sig = 1;

        public List<Campana> listar() { return List.copyOf(datos); }
        public Optional<Campana> buscar(int id) { return datos.stream().filter(c -> c.id() == id).findFirst(); }
        public List<Campana> activas() { return datos.stream().filter(Campana::activa).toList(); }
        public Campana guardar(Campana c) {
            Campana g = new Campana(c.id() == null ? sig++ : c.id(), c.nombre(), c.fechaInicio(), c.fechaFin(), c.activa(), c.metaMonto(), c.precios());
            datos.removeIf(x -> x.id().equals(g.id()));
            datos.add(g);
            return g;
        }
        public void eliminar(int id) { datos.removeIf(c -> c.id() == id); }
        public void activarSola(int id) {
            List<Campana> nueva = datos.stream().map(c -> new Campana(c.id(), c.nombre(), c.fechaInicio(), c.fechaFin(), c.id() == id, c.metaMonto(), c.precios())).toList();
            datos.clear();
            datos.addAll(nueva);
        }
        public void apagar(int id) {
            Campana c = buscar(id).orElseThrow();
            guardar(new Campana(c.id(), c.nombre(), c.fechaInicio(), c.fechaFin(), false, c.metaMonto(), c.precios()));
        }
    }

    @Test
    @DisplayName("la campaña rechaza lo inválido con mensajes en palabras del dueño")
    void validaciones() {
        assertThatThrownBy(() -> new Campana(null, " ", INI, FIN, false, null, List.of())).hasMessageContaining("nombre");
        assertThatThrownBy(() -> new Campana(null, "X", null, FIN, false, null, List.of())).hasMessageContaining("fecha de inicio");
        assertThatThrownBy(() -> new Campana(null, "X", FIN, INI, false, null, List.of())).hasMessageContaining("no puede ser después");
        assertThatThrownBy(() -> new Campana(null, "X", INI, FIN, false, new BigDecimal("-1"), List.of())).hasMessageContaining("negativa");
        assertThatThrownBy(() -> campana(null, false, INI, FIN, precio(1, "100"), precio(1, "90"))).hasMessageContaining("repetido");
        assertThatThrownBy(() -> precio(1, "0")).isInstanceOf(CampanaInvalidaException.class);
    }

    @Test
    @DisplayName("vigente = encendida y dentro de sus fechas, los dos extremos incluidos")
    void vigencia() {
        Campana c = campana(1, true, INI, FIN, precio(1, "100"));
        assertThat(c.vigenteEn(INI)).isTrue();
        assertThat(c.vigenteEn(FIN)).isTrue();
        assertThat(c.vigenteEn(INI.minusDays(1))).isFalse();
        assertThat(c.vigenteEn(FIN.plusDays(1))).isFalse();
        assertThat(campana(1, false, INI, FIN, precio(1, "100")).vigenteEn(INI)).isFalse();
        assertThat(c.precioDe(1)).isPresent();
        assertThat(c.precioDe(2)).isEmpty();
    }

    @Test
    @DisplayName("crear nace apagada y pide al menos un juguete; activar apaga las demás; duplicar copia apagada")
    void administrar() {
        Memoria m = new Memoria();
        AdministrarCampanasService s = new AdministrarCampanasService(m);
        assertThatThrownBy(() -> s.crear(campana(null, true, INI, FIN))).hasMessageContaining("al menos un juguete");

        Campana reyes = s.crear(campana(null, true, INI, FIN, precio(1, "100")));
        assertThat(reyes.activa()).as("nace apagada aunque venga encendida").isFalse();
        Campana roscas = s.crear(campana(null, false, LocalDate.of(2027, 1, 7), LocalDate.of(2027, 1, 10), precio(2, "50")));

        s.activar(reyes.id());
        s.activar(roscas.id());
        assertThat(m.activas()).extracting(Campana::id).containsExactly(roscas.id());

        Campana copia = s.duplicar(reyes.id());
        assertThat(copia.nombre()).isEqualTo("Copia de Reyes");
        assertThat(copia.activa()).isFalse();
        assertThat(copia.precios()).isEqualTo(reyes.precios());

        s.actualizar(roscas.id(), campana(null, false, INI, FIN, precio(3, "70")));
        assertThat(s.obtener(roscas.id()).activa()).as("actualizar no cambia si está encendida").isTrue();
        assertThat(s.obtener(roscas.id()).precios()).extracting(PrecioDeCampana::productoId).containsExactly(3);

        s.apagar(roscas.id());
        assertThat(m.activas()).isEmpty();
        s.eliminar(copia.id());
        assertThatThrownBy(() -> s.obtener(copia.id())).hasMessageContaining("ya no existe");
    }

    @Test
    @DisplayName("la campaña vigente solo sale si está encendida y hoy cae en sus fechas")
    void vigente() {
        Memoria m = new Memoria();
        m.guardar(campana(null, true, INI, FIN, precio(1, "100")));
        ZoneId z = ZoneId.of("America/Mexico_City");
        Clock dentro = Clock.fixed(LocalDate.of(2026, 12, 25).atStartOfDay(z).toInstant(), z);
        Clock fuera = Clock.fixed(LocalDate.of(2027, 2, 1).atStartOfDay(z).toInstant(), z);
        assertThat(new ConsultarCampanaActivaService(m, dentro).vigente()).isPresent();
        assertThat(new ConsultarCampanaActivaService(m, fuera).vigente()).isEmpty();
        assertThat(new ConsultarCampanaActivaService(new Memoria(), dentro).vigente()).isEmpty();
    }
}
