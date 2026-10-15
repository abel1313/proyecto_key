package com.ventas.key.hexagonal.datosprueba.aplicacion.servicio;

import com.ventas.key.hexagonal.datosprueba.dominio.excepcion.AmbienteNoPermitidoException;
import com.ventas.key.hexagonal.datosprueba.dominio.excepcion.GeneracionEnCursoException;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ArticuloGuardado;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.Avance;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.CatalogoAleatorio;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.LineaDePedido;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ModeloDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.PlanDeDatos;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.TipoPedidoPrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.entrada.DatosPruebaCasoUso;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.AmbientePort;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.CatalogoPruebaPort;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.PedidosPruebaPort;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.PedidosPruebaPort.PedidoCreado;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Use Case interactor]
 *
 * <p>Arranca la corrida en un hilo propio y regresa de inmediato (el pedido del dueño: "ejecutarlo
 * una vez y que se guarde en segundo plano"). Primero los modelos y articulos, en lotes; despues
 * los pedidos, con los articulos recien creados.
 */
@Slf4j
@Service
public class GenerarDatosPruebaService implements DatosPruebaCasoUso {

    static final int TAMANO_LOTE = 500;
    /** R13: si de los primeros 20 pedidos fallan mas de 10, algo esta mal y se detiene. */
    static final int PEDIDOS_DE_MUESTRA = 20;

    private final AmbientePort ambiente;
    private final CatalogoPruebaPort catalogo;
    private final PedidosPruebaPort pedidos;
    private final Executor executor;

    private final AtomicReference<Avance> avance = new AtomicReference<>(Avance.sinCorrer());

    public GenerarDatosPruebaService(AmbientePort ambiente, CatalogoPruebaPort catalogo, PedidosPruebaPort pedidos,
                                     @Qualifier("datosPruebaExecutor") Executor executor) {
        this.ambiente = ambiente;
        this.catalogo = catalogo;
        this.pedidos = pedidos;
        this.executor = executor;
    }

    @Override
    public Avance iniciar(PlanDeDatos plan, int usuarioId) {
        exigirBaseDePruebas();
        Avance actual = avance.get();
        Avance nuevo = Avance.arranque(plan);
        // R3: compareAndSet para que dos clics casi al mismo tiempo no arranquen dos corridas.
        if (actual.enCurso() || !avance.compareAndSet(actual, nuevo)) {
            throw new GeneracionEnCursoException();
        }
        log.info("Datos de prueba: arranca la corrida ({} modelos, {}-{} articulos, {} pedidos, semilla {})",
                plan.modelos(), plan.articulosMin(), plan.articulosMax(), plan.pedidos(), plan.semilla());
        executor.execute(() -> correr(plan, usuarioId));
        return nuevo;
    }

    @Override
    public Avance avance() {
        return avance.get();
    }

    @Override
    public int darDeBaja() {
        exigirBaseDePruebas();
        if (avance.get().enCurso()) {
            throw new GeneracionEnCursoException();
        }
        int articulos = catalogo.darDeBaja();
        log.info("Datos de prueba: {} articulos dados de baja", articulos);
        return articulos;
    }

    /** R1. */
    private void exigirBaseDePruebas() {
        String base = ambiente.nombreDeLaBase();
        if (!AmbientePort.BASE_DE_PRUEBAS.equals(base)) {
            throw new AmbienteNoPermitidoException(base);
        }
    }

    // ───────────────────────────── La corrida ─────────────────────────────

    void correr(PlanDeDatos plan, int usuarioId) {
        Random rnd = new Random(plan.semilla());
        try {
            List<ArticuloGuardado> articulos = crearCatalogo(plan, rnd);
            if (plan.pedidos() > 0 && !crearPedidos(plan, usuarioId, articulos, rnd)) {
                return;
            }
            avance.updateAndGet(Avance::terminado);
            log.info("Datos de prueba: terminado {}", avance.get());
        } catch (RuntimeException e) {
            log.error("Datos de prueba: la corrida se detuvo", e);
            avance.updateAndGet(a -> a.fallo(motivo(e)));
        }
    }

    private List<ArticuloGuardado> crearCatalogo(PlanDeDatos plan, Random rnd) {
        avance.updateAndGet(a -> a.conFase("Preparando categorías"));
        catalogo.asegurarCategorias(CatalogoAleatorio.categorias());
        if (catalogo.imagenesDisponibles() == 0) {
            // R7: se crean igual, pero sin foto la tienda no los muestra.
            avance.updateAndGet(a -> a.conAviso("QA no tiene imágenes para reusar: los artículos se crearon, "
                    + "pero no van a salir en la tienda pública hasta que tengan foto"));
        }

        long numero = catalogo.siguienteNumero();
        List<ArticuloGuardado> todos = new ArrayList<>();
        int modelosHechos = 0;
        while (modelosHechos < plan.modelos()) {
            int enEsteLote = Math.min(TAMANO_LOTE, plan.modelos() - modelosHechos);
            List<ModeloDePrueba> lote = new ArrayList<>(enEsteLote);
            for (int i = 0; i < enEsteLote; i++) {
                lote.add(CatalogoAleatorio.modelo(numero++, plan.articulosMin(), plan.articulosMax(), rnd));
            }
            todos.addAll(catalogo.guardarLote(lote));
            modelosHechos += enEsteLote;
            int modelos = modelosHechos;
            int articulos = todos.size();
            avance.updateAndGet(a -> a.conCatalogo(modelos, articulos));
        }
        return todos;
    }

    /** @return false si se detuvo por R13 */
    private boolean crearPedidos(PlanDeDatos plan, int usuarioId, List<ArticuloGuardado> articulos, Random rnd) {
        avance.updateAndGet(a -> a.conFase("Creando pedidos"));
        List<Integer> clientes = pedidos.clientesDePrueba(Math.min(50, plan.pedidos()));
        int[] stock = articulos.stream().mapToInt(ArticuloGuardado::stock).toArray();

        int creados = 0;
        int conError = 0;
        for (int i = 0; i < plan.pedidos(); i++) {
            List<LineaDePedido> lineas = lineas(articulos, stock, rnd);
            if (lineas.isEmpty()) {
                avance.updateAndGet(a -> a.conAviso("Se acabó el stock de los artículos de prueba antes de "
                        + "completar los pedidos"));
                break;
            }
            TipoPedidoPrueba tipo = TipoPedidoPrueba.elegir(rnd);
            int cliente = clientes.get(rnd.nextInt(clientes.size()));
            String error = null;
            try {
                PedidoCreado pedido = pedidos.crearPedido(tipo, cliente, usuarioId, lineas);
                descontar(articulos, stock, lineas);
                cobrar(tipo, pedido, usuarioId, rnd);
                creados++;
            } catch (RuntimeException e) {
                conError++;
                error = motivo(e);
                log.warn("Datos de prueba: el pedido {} ({}) fallo: {}", i + 1, tipo, error);
            }
            int c = creados;
            int e = conError;
            String err = error;
            avance.updateAndGet(a -> a.conPedidos(c, e, err));

            if (i + 1 == PEDIDOS_DE_MUESTRA && conError > PEDIDOS_DE_MUESTRA / 2) {
                String ultimo = avance.get().ultimoError();
                avance.updateAndGet(a -> a.fallo("fallaron " + e + " de los primeros " + PEDIDOS_DE_MUESTRA
                        + " pedidos. Último error: " + ultimo));
                return false;
            }
        }
        return true;
    }

    /** De 1 a 3 articulos distintos, con 1 o 2 piezas, solo de los que aun tienen stock. */
    private static List<LineaDePedido> lineas(List<ArticuloGuardado> articulos, int[] stock, Random rnd) {
        int cuantas = 1 + rnd.nextInt(3);
        List<LineaDePedido> lineas = new ArrayList<>();
        Set<Integer> usados = new HashSet<>();
        for (int intento = 0; intento < 30 && lineas.size() < cuantas; intento++) {
            int idx = rnd.nextInt(articulos.size());
            if (stock[idx] < 1 || !usados.add(idx)) {
                continue;
            }
            int piezas = Math.min(stock[idx], 1 + rnd.nextInt(2));
            ArticuloGuardado a = articulos.get(idx);
            lineas.add(new LineaDePedido(a.id(), piezas, a.precio()));
        }
        return lineas;
    }

    private static void descontar(List<ArticuloGuardado> articulos, int[] stock, List<LineaDePedido> lineas) {
        for (LineaDePedido l : lineas) {
            for (int i = 0; i < articulos.size(); i++) {
                if (articulos.get(i).id() == l.articuloId()) {
                    stock[i] -= l.cantidad();
                    break;
                }
            }
        }
    }

    /** R9: lo que se cobra despues de crear el pedido, segun su tipo. */
    private void cobrar(TipoPedidoPrueba tipo, PedidoCreado pedido, int usuarioId, Random rnd) {
        switch (tipo) {
            case CONTADO, APARTADO -> { /* contado ya se cobro; el Apartado nace sin dinero */ }
            case APARTADO_PAGADO -> pedidos.abonar(pedido.pedidoId(), pedido.total(), usuarioId);
            case IR_PAGANDO -> irPagando(pedido, usuarioId, rnd);
        }
    }

    /** Enganche del 20 al 50 %, de 0 a 2 abonos mas, y 1 de cada 4 se termina de pagar. */
    private void irPagando(PedidoCreado pedido, int usuarioId, Random rnd) {
        double total = pedido.total();
        double enganche = Math.max(1, Math.floor(total * (0.20 + rnd.nextDouble() * 0.30)));
        pedidos.abonar(pedido.pedidoId(), enganche, usuarioId);
        double resta = total - enganche;

        int abonosMas = rnd.nextInt(3);
        boolean seTerminaDePagar = rnd.nextInt(4) == 0;
        for (int i = 0; i < abonosMas && resta > 2; i++) {
            boolean ultimo = i == abonosMas - 1;
            double abono = ultimo && seTerminaDePagar ? resta : Math.max(1, Math.floor(resta * (0.10 + rnd.nextDouble() * 0.30)));
            pedidos.abonar(pedido.pedidoId(), abono, usuarioId);
            resta -= abono;
        }
        if (seTerminaDePagar && abonosMas == 0 && resta > 0) {
            pedidos.abonar(pedido.pedidoId(), resta, usuarioId);
        }
    }

    private static String motivo(RuntimeException e) {
        String m = e.getMessage();
        return m == null || m.isBlank() ? e.getClass().getSimpleName() : m;
    }
}
