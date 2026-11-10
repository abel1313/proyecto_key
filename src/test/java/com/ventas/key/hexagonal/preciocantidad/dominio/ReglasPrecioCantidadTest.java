package com.ventas.key.hexagonal.preciocantidad.dominio;

import com.ventas.key.hexagonal.preciocantidad.dominio.excepcion.GrupoDePrecioInvalidoException;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.GrupoPrecioCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.LineaPorCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.MiembroDeGrupo;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.PrecioDeGrupo;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.ReglasPrecioCantidad;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas G1 a G4 y G6 de PLAN_CAMPANAS_Y_PRECIOS.md, sin Spring ni base de datos. */
class ReglasPrecioCantidadTest {

    private static final LocalDate HOY = LocalDate.of(2026, 12, 10);

    private static MiembroDeGrupo m(int producto, String precio) {
        return new MiembroDeGrupo(producto, "P" + producto, new BigDecimal(precio));
    }

    private static GrupoPrecioCantidad grupo(int id, int minimo, MiembroDeGrupo... miembros) {
        return new GrupoPrecioCantidad(id, "Grupo " + id, minimo, null, null, true, List.of(miembros));
    }

    private static List<LineaPorCantidad> lineas(int... productoYCantidad) {
        java.util.ArrayList<LineaPorCantidad> l = new java.util.ArrayList<>();
        for (int i = 0; i < productoYCantidad.length; i += 2) {
            l.add(new LineaPorCantidad(productoYCantidad[i], productoYCantidad[i + 1]));
        }
        return l;
    }

    @Test
    void unaPiezaDeCadaProductoDelGrupo_juntanElMinimo_ylosTresEntran() {
        var g1 = grupo(1, 3, m(1, "200"), m(2, "270"), m(3, "150"));
        Map<Integer, PrecioDeGrupo> r = ReglasPrecioCantidad.calcular(List.of(g1), lineas(1, 1, 2, 1, 3, 1), HOY);
        assertThat(r).containsOnlyKeys(1, 2, 3);
        assertThat(r.get(2).precio()).isEqualByComparingTo("270");
        assertThat(r.get(1).piezasDelGrupo()).isEqualTo(3);
    }

    @Test
    void sinLlegarAlMinimo_nadieEntra() {
        var g1 = grupo(1, 3, m(1, "200"), m(2, "270"));
        assertThat(ReglasPrecioCantidad.calcular(List.of(g1), lineas(1, 1, 2, 1), HOY)).isEmpty();
    }

    @Test
    void dosPiezasDelMismoProducto_valenParaUnGrupoDeUnSoloProducto() {
        var g = grupo(1, 2, m(1, "200"));
        assertThat(ReglasPrecioCantidad.calcular(List.of(g), lineas(1, 2), HOY)).containsOnlyKeys(1);
    }

    @Test
    void lasPiezasSoloSeSumanDentroDelMismoGrupo() {
        var g1 = grupo(1, 3, m(1, "200"), m(2, "270"));
        var g2 = grupo(2, 3, m(3, "150"), m(4, "100"));
        // 1 pieza de A (grupo 1), 1 de C y 1 de D (grupo 2): ningun grupo llega a 3.
        assertThat(ReglasPrecioCantidad.calcular(List.of(g1, g2), lineas(1, 1, 3, 1, 4, 1), HOY)).isEmpty();
        // 2 del grupo 2 y 1 del grupo 1: tampoco.
        assertThat(ReglasPrecioCantidad.calcular(List.of(g1, g2), lineas(1, 1, 3, 1, 4, 1), HOY)).isEmpty();
    }

    @Test
    void soloEntranLosProductosDelGrupoQueLaVentaLleva() {
        var g = grupo(1, 2, m(1, "200"), m(2, "270"), m(3, "150"));
        assertThat(ReglasPrecioCantidad.calcular(List.of(g), lineas(1, 2), HOY)).containsOnlyKeys(1);
    }

    @Test
    void variasLineasDelMismoProducto_porEjemploTallas_sumanSusPiezas() {
        var g = grupo(1, 2, m(1, "200"));
        assertThat(ReglasPrecioCantidad.calcular(List.of(g), lineas(1, 1, 1, 1), HOY)).containsOnlyKeys(1);
    }

    @Test
    void fueraDeFechasOApagado_noCuenta() {
        var vencido = new GrupoPrecioCantidad(1, "V", 2, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), true, List.of(m(1, "200")));
        var futuro = new GrupoPrecioCantidad(2, "F", 2, LocalDate.of(2027, 1, 1), null, true, List.of(m(2, "200")));
        var apagado = new GrupoPrecioCantidad(3, "A", 2, null, null, false, List.of(m(3, "200")));
        assertThat(ReglasPrecioCantidad.calcular(List.of(vencido, futuro, apagado), lineas(1, 2, 2, 2, 3, 2), HOY)).isEmpty();
    }

    @Test
    void losDosExtremosDeLasFechasEntran() {
        var g = new GrupoPrecioCantidad(1, "G", 2, HOY, HOY, true, List.of(m(1, "200")));
        assertThat(ReglasPrecioCantidad.calcular(List.of(g), lineas(1, 2), HOY)).containsOnlyKeys(1);
        assertThat(ReglasPrecioCantidad.calcular(List.of(g), lineas(1, 2), HOY.plusDays(1))).isEmpty();
        assertThat(ReglasPrecioCantidad.calcular(List.of(g), lineas(1, 2), HOY.minusDays(1))).isEmpty();
    }

    @Test
    void gana_elPrecioMasBajo_losDescuentosNoSeSuman() {
        var g = grupo(1, 2, m(1, "200"));
        PrecioDeGrupo pg = ReglasPrecioCantidad.calcular(List.of(g), lineas(1, 2), HOY).get(1);
        assertThat(ReglasPrecioCantidad.precioACobrar(new BigDecimal("250"), pg)).isEqualByComparingTo("200");
        assertThat(ReglasPrecioCantidad.precioACobrar(new BigDecimal("180"), pg)).isEqualByComparingTo("180");
        assertThat(ReglasPrecioCantidad.precioACobrar(new BigDecimal("250"), null)).isEqualByComparingTo("250");
    }

    @Test
    void lasReglasDelGrupo_seValidanAlCrearlo() {
        assertThatThrownBy(() -> grupo(1, 1, m(1, "200"))).isInstanceOf(GrupoDePrecioInvalidoException.class).hasMessageContaining("2 o más");
        assertThatThrownBy(() -> new GrupoPrecioCantidad(1, " ", 2, null, null, true, List.of(m(1, "200")))).hasMessageContaining("nombre");
        assertThatThrownBy(() -> new GrupoPrecioCantidad(1, "G", 2, HOY.plusDays(1), HOY, true, List.of(m(1, "200")))).hasMessageContaining("fecha de inicio");
        assertThatThrownBy(() -> grupo(1, 2, m(1, "200"), m(1, "180"))).hasMessageContaining("repetido");
        assertThatThrownBy(() -> m(1, "0")).hasMessageContaining("mayor a cero");
    }

    // ── Pedido ya guardado: se vuelve a calcular (Fase 1b) ─────────────────────

    private static com.ventas.key.hexagonal.preciocantidad.dominio.modelo.LineaDePrecio linea(int detalle, int producto, String actual, String sinGrupo, boolean combo) {
        return new com.ventas.key.hexagonal.preciocantidad.dominio.modelo.LineaDePrecio(detalle, producto, new BigDecimal(actual),
                sinGrupo == null ? null : new BigDecimal(sinGrupo), combo);
    }

    private static Map<Integer, PrecioDeGrupo> grupoVigente(int minimo, MiembroDeGrupo... m) {
        return ReglasPrecioCantidad.calcular(List.of(grupo(1, minimo, m)), lineas(1, minimo), HOY).isEmpty() ? Map.of()
                : ReglasPrecioCantidad.calcular(List.of(grupo(1, minimo, m)), lineas(m[0].productoId(), minimo), HOY);
    }

    @Test
    void alEditar_siElGrupoYaNoJuntaElMinimo_laLineaRegresaAlPrecioQueTeniaAntes() {
        // Se cobro a $200 (precio de grupo) y antes valia $250.
        var lineas = List.of(linea(10, 1, "200", "250", false));
        var r = ReglasPrecioCantidad.repreciar(lineas, Map.of());
        assertThat(r).hasSize(1);
        assertThat(r.get(0).precioNuevo()).isEqualByComparingTo("250");
        assertThat(r.get(0).precioSinGrupoNuevo()).as("ya no se cobra con grupo: se borra la marca").isNull();
    }

    @Test
    void alEditar_siElGrupoJuntaElMinimo_laLineaPasaAlPrecioDeGrupo_yRecuerdaElDeAntes() {
        var g = grupoVigente(2, m(1, "200"));
        var r = ReglasPrecioCantidad.repreciar(List.of(linea(10, 1, "250", null, false)), g);
        assertThat(r.get(0).precioNuevo()).isEqualByComparingTo("200");
        assertThat(r.get(0).precioSinGrupoNuevo()).isEqualByComparingTo("250");
    }

    @Test
    void alEditar_unaRebajaMasBajaQueElGrupoSeRespeta_yNoSeMarcaComoDeGrupo() {
        var g = grupoVigente(2, m(1, "200"));
        var r = ReglasPrecioCantidad.repreciar(List.of(linea(10, 1, "180", null, false)), g);
        assertThat(r.get(0).precioNuevo()).isEqualByComparingTo("180");
        assertThat(r.get(0).precioSinGrupoNuevo()).isNull();
    }

    @Test
    void alEditar_laLineaQueSeCobroConGrupoSobreUnaRebaja_regresaALaRebaja_noAlNormal() {
        // Base 220 (rebaja del administrador), precio de grupo 200: al bajar del minimo regresa a 220.
        var r = ReglasPrecioCantidad.repreciar(List.of(linea(10, 1, "200", "220", false)), Map.of());
        assertThat(r.get(0).precioNuevo()).isEqualByComparingTo("220");
    }

    @Test
    void alEditar_lasLineasDeUnComboNoSeTocan_yLasQueNuncaSeCobraronConGrupoSeQuedan() {
        var g = grupoVigente(2, m(1, "200"));
        var r = ReglasPrecioCantidad.repreciar(List.of(linea(10, 1, "90", null, true), linea(11, 2, "300", null, false)), g);
        assertThat(r).extracting(x -> x.detalleId()).containsExactly(11);
        assertThat(r.get(0).precioNuevo()).isEqualByComparingTo("300");
        assertThat(r.get(0).precioSinGrupoNuevo()).isNull();
    }
}
