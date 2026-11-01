package com.ventas.key.hexagonal.revisionventas.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.revisionventas.dominio.excepcion.RevisionVentasException;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.BusquedaDeHallazgos;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.Hallazgo;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.TipoHallazgo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El SQL de verdad contra una base en memoria con las columnas que usan las consultas. Cada regla tiene datos que SÍ
 * deben salir y datos parecidos que NO. (Las diferencias de MySQL se comprueban en QA.)
 */
class HallazgosJdbcAdapterTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 31);

    private JdbcTemplate jdbc;
    private HallazgosJdbcAdapter adaptador;

    @BeforeEach
    void preparar() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:rev" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE pedidos (id INT PRIMARY KEY, estado_pedido VARCHAR(30), tipo_pedido VARCHAR(10), "
                + "total_pedido DOUBLE, total_pagado DOUBLE, fecha_pedido DATE)");
        jdbc.execute("CREATE TABLE detalle_pedidos (id INT PRIMARY KEY, pedido_id INT, sub_total DOUBLE)");
        jdbc.execute("CREATE TABLE ventas (id INT PRIMARY KEY, pedido_id INT NULL, total_venta DOUBLE, fecha_venta TIMESTAMP)");
        jdbc.execute("CREATE TABLE detalle_venta_variantes (id INT PRIMARY KEY, venta_id INT, sub_total DOUBLE)");
        jdbc.execute("CREATE TABLE abono_pedido (id INT PRIMARY KEY, pedido_id INT, monto DOUBLE)");
        jdbc.execute("CREATE TABLE producto (id INT PRIMARY KEY, nombre VARCHAR(80), stock INT, habilitado CHAR(1), es_catalogo_interno TINYINT)");
        jdbc.execute("CREATE TABLE variantes (id INT PRIMARY KEY, producto_id INT, stock INT, habilitado CHAR(1), talla VARCHAR(10), color VARCHAR(30))");

        // Pedidos (fecha dentro de la ventana salvo el 6)
        jdbc.update("INSERT INTO pedidos VALUES (1, 'Entregado', 'NORMAL', 450, 0, '2026-10-10')");     // pagado, con venta: bien
        jdbc.update("INSERT INTO pedidos VALUES (2, 'Entregado', 'NORMAL', 300, 0, '2026-10-12')");     // pagado SIN venta
        jdbc.update("INSERT INTO pedidos VALUES (3, 'PAGADO', 'FIADO', 200, 200, '2026-10-05')");       // pagado a abonos, con venta: bien
        jdbc.update("INSERT INTO pedidos VALUES (4, 'FIADO', 'FIADO', 400, 100, '2026-10-06')");        // 100 = abonos: bien
        jdbc.update("INSERT INTO pedidos VALUES (5, 'FIADO', 'FIADO', 400, 100, '2026-10-07')");        // dice 100, abonos 60: MAL
        jdbc.update("INSERT INTO pedidos VALUES (6, 'Entregado', 'NORMAL', 100, 0, '2026-08-01')");     // sin venta pero fuera de fechas
        jdbc.update("INSERT INTO pedidos VALUES (7, 'CANCELADO', 'FIADO', 999, 50, '2026-10-08')");     // cancelado: no se revisa
        jdbc.update("INSERT INTO pedidos VALUES (8, 'Pendiente', 'NORMAL', 500, 0, '2026-10-09')");     // total 500, artículos 400: MAL
        jdbc.update("INSERT INTO detalle_pedidos VALUES (1, 1, 450), (2, 2, 300), (3, 3, 200), (4, 4, 400), (5, 5, 400), "
                + "(6, 7, 1), (7, 8, 200), (8, 8, 200)");
        jdbc.update("INSERT INTO abono_pedido VALUES (1, 3, 150), (2, 3, 50), (3, 4, 100), (4, 5, 60), (5, 7, 10)");

        // Ventas
        jdbc.update("INSERT INTO ventas VALUES (1, 1, 450, '2026-10-10 12:00:00')");                    // bien
        jdbc.update("INSERT INTO ventas VALUES (2, 3, 200, '2026-10-20 12:00:00')");                    // bien
        jdbc.update("INSERT INTO ventas VALUES (3, NULL, 99, '2026-10-15 12:00:00')");                  // sin pedido
        jdbc.update("INSERT INTO ventas VALUES (4, 4, 500, '2026-10-16 12:00:00')");                    // total 500, artículos 300: MAL
        jdbc.update("INSERT INTO ventas VALUES (5, 6, 77, '2026-09-01 12:00:00')");                     // sin detalle y fuera de fechas
        jdbc.update("INSERT INTO detalle_venta_variantes VALUES (1, 1, 450), (2, 2, 200), (3, 4, 300)");

        // Stock
        jdbc.update("INSERT INTO producto VALUES (1, 'Pantalón', 5, '1', 0)");
        jdbc.update("INSERT INTO producto VALUES (2, 'Great Jeans', 0, '1', 0)");                        // modelo en 0
        jdbc.update("INSERT INTO producto VALUES (3, 'Modelo negativo', -2, '1', 0)");
        jdbc.update("INSERT INTO producto VALUES (4, 'Interno', 0, '1', 1)");                            // catálogo interno: no cuenta
        jdbc.update("INSERT INTO producto VALUES (5, 'Deshabilitado', 0, '0', 0)");                      // modelo apagado: no cuenta
        jdbc.update("INSERT INTO variantes VALUES (10, 1, 2, '1', 'M', 'Rojo')");                        // bien
        jdbc.update("INSERT INTO variantes VALUES (11, 1, -1, '1', 'L', 'Azul')");                       // stock negativo
        jdbc.update("INSERT INTO variantes VALUES (20, 2, 3, '1', '14', 'AZUL MARINO')");                // con stock, modelo en 0: MAL
        jdbc.update("INSERT INTO variantes VALUES (21, 2, 0, '1', '16', 'AZUL MARINO')");                // sin stock: bien
        jdbc.update("INSERT INTO variantes VALUES (22, 2, 4, '0', '18', 'AZUL MARINO')");                // apagado: no cuenta
        jdbc.update("INSERT INTO variantes VALUES (40, 4, 5, '1', 'U', 'Negro')");
        jdbc.update("INSERT INTO variantes VALUES (50, 5, 4, '1', 'U', 'Negro')");

        adaptador = new HallazgosJdbcAdapter(jdbc);
    }

    private BusquedaDeHallazgos buscar(TipoHallazgo tipo) {
        return adaptador.buscar(tipo, DESDE, HASTA, 50);
    }

    @Test
    @DisplayName("pagado sin venta: solo el pedido 2 (el 6 está fuera de fechas, el 1 y el 3 sí tienen venta)")
    void pagadoSinVenta() {
        BusquedaDeHallazgos r = buscar(TipoHallazgo.PAGADO_SIN_VENTA);
        assertThat(r.total()).isEqualTo(1);
        assertThat(r.ejemplos()).extracting(Hallazgo::pedidoId).containsExactly(2);
        assertThat(r.ejemplos().get(0).detalle()).contains("Pedido #2").contains("$300.00");
    }

    @Test
    @DisplayName("el total del pedido no cuadra: solo el 8 (el cancelado y los que sí suman, no)")
    void totalDelPedido() {
        BusquedaDeHallazgos r = buscar(TipoHallazgo.TOTAL_PEDIDO_NO_CUADRA);
        assertThat(r.ejemplos()).extracting(Hallazgo::pedidoId).containsExactly(8);
        assertThat(r.ejemplos().get(0).detalle()).contains("$500.00").contains("$400.00");
    }

    @Test
    @DisplayName("el total de la venta no cuadra: solo la 4 (la 5 no tiene renglones y está fuera de fechas)")
    void totalDeLaVenta() {
        BusquedaDeHallazgos r = buscar(TipoHallazgo.TOTAL_VENTA_NO_CUADRA);
        assertThat(r.ejemplos()).extracting(Hallazgo::ventaId).containsExactly(4);
        assertThat(r.ejemplos().get(0).detalle()).contains("$500.00").contains("$300.00");
    }

    @Test
    @DisplayName("abonos que no cuadran: solo el 5 (el 3 y el 4 sí cuadran; el 7 está cancelado)")
    void abonos() {
        BusquedaDeHallazgos r = buscar(TipoHallazgo.ABONOS_NO_CUADRAN);
        assertThat(r.ejemplos()).extracting(Hallazgo::pedidoId).containsExactly(5);
        assertThat(r.ejemplos().get(0).detalle()).contains("Ir pagando").contains("$100.00").contains("$60.00");
    }

    @Test
    @DisplayName("stock negativo: el artículo 11 y el modelo 3")
    void stockNegativo() {
        BusquedaDeHallazgos r = buscar(TipoHallazgo.STOCK_NEGATIVO);
        assertThat(r.total()).isEqualTo(2);
        assertThat(r.ejemplos()).extracting(Hallazgo::articuloId).containsExactlyInAnyOrder(11, null);
        assertThat(r.ejemplos()).extracting(Hallazgo::detalle)
                .anyMatch(d -> d.contains("artículo #11")).anyMatch(d -> d.contains("modelo #3"));
    }

    @Test
    @DisplayName("artículo con stock y modelo en 0: solo Great Jeans talla 14 (no el sin stock, el apagado, el interno ni el modelo apagado)")
    void articuloConStockSinModelo() {
        BusquedaDeHallazgos r = buscar(TipoHallazgo.ARTICULO_CON_STOCK_SIN_MODELO);
        assertThat(r.total()).isEqualTo(1);
        assertThat(r.ejemplos()).extracting(Hallazgo::articuloId).containsExactly(20);
        assertThat(r.ejemplos().get(0).detalle()).contains("Great Jeans talla 14 AZUL MARINO").contains("tiene 3 pieza").contains("modelo #2 tiene 0");
    }

    @Test
    @DisplayName("venta sin pedido: solo la 3")
    void ventaSinPedido() {
        BusquedaDeHallazgos r = buscar(TipoHallazgo.VENTA_SIN_PEDIDO);
        assertThat(r.ejemplos()).extracting(Hallazgo::ventaId).containsExactly(3);
    }

    @Test
    @DisplayName("el total es el real aunque los ejemplos se limiten")
    void limiteDeEjemplos() {
        for (int i = 100; i < 105; i++) {
            jdbc.update("INSERT INTO ventas VALUES (?, NULL, 10, '2026-10-15 12:00:00')", i);
        }
        BusquedaDeHallazgos r = adaptador.buscar(TipoHallazgo.VENTA_SIN_PEDIDO, DESDE, HASTA, 2);
        assertThat(r.total()).isEqualTo(6);
        assertThat(r.ejemplos()).hasSize(2);
        assertThat(r.ejemplos()).extracting(Hallazgo::ventaId).containsExactly(104, 103);   // los más recientes primero
    }

    @Test
    @DisplayName("sin la tabla de cobros con tarjeta (producción sin migración) ese tipo no se puede revisar, sin tumbar lo demás")
    void sinTablaDeTarjeta() {
        assertThatThrownBy(() -> buscar(TipoHallazgo.COBRO_TARJETA_SIN_APLICAR))
                .isInstanceOf(RevisionVentasException.RevisionNoDisponible.class);
    }

    @Test
    @DisplayName("cobros con tarjeta: sin aplicar, y aprobado con pedido que no quedó pagado; no el aprobado bien aplicado")
    void cobrosConTarjeta() {
        jdbc.execute("CREATE TABLE pago_tarjeta_intento (id INT PRIMARY KEY, pedido_id INT, monto DECIMAL(12,2), estado VARCHAR(20))");
        jdbc.update("INSERT INTO pago_tarjeta_intento VALUES (1, 2, 300.00, 'COBRADO_SIN_APLICAR')");   // mal
        jdbc.update("INSERT INTO pago_tarjeta_intento VALUES (2, 8, 500.00, 'APROBADO')");              // aprobado y pedido Pendiente: mal
        jdbc.update("INSERT INTO pago_tarjeta_intento VALUES (3, 1, 450.00, 'APROBADO')");              // aprobado y pedido Entregado: bien
        jdbc.update("INSERT INTO pago_tarjeta_intento VALUES (4, 8, 500.00, 'RECHAZADO')");             // rechazado: no es problema
        BusquedaDeHallazgos r = buscar(TipoHallazgo.COBRO_TARJETA_SIN_APLICAR);
        assertThat(r.total()).isEqualTo(2);
        assertThat(r.ejemplos()).extracting(Hallazgo::pedidoId).containsExactlyInAnyOrder(2, 8);
    }

    @Test
    @DisplayName("todas las consultas son de solo lectura: no cambian ningún dato")
    void soloLee() {
        int antes = jdbc.queryForObject("SELECT COUNT(*) FROM pedidos", Integer.class)
                + jdbc.queryForObject("SELECT COUNT(*) FROM ventas", Integer.class)
                + jdbc.queryForObject("SELECT COALESCE(SUM(stock), 0) FROM variantes", Integer.class);
        for (TipoHallazgo tipo : TipoHallazgo.values()) {
            try {
                buscar(tipo);
            } catch (RevisionVentasException.RevisionNoDisponible ignorado) {
                // el cobro con tarjeta no tiene tabla en esta base de prueba
            }
        }
        int despues = jdbc.queryForObject("SELECT COUNT(*) FROM pedidos", Integer.class)
                + jdbc.queryForObject("SELECT COUNT(*) FROM ventas", Integer.class)
                + jdbc.queryForObject("SELECT COALESCE(SUM(stock), 0) FROM variantes", Integer.class);
        assertThat(despues).isEqualTo(antes);
        assertThat(List.of(TipoHallazgo.values())).hasSize(8);
    }
}
