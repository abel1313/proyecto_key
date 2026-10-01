package com.ventas.key.hexagonal.datosprueba;

import com.ventas.key.hexagonal.datosprueba.aplicacion.servicio.GenerarDatosPruebaService;
import com.ventas.key.hexagonal.datosprueba.dominio.excepcion.AmbienteNoPermitidoException;
import com.ventas.key.hexagonal.datosprueba.dominio.excepcion.GeneracionEnCursoException;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ArticuloDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ArticuloGuardado;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.Avance;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.LineaDePedido;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ModeloDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.PlanDeDatos;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.TipoPedidoPrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.AmbientePort;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.CatalogoPruebaPort;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.PedidosPruebaPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El servicio con puertos falsos en memoria: como se reparten los lotes, los pedidos y los abonos
 * (README: R1, R3, R9, R12, R13, R15).
 */
class GenerarDatosPruebaServiceTest {

    private static final Executor EN_ESTE_HILO = Runnable::run;

    // ── Puertos falsos ──────────────────────────────────────────────────────────────────

    static class Catalogo implements CatalogoPruebaPort {
        final List<Integer> lotes = new ArrayList<>();
        int siguienteId = 1;
        int imagenes = 5;
        Integer fallarEnLote;

        @Override public long siguienteNumero() { return 1; }
        @Override public void asegurarCategorias(List<String> categorias) { }
        @Override public int imagenesDisponibles() { return imagenes; }
        @Override public int darDeBaja() { return 7; }

        @Override
        public List<ArticuloGuardado> guardarLote(List<ModeloDePrueba> modelos) {
            if (fallarEnLote != null && lotes.size() + 1 == fallarEnLote) {
                throw new IllegalStateException("se cayo la base");
            }
            lotes.add(modelos.size());
            List<ArticuloGuardado> r = new ArrayList<>();
            for (ModeloDePrueba m : modelos) {
                for (ArticuloDePrueba a : m.articulos()) {
                    r.add(new ArticuloGuardado(siguienteId++, m.precioVenta(), a.stock()));
                }
            }
            return r;
        }
    }

    /** Lleva la cuenta de cada pedido como lo haria el abono real: nunca mas que el saldo. */
    static class Pedidos implements PedidosPruebaPort {
        final Map<Integer, double[]> pedidos = new HashMap<>();   // id -> {total, pagado}
        final Map<Integer, TipoPedidoPrueba> tipos = new HashMap<>();
        int siguiente = 1;
        boolean fallarSiempre;

        @Override public List<Integer> clientesDePrueba(int cuantos) { return List.of(1, 2, 3); }

        @Override
        public PedidoCreado crearPedido(TipoPedidoPrueba tipo, int clienteId, int usuarioId, List<LineaDePedido> lineas) {
            if (fallarSiempre) {
                throw new IllegalStateException("Stock insuficiente");
            }
            double total = lineas.stream().mapToDouble(LineaDePedido::subtotal).sum();
            int id = siguiente++;
            pedidos.put(id, new double[]{total, 0});
            tipos.put(id, tipo);
            return new PedidoCreado(tipo == TipoPedidoPrueba.CONTADO ? null : id, total);
        }

        @Override
        public void abonar(int pedidoId, double monto, int usuarioId) {
            double[] p = pedidos.get(pedidoId);
            double saldo = p[0] - p[1];
            if (monto <= 0 || monto > saldo + 0.001) {
                throw new IllegalStateException("abono invalido: " + monto + " con saldo " + saldo);
            }
            if (tipos.get(pedidoId) != TipoPedidoPrueba.IR_PAGANDO && Math.abs(monto - saldo) > 0.01) {
                throw new IllegalStateException("un Apartado solo se paga completo");
            }
            p[1] += monto;
        }
    }

    private static AmbientePort base(String nombre) {
        return () -> nombre;
    }

    private GenerarDatosPruebaService servicio(Catalogo c, Pedidos p, String base, Executor ex) {
        return new GenerarDatosPruebaService(base(base), c, p, ex);
    }

    // ── Pruebas ─────────────────────────────────────────────────────────────────────────

    @Test
    void r1_fuera_de_qa_no_hace_nada() {
        Catalogo c = new Catalogo();
        GenerarDatosPruebaService s = servicio(c, new Pedidos(), "inventario_key", EN_ESTE_HILO);

        assertThatThrownBy(() -> s.iniciar(new PlanDeDatos(10, 1, 2, 5, 1L), 1))
                .isInstanceOf(AmbienteNoPermitidoException.class)
                .hasMessageContaining("inventario_key");
        assertThatThrownBy(s::darDeBaja).isInstanceOf(AmbienteNoPermitidoException.class);
        assertThat(c.lotes).isEmpty();
        assertThat(s.avance().estado()).isEqualTo(Avance.Estado.SIN_CORRER);
    }

    @Test
    void r12_lotes_de_500_y_el_avance_al_terminar() {
        Catalogo c = new Catalogo();
        Pedidos p = new Pedidos();
        GenerarDatosPruebaService s = servicio(c, p, AmbientePort.BASE_DE_PRUEBAS, EN_ESTE_HILO);

        s.iniciar(new PlanDeDatos(1_234, 1, 4, 300, 99L), 1);

        assertThat(c.lotes).containsExactly(500, 500, 234);
        Avance a = s.avance();
        assertThat(a.estado()).isEqualTo(Avance.Estado.TERMINADO);
        assertThat(a.modelosCreados()).isEqualTo(1_234);
        assertThat(a.articulosCreados()).isEqualTo(c.siguienteId - 1).isBetween(1_234, 4 * 1_234);
        assertThat(a.pedidosCreados()).isEqualTo(300);
        assertThat(a.pedidosConError()).isZero();
        assertThat(a.fin()).isNotNull();
    }

    @Test
    void r9_cada_pedido_se_cobra_segun_su_forma_de_cobro() {
        Catalogo c = new Catalogo();
        Pedidos p = new Pedidos();
        servicio(c, p, AmbientePort.BASE_DE_PRUEBAS, EN_ESTE_HILO).iniciar(new PlanDeDatos(500, 1, 4, 1_000, 7L), 1);

        int irPagandoLiquidados = 0;
        int irPagandoDebiendo = 0;
        for (Map.Entry<Integer, TipoPedidoPrueba> e : p.tipos.entrySet()) {
            double[] cuenta = p.pedidos.get(e.getKey());
            switch (e.getValue()) {
                // El contado lo cobra la venta misma; aqui no se le abona nada.
                case CONTADO, APARTADO -> assertThat(cuenta[1]).as("pedido %d", e.getKey()).isZero();
                case APARTADO_PAGADO -> assertThat(cuenta[1]).isEqualTo(cuenta[0]);
                case IR_PAGANDO -> {
                    // Siempre con enganche, nunca mas que el total.
                    assertThat(cuenta[1]).isPositive().isLessThanOrEqualTo(cuenta[0]);
                    if (Math.abs(cuenta[1] - cuenta[0]) < 0.01) {
                        irPagandoLiquidados++;
                    } else {
                        irPagandoDebiendo++;
                    }
                }
            }
        }
        assertThat(irPagandoDebiendo).isGreaterThan(irPagandoLiquidados);
        assertThat(irPagandoLiquidados).isPositive();
    }

    @Test
    void r12_si_falla_un_lote_lo_guardado_se_queda_y_dice_donde_paro() {
        Catalogo c = new Catalogo();
        c.fallarEnLote = 3;
        GenerarDatosPruebaService s = servicio(c, new Pedidos(), AmbientePort.BASE_DE_PRUEBAS, EN_ESTE_HILO);

        s.iniciar(new PlanDeDatos(2_000, 1, 2, 10, 1L), 1);

        Avance a = s.avance();
        assertThat(a.estado()).isEqualTo(Avance.Estado.FALLO);
        assertThat(a.modelosCreados()).isEqualTo(1_000);
        assertThat(a.fase()).contains("se cayo la base");
    }

    @Test
    void r13_si_fallan_la_mayoria_de_los_primeros_pedidos_se_detiene() {
        Pedidos p = new Pedidos();
        p.fallarSiempre = true;
        GenerarDatosPruebaService s = servicio(new Catalogo(), p, AmbientePort.BASE_DE_PRUEBAS, EN_ESTE_HILO);

        s.iniciar(new PlanDeDatos(100, 1, 2, 500, 1L), 1);

        Avance a = s.avance();
        assertThat(a.estado()).isEqualTo(Avance.Estado.FALLO);
        assertThat(a.pedidosConError()).isEqualTo(20);
        assertThat(a.fase()).contains("Stock insuficiente");
    }

    @Test
    void r3_y_r15_una_corrida_a_la_vez_y_no_se_da_de_baja_mientras_corre() {
        List<Runnable> pendientes = new ArrayList<>();
        GenerarDatosPruebaService s = servicio(new Catalogo(), new Pedidos(), AmbientePort.BASE_DE_PRUEBAS, pendientes::add);

        Avance primero = s.iniciar(new PlanDeDatos(10, 1, 1, 0, 1L), 1);
        assertThat(primero.estado()).isEqualTo(Avance.Estado.EN_CURSO);

        assertThatThrownBy(() -> s.iniciar(new PlanDeDatos(10, 1, 1, 0, 1L), 1))
                .isInstanceOf(GeneracionEnCursoException.class);
        assertThatThrownBy(s::darDeBaja).isInstanceOf(GeneracionEnCursoException.class);

        pendientes.get(0).run();
        assertThat(s.avance().estado()).isEqualTo(Avance.Estado.TERMINADO);
        assertThat(s.darDeBaja()).isEqualTo(7);
    }

    @Test
    void r7_sin_imagenes_en_qa_se_crean_igual_y_lo_avisa() {
        Catalogo c = new Catalogo();
        c.imagenes = 0;
        GenerarDatosPruebaService s = servicio(c, new Pedidos(), AmbientePort.BASE_DE_PRUEBAS, EN_ESTE_HILO);

        s.iniciar(new PlanDeDatos(10, 1, 1, 0, 1L), 1);

        assertThat(s.avance().estado()).isEqualTo(Avance.Estado.TERMINADO);
        assertThat(s.avance().aviso()).contains("no van a salir en la tienda");
    }
}
