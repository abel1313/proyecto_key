package com.ventas.key.mis.productos.repository;

import com.ventas.key.hexagonal.datosprueba.aplicacion.servicio.GenerarDatosPruebaService;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.Avance;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.PlanDeDatos;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.AmbientePort;
import com.ventas.key.hexagonal.datosprueba.infraestructura.salida.pedidos.PedidosPruebaVentaAdapter;
import com.ventas.key.hexagonal.datosprueba.infraestructura.salida.persistencia.AmbienteMysqlAdapter;
import com.ventas.key.hexagonal.datosprueba.infraestructura.salida.persistencia.CatalogoPruebaJdbcAdapter;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.service.AbonoServiceImpl;
import com.ventas.key.mis.productos.service.CacheService;
import com.ventas.key.mis.productos.service.EmailService;
import com.ventas.key.mis.productos.service.PromocionServiceImpl;
import com.ventas.key.mis.productos.service.RestockNotificacionService;
import com.ventas.key.mis.productos.service.VentaServiceImpl;
import com.ventas.key.mis.productos.service.WhatsappService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * El generador de datos de prueba completo contra una base real: SQL por lotes del catalogo, y
 * pedidos por la venta directa y el abono de verdad (R8). Por defecto corre en H2; para correrlo
 * contra MySQL 8 (regla de CLAUDE.md: ningun SQL se entrega sin correrlo) se pasa la URL:
 *
 * <pre>
 * mvn test -Dtest=DatosPruebaIntegracionTest \
 *   -Dspring.datasource.url=jdbc:mysql://localhost/inventario_key_qa \
 *   -Dspring.datasource.username=root -Dspring.datasource.password= \
 *   -Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver \
 *   -Dspring.jpa.hibernate.ddl-auto=none -Dspring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
 * </pre>
 *
 * <p>Cada prueba se deshace al terminar (@DataJpaTest), asi que tampoco deja nada en MySQL.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(JdbcTemplateAutoConfiguration.class)
@Import({CatalogoPruebaJdbcAdapter.class, PedidosPruebaVentaAdapter.class, AmbienteMysqlAdapter.class,
        GenerarDatosPruebaService.class, VentaServiceImpl.class, AbonoServiceImpl.class,
        DatosPruebaIntegracionTest.Config.class})
class DatosPruebaIntegracionTest {

    @TestConfiguration
    static class Config {
        /** La corrida en el mismo hilo de la prueba: asi corre dentro de su transaccion. */
        @Bean("datosPruebaExecutor")
        Executor datosPruebaExecutor() {
            return Runnable::run;
        }

        /** H2 no se llama inventario_key_qa; R1 se prueba aparte con el adaptador real. */
        @Bean
        @Primary
        AmbientePort baseDePruebas() {
            return () -> AmbientePort.BASE_DE_PRUEBAS;
        }
    }

    @Autowired private GenerarDatosPruebaService servicio;
    @Autowired private AmbienteMysqlAdapter ambienteReal;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    @MockBean private PromocionServiceImpl promocionService;
    @MockBean private ErrorGenerico errorGenerico;
    @MockBean private CacheService cacheService;
    @MockBean private RabbitTemplate rabbitTemplate;
    @MockBean private EmailService emailService;
    @MockBean private WhatsappService whatsappService;
    @MockBean private RestockNotificacionService restockNotificacionService;

    private int usuarioYFormaDePago() {
        // Con id 1 a proposito: AbonoServiceImpl registra la venta de un pedido que se termina de
        // pagar con la forma de pago fija PAGOS_EFECTIVO = 1 (en QA la 1 es Efectivo). En MySQL un
        // rollback no regresa el AUTO_INCREMENT, asi que sin el id explicito la segunda corrida
        // quedaba con id 2 y los abonos que liquidan fallaban.
        jdbc.update("INSERT INTO tipo_pago (id, forma_pago) VALUES (1, 'Efectivo')");
        jdbc.update("INSERT INTO meses_intereses (id, meses, descripcion) VALUES (1, '0', 'Contado')");
        jdbc.update("INSERT INTO pagos_y_meses (id, tipo_pago_id, meses_intereses_id) VALUES (1, 1, 1)");

        Usuario u = new Usuario();
        u.setUsername("admin-prueba-" + System.nanoTime());
        u.setPassword("x");
        u.setEnabled(true);
        u.setAceptoPrivacidad(true);
        em.persist(u);
        em.flush();
        return u.getId();
    }

    /** Un producto real con su foto: la que deben reusar los de prueba (R7) y que la baja no toca (R14). */
    private void productoRealConFoto() {
        jdbc.update("INSERT INTO imagenes_copy (id, base_64, extension, nombre_imagen) VALUES (900001, 'real.jpg', 'jpg', 'real')");
        jdbc.update("INSERT INTO codigo_barras (codigo_barras) VALUES ('7501111111111')");
        jdbc.update("""
                INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja,
                                      piezas, stock, habilitado, es_catalogo_interno, codigo_barras_generado,
                                      codigo_barras_id, fecha_creacion)
                SELECT 'Bolsa real', 'Bolsa real', 'Negro', 'Marca Real', 100, 299, 299, 3, 3, '1', 0, 0, id, NOW()
                FROM codigo_barras WHERE codigo_barras = '7501111111111'""");
        jdbc.update("""
                INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado, usar_descuento, fecha_creacion)
                SELECT id, 'Única', 'Negro', 'Bolsa real', 'Marca Real', 3, '1', 0, NOW() FROM producto WHERE nombre = 'Bolsa real'""");
        jdbc.update("""
                INSERT INTO variante_imagen (variante_id, imagen_id, principal)
                SELECT v.id, 900001, 1 FROM variantes v WHERE v.marca = 'Marca Real'""");
    }

    private int contar(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }

    @Test
    void r1_el_adaptador_real_le_pregunta_el_nombre_a_la_base() {
        // En H2 la base se llama TESTDB; en el MySQL de prueba, inventario_key_qa.
        assertThat(ambienteReal.nombreDeLaBase()).isNotBlank();
    }

    @Test
    void genera_catalogo_y_pedidos_con_las_reglas_de_siempre() {
        int usuarioId = usuarioYFormaDePago();
        productoRealConFoto();

        servicio.iniciar(new PlanDeDatos(1_200, 1, 4, 150, 2026L), usuarioId);
        Avance a = servicio.avance();
        assertThat(a.estado()).as("avance: %s", a).isEqualTo(Avance.Estado.TERMINADO);
        assertThat(a.modelosCreados()).isEqualTo(1_200);
        assertThat(a.pedidosCreados()).as("avance: %s", a).isEqualTo(150);
        assertThat(a.pedidosConError()).isZero();

        // R4: todo reconocible por codigo y marca.
        assertThat(contar("SELECT COUNT(*) FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id "
                + "WHERE cb.codigo_barras LIKE '2098_________' AND p.marca = 'Prueba QA'")).isEqualTo(1_200);
        assertThat(contar("SELECT COUNT(*) FROM variantes WHERE marca = 'Prueba QA'")).isEqualTo(a.articulosCreados());

        // R6: el stock del modelo cuadra con sus articulos (despues de vender, por los dos lados igual).
        assertThat(contar("SELECT COUNT(*) FROM producto p WHERE p.marca = 'Prueba QA' AND p.stock <> "
                + "(SELECT SUM(v.stock) FROM variantes v WHERE v.producto_id = p.id)")).isZero();

        // R8/R9: los pedidos los creo la venta directa real.
        Map<String, Object> porTipo = jdbc.queryForMap("""
                SELECT SUM(CASE WHEN tipo_pedido = 'APARTADO' THEN 1 ELSE 0 END) AS apartados,
                       SUM(CASE WHEN tipo_pedido = 'FIADO' THEN 1 ELSE 0 END) AS ir_pagando,
                       SUM(CASE WHEN estado_pedido = 'Entregado' THEN 1 ELSE 0 END) AS contado,
                       SUM(CASE WHEN estado_pedido = 'PAGADO' THEN 1 ELSE 0 END) AS pagados,
                       COUNT(*) AS total
                FROM pedidos WHERE observaciones = '[DATOS DE PRUEBA]'""");
        int total = ((Number) valor(porTipo, "total")).intValue();
        assertThat(total).isEqualTo(150);
        assertThat(((Number) valor(porTipo, "contado")).intValue()).isPositive();
        assertThat(((Number) valor(porTipo, "apartados")).intValue()).isPositive();
        assertThat(((Number) valor(porTipo, "ir_pagando")).intValue()).isPositive();
        assertThat(((Number) valor(porTipo, "pagados")).intValue()).isPositive();

        // Ninguno pagado de mas, y ningun Apartado con dinero a medias (regla 2.1).
        assertThat(contar("SELECT COUNT(*) FROM pedidos WHERE observaciones = '[DATOS DE PRUEBA]' "
                + "AND total_pagado > total_pedido + 0.01")).isZero();
        assertThat(contar("SELECT COUNT(*) FROM pedidos WHERE observaciones = '[DATOS DE PRUEBA]' "
                + "AND tipo_pedido = 'APARTADO' AND total_pagado > 0 AND total_pagado < total_pedido - 0.01")).isZero();
        // Todo Ir pagando trae enganche.
        assertThat(contar("SELECT COUNT(*) FROM pedidos WHERE observaciones = '[DATOS DE PRUEBA]' "
                + "AND tipo_pedido = 'FIADO' AND (total_pagado IS NULL OR total_pagado <= 0)")).isZero();

        // R10: ningun correo ni WhatsApp.
        verify(emailService, never()).enviarTicket(anyString(), anyString(), any());
        verify(whatsappService, never()).enviarMensaje(anyString(), anyString());

        // R7: cada articulo de prueba quedo ligado a la foto real, y cada modelo tambien.
        assertThat(contar("SELECT COUNT(*) FROM variante_imagen vi JOIN variantes v ON v.id = vi.variante_id "
                + "WHERE v.marca = 'Prueba QA' AND vi.imagen_id = 900001")).isEqualTo(a.articulosCreados());
        assertThat(contar("SELECT COUNT(*) FROM producto_imagen_copy pi JOIN producto p ON p.id = pi.producto_id "
                + "WHERE p.marca = 'Prueba QA'")).isEqualTo(1_200);
        assertThat(a.aviso()).isNull();

        // R5: una segunda corrida sigue la numeracion, no repite codigos.
        servicio.iniciar(new PlanDeDatos(300, 1, 2, 0, 7L), usuarioId);
        assertThat(servicio.avance().estado()).isEqualTo(Avance.Estado.TERMINADO);
        assertThat(contar("SELECT COUNT(*) FROM codigo_barras WHERE codigo_barras LIKE '2098_________'")).isEqualTo(1_500);
        assertThat(contar("SELECT COUNT(DISTINCT codigo_barras) FROM codigo_barras WHERE codigo_barras LIKE '2098_________'"))
                .isEqualTo(1_500);

        // R14: dar de baja deja todo con habilitado 0 y sin ligas de imagen; las imagenes no se tocan.
        int articulos = contar("SELECT COUNT(*) FROM variantes WHERE marca = 'Prueba QA'");
        assertThat(servicio.darDeBaja()).isEqualTo(articulos);
        assertThat(contar("SELECT COUNT(*) FROM variantes WHERE marca = 'Prueba QA' AND habilitado = '1'")).isZero();
        assertThat(contar("SELECT COUNT(*) FROM producto WHERE marca = 'Prueba QA' AND habilitado = '1'")).isZero();
        assertThat(contar("SELECT COUNT(*) FROM variante_imagen vi JOIN variantes v ON v.id = vi.variante_id "
                + "WHERE v.marca = 'Prueba QA'")).isZero();
        // El producto real, su articulo, su liga y su foto siguen igual.
        assertThat(contar("SELECT COUNT(*) FROM producto WHERE marca = 'Marca Real' AND habilitado = '1' AND stock = 3")).isEqualTo(1);
        assertThat(contar("SELECT COUNT(*) FROM variantes WHERE marca = 'Marca Real' AND habilitado = '1' AND stock = 3")).isEqualTo(1);
        assertThat(contar("SELECT COUNT(*) FROM variante_imagen WHERE imagen_id = 900001")).isEqualTo(1);
        assertThat(contar("SELECT COUNT(*) FROM imagenes_copy WHERE id = 900001")).isEqualTo(1);
    }

    /**
     * Opcional (tarda): la corrida que va a pedir el boton, 20 000 modelos y 1 000 pedidos, para medir
     * el tiempo. Se activa con -Ddatosprueba.cargaCompleta=true.
     */
    @Test
    @EnabledIfSystemProperty(named = "datosprueba.cargaCompleta", matches = "true")
    void carga_completa_20_mil_modelos_y_1000_pedidos() {
        int usuarioId = usuarioYFormaDePago();
        productoRealConFoto();
        long inicio = System.currentTimeMillis();

        servicio.iniciar(new PlanDeDatos(20_000, 1, 4, 1_000, 2026L), usuarioId);

        Avance a = servicio.avance();
        System.out.printf("CARGA COMPLETA: %d modelos, %d articulos, %d pedidos (%d con error) en %d s%n",
                a.modelosCreados(), a.articulosCreados(), a.pedidosCreados(), a.pedidosConError(),
                (System.currentTimeMillis() - inicio) / 1000);
        assertThat(a.estado()).as("avance: %s", a).isEqualTo(Avance.Estado.TERMINADO);
        assertThat(a.modelosCreados()).isEqualTo(20_000);
        assertThat(a.pedidosCreados()).isEqualTo(1_000);
    }

    private static Object valor(Map<String, Object> fila, String columna) {
        Object v = fila.get(columna);
        return v != null ? v : fila.get(columna.toUpperCase());
    }
}
