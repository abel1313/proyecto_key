package com.ventas.key.mis.productos.concurrencia;

import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.ICodigoBarrasRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrencia de stock (noche del 2026-11-05). Estas pruebas ejercitan el PATRÓN de bloqueo que usan las ventas, con hilos
 * reales y transacciones reales sobre H2 (no el servicio completo de ventas, que arrastra pagos, tarifas y clientes).
 *
 * <ul>
 *   <li>{@code lasUltimasPiezasSeVendenUnaSolaVez}: el bloqueo de la fila del artículo ({@code findByIdWithLock}) evita vender de más.</li>
 *   <li>{@code con_una_sentencia_atomica_no_se_pierde_ninguna_resta}: restar con UPDATE ... SET stock = stock - 1 en el producto mantiene el total cuadrado.
 *   <li>{@code SIN_refrescar_el_producto_tras_bloquearlo...}: DEMUESTRA que el patrón de {@code PedidoServiceImpl} (bloquear el artículo y luego el producto) puede leer un stock viejo.</li></li>
 *   <li>{@code SIN_bloquear_el_producto_se_pierde_una_resta}: DEMUESTRA el riesgo de {@code VentaServiceImpl.saveVentaDirecta}, que
 *       bloquea el artículo pero resta el stock del producto sin bloquearlo (ver AUDITORIA_SEGURIDAD y propuestas/parches). Si algún día
 *       se corrige el servicio, esta prueba de demostración se puede borrar: no prueba el servicio, prueba el patrón.</li>
 * </ul>
 */
@org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
@org.springframework.test.context.ActiveProfiles("test")
// Sin la transacción envolvente de la prueba: cada hilo abre y confirma la suya, como en producción.
@org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
class StockConcurrenciaTest {

    @Autowired private IProductosRepository productos;
    @Autowired private IVarianteRepository variantes;
    @Autowired private ICodigoBarrasRepository codigos;
    @Autowired private PlatformTransactionManager tm;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;

    private final List<Integer> variantesCreadas = new ArrayList<>();
    private final List<Integer> productosCreados = new ArrayList<>();

    /** La prueba no corre en una transacción que se revierta: se borra lo creado para no dejar datos en la H2 compartida. */
    @org.junit.jupiter.api.AfterEach
    void limpiar() {
        new TransactionTemplate(tm).executeWithoutResult(s -> {
            variantesCreadas.forEach(id -> variantes.deleteById(id));
            productosCreados.forEach(id -> productos.deleteById(id));
        });
        variantesCreadas.clear();
        productosCreados.clear();
    }

    private Producto nuevoProducto(int stock) {
        CodigoBarra cb = new CodigoBarra();
        cb.setCodigoBarras("750" + System.nanoTime());
        cb = codigos.save(cb);
        Producto p = new Producto();
        p.setNombre("Prueba " + System.nanoTime());
        p.setStock(stock);
        p.setHabilitado('1');
        p.setPrecioCosto(10.0);
        p.setPrecioVenta(20.0);
        p.setPrecioRebaja(0.0);
        p.setPiezas(0.0);
        p.setCodigoBarras(cb);
        p.setEsCatalogoInterno(false);
        p = productos.save(p);
        productosCreados.add(p.getId());
        return p;
    }

    private Variantes nuevoArticulo(Producto p, int stock) {
        Variantes v = new Variantes();
        v.setProducto(p);
        v.setStock(stock);
        v = variantes.save(v);
        variantesCreadas.add(v.getId());
        return v;
    }

    private void enParalelo(int hilos, java.util.concurrent.Callable<Void>... tareas) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(hilos);
        try {
            List<Future<Void>> fs = new ArrayList<>();
            for (var t : tareas) fs.add(ex.submit(t));
            for (var f : fs) f.get(30, TimeUnit.SECONDS);
        } finally {
            ex.shutdownNow();
        }
    }

    @Test
    @DisplayName("12 compradoras a la vez por las últimas 3 piezas: se venden exactamente 3")
    void lasUltimasPiezasSeVendenUnaSolaVez() throws Exception {
        Producto p = nuevoProducto(3);
        Variantes v = nuevoArticulo(p, 3);
        AtomicInteger vendidas = new AtomicInteger();
        int n = 12;
        CountDownLatch salida = new CountDownLatch(1);
        List<Callable<Void>> tareas = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            tareas.add(() -> {
                salida.await();
                new TransactionTemplate(tm).executeWithoutResult(s -> {
                    Variantes x = variantes.findByIdWithLock(v.getId()).orElseThrow();
                    if (x.getStock() >= 1) {
                        x.setStock(x.getStock() - 1);
                        variantes.save(x);
                        vendidas.incrementAndGet();
                    }
                });
                return null;
            });
        }
        ExecutorService ex = Executors.newFixedThreadPool(n);
        List<Future<Void>> fs = new ArrayList<>();
        for (var t : tareas) fs.add(ex.submit(t));
        salida.countDown();
        for (var f : fs) f.get(30, TimeUnit.SECONDS);
        ex.shutdownNow();

        assertThat(vendidas.get()).isEqualTo(3);
        assertThat(variantes.findById(v.getId()).orElseThrow().getStock()).isZero();
    }

    @Test
    @DisplayName("dos artículos del mismo producto, con UNA sola sentencia atómica sobre el producto: el total queda cuadrado")
    void con_una_sentencia_atomica_no_se_pierde_ninguna_resta() throws Exception {
        Producto p = nuevoProducto(10);
        Variantes a = nuevoArticulo(p, 5), b = nuevoArticulo(p, 5);
        List<Callable<Void>> tareas = new ArrayList<>();
        for (Variantes v : List.of(a, b, a, b)) {
            tareas.add(() -> {
                new TransactionTemplate(tm).executeWithoutResult(s -> {
                    Variantes x = variantes.findByIdWithLock(v.getId()).orElseThrow();
                    x.setStock(x.getStock() - 1);
                    variantes.save(x);
                    int filas = em.createQuery("update Producto p set p.stock = p.stock - 1 where p.id = :id and p.stock >= 1")
                            .setParameter("id", p.getId()).executeUpdate();
                    assertThat(filas).isEqualTo(1);
                });
                return null;
            });
        }
        enParalelo(4, tareas.toArray(new Callable[0]));
        assertThat(productos.findById(p.getId()).orElseThrow().getStock()).isEqualTo(6);
    }

    @Test
    @DisplayName("DEMUESTRA el riesgo: cargar el artículo (que trae su producto) y DESPUÉS bloquear el producto lee un stock viejo")
    void SIN_refrescar_el_producto_tras_bloquearlo_se_pierde_una_resta() throws Exception {
        // Es el patrón de PedidoServiceImpl: findByIdWithLock(artículo) y luego findByIdWithLock(producto). El artículo ya trajo al producto
        // a la sesión; el bloqueo de la fila espera, pero el objeto que se devuelve es el de la sesión (valor anterior a la espera).
        Producto p = nuevoProducto(10);
        Variantes a = nuevoArticulo(p, 5), b = nuevoArticulo(p, 5);
        CyclicBarrier ambosCargaron = new CyclicBarrier(2);
        List<Callable<Void>> tareas = new ArrayList<>();
        for (Variantes v : List.of(a, b)) {
            tareas.add(() -> {
                new TransactionTemplate(tm).executeWithoutResult(s -> {
                    Variantes x = variantes.findByIdWithLock(v.getId()).orElseThrow();
                    try { ambosCargaron.await(10, TimeUnit.SECONDS); } catch (Exception e) { throw new IllegalStateException(e); }
                    Producto pr = productos.findByIdWithLock(x.getProducto().getId()).orElseThrow();
                    x.setStock(x.getStock() - 1);
                    variantes.save(x);
                    pr.setStock(pr.getStock() - 1);
                    productos.save(pr);
                });
                return null;
            });
        }
        enParalelo(2, tareas.toArray(new Callable[0]));
        assertThat(productos.findById(p.getId()).orElseThrow().getStock()).as("debería ser 8; se perdió una resta").isEqualTo(9);
    }

    @Test
    @DisplayName("DEMUESTRA el riesgo: sin bloquear el producto, dos ventas a la vez de artículos distintos pierden una resta")
    void SIN_bloquear_el_producto_se_pierde_una_resta() throws Exception {
        Producto p = nuevoProducto(10);
        Variantes a = nuevoArticulo(p, 5), b = nuevoArticulo(p, 5);
        CyclicBarrier ambosYaLeyeron = new CyclicBarrier(2);
        List<Callable<Void>> tareas = new ArrayList<>();
        for (Variantes v : List.of(a, b)) {
            tareas.add(() -> {
                new TransactionTemplate(tm).executeWithoutResult(s -> {
                    Variantes x = variantes.findByIdWithLock(v.getId()).orElseThrow();   // como VentaServiceImpl: bloquea el artículo
                    Producto pr = productos.findById(x.getProducto().getId()).orElseThrow();   // ...pero lee el producto sin bloquear
                    try { ambosYaLeyeron.await(10, TimeUnit.SECONDS); } catch (Exception e) { throw new IllegalStateException(e); }
                    x.setStock(x.getStock() - 1);
                    variantes.save(x);
                    pr.setStock(pr.getStock() - 1);
                    productos.save(pr);
                });
                return null;
            });
        }
        enParalelo(2, tareas.toArray(new Callable[0]));
        // Se vendieron 2 piezas; el stock del producto debería ser 8, pero se perdió una resta.
        assertThat(productos.findById(p.getId()).orElseThrow().getStock()).isEqualTo(9);
    }
}
