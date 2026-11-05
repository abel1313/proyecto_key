package com.ventas.key.mis.productos.concurrencia;

import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.ICodigoBarrasRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verificacion (revision C, 2026-11-05) de lo que hace Hibernate 6 con findByIdWithLock sobre una entidad YA gestionada
 * en la sesion, y de si el producto que trae el articulo (ManyToOne EAGER) queda bloqueado. Corre igual en H2 y en MySQL
 * (con SPRING_APPLICATION_JSON apuntando a un MySQL desechable).
 */
@org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
class HibernateBloqueoEntidadGestionadaTest {

    @Autowired private IProductosRepository productos;
    @Autowired private IVarianteRepository variantes;
    @Autowired private ICodigoBarrasRepository codigos;
    @Autowired private PlatformTransactionManager tm;
    @Autowired private JdbcTemplate jdbc;
    @PersistenceContext private EntityManager em;

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
        return productos.save(p);
    }

    private Variantes nuevoArticulo(Producto p, int stock) {
        Variantes v = new Variantes();
        v.setProducto(p);
        v.setStock(stock);
        return variantes.save(v);
    }

    /** Otra transaccion (otro hilo, otra conexion) cambia el stock del producto y confirma. */
    private void otraTransaccionDejaElStockEn(int productoId, int stock) throws Exception {
        Executors.newSingleThreadExecutor().submit(() ->
                new TransactionTemplate(tm).executeWithoutResult(s ->
                        jdbc.update("update producto set stock = ? where id = ?", stock, productoId))).get(10, TimeUnit.SECONDS);
    }

    private int stockEnBdDesdeOtraConexion(int productoId) throws Exception {
        return Executors.newSingleThreadExecutor().submit(() ->
                jdbc.queryForObject("select stock from producto where id = ?", Integer.class, productoId)).get(10, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("findByIdWithLock(producto) despues de cargar el articulo devuelve LA MISMA instancia con el stock viejo")
    void findByIdWithLock_no_refresca_una_entidad_ya_gestionada() throws Exception {
        Producto p = nuevoProducto(10);
        Variantes a = nuevoArticulo(p, 5);
        int[] resultado = new int[3];
        new TransactionTemplate(tm).executeWithoutResult(s -> {
            try {
                Variantes x = variantes.findByIdWithLock(a.getId()).orElseThrow();   // trae al producto (EAGER) con stock 10
                Producto enSesion = x.getProducto();
                otraTransaccionDejaElStockEn(p.getId(), 7);                           // alguien vende 3 y confirma
                Producto bloqueado = productos.findByIdWithLock(p.getId()).orElseThrow(); // SELECT ... FOR UPDATE
                resultado[0] = bloqueado == enSesion ? 1 : 0;
                resultado[1] = bloqueado.getStock();
                resultado[2] = stockEnBdDesdeOtraConexion(p.getId());
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        System.out.printf("### [no_refresca] misma instancia=%s, stock visto=%d, stock en BD=%d%n", resultado[0] == 1, resultado[1], resultado[2]);
        assertThat(resultado[0]).as("misma instancia").isEqualTo(1);
        assertThat(resultado[1]).as("stock que ve el servicio tras bloquear").isEqualTo(10);   // VIEJO
        assertThat(resultado[2]).as("stock real confirmado").isEqualTo(7);
    }

    @Test
    @DisplayName("em.refresh(producto, PESSIMISTIC_WRITE) SI trae lo ultimo confirmado")
    void refresh_con_bloqueo_trae_el_valor_confirmado() throws Exception {
        Producto p = nuevoProducto(10);
        Variantes a = nuevoArticulo(p, 5);
        int[] visto = new int[1];
        new TransactionTemplate(tm).executeWithoutResult(s -> {
            try {
                Variantes x = variantes.findByIdWithLock(a.getId()).orElseThrow();
                otraTransaccionDejaElStockEn(p.getId(), 7);
                em.refresh(x.getProducto(), LockModeType.PESSIMISTIC_WRITE);
                visto[0] = x.getProducto().getStock();
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        System.out.printf("### [refresh] stock visto=%d%n", visto[0]);
        assertThat(visto[0]).isEqualTo(7);
    }

    @Test
    @DisplayName("findById (lectura sin bloqueo) en una transaccion que ya leyo algo: en MySQL REPEATABLE READ ve la foto vieja")
    void findById_sin_bloqueo_tras_otra_lectura() throws Exception {
        Producto p = nuevoProducto(10);
        Variantes a = nuevoArticulo(p, 5);
        int[] visto = new int[2];
        new TransactionTemplate(tm).executeWithoutResult(s -> {
            try {
                // Cualquier lectura consistente abre la "foto" en InnoDB REPEATABLE READ (en el servicio: usuario, cliente, pagos...)
                jdbc.queryForObject("select count(*) from variantes", Integer.class);
                otraTransaccionDejaElStockEn(p.getId(), 7);
                visto[0] = productos.findById(p.getId()).orElseThrow().getStock();            // no estaba en sesion: va a la BD sin FOR UPDATE
                visto[1] = jdbc.queryForObject("select stock from producto where id = ? for update", Integer.class, p.getId());
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        System.out.printf("### [snapshot] findById sin bloqueo vio=%d, SELECT FOR UPDATE vio=%d%n", visto[0], visto[1]);
        assertThat(visto[1]).as("lectura con bloqueo = ultimo confirmado").isEqualTo(7);
    }

    @Test
    @DisplayName("el producto que trae findByIdWithLock(articulo) por EAGER: ¿queda bloqueado su renglon?")
    void bloquear_el_articulo_bloquea_tambien_su_producto() throws Exception {
        Producto p = nuevoProducto(10);
        Variantes a = nuevoArticulo(p, 5);
        CountDownLatch yaBloqueo = new CountDownLatch(1), soltar = new CountDownLatch(1);
        ExecutorService ex = Executors.newFixedThreadPool(2);
        Future<?> duenio = ex.submit(() -> new TransactionTemplate(tm).executeWithoutResult(s -> {
            variantes.findByIdWithLock(a.getId()).orElseThrow();
            yaBloqueo.countDown();
            try { soltar.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) { throw new IllegalStateException(e); }
        }));
        yaBloqueo.await(10, TimeUnit.SECONDS);
        long t0 = System.nanoTime();
        Future<?> otro = ex.submit(() -> new TransactionTemplate(tm).executeWithoutResult(s ->
                jdbc.update("update producto set stock = stock - 1 where id = ?", p.getId())));
        boolean bloqueado;
        try { otro.get(1500, TimeUnit.MILLISECONDS); bloqueado = false; } catch (TimeoutException e) { bloqueado = true; }
        soltar.countDown();
        duenio.get(10, TimeUnit.SECONDS);
        otro.get(10, TimeUnit.SECONDS);
        ex.shutdownNow();
        System.out.printf("### [eager] UPDATE del producto mientras otro tiene bloqueado el articulo: %s (%.0f ms)%n",
                bloqueado ? "ESPERO (producto bloqueado)" : "NO espero (producto libre)", (System.nanoTime() - t0) / 1e6);
    }
}
