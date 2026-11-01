package com.ventas.key.hexagonal.graficasventas.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.graficasventas.dominio.modelo.CategoriaPedido;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.SaldoPendiente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** El SQL de «dinero por cobrar» y «pedidos por estado» contra una base en memoria. */
class PedidosYSaldosJdbcTest {

    private JdbcTemplate jdbc;
    private GraficasJdbcAdapter adaptador;

    @BeforeEach
    void preparar() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:ped" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE clientes (id INT PRIMARY KEY, nombre_persona VARCHAR(60), apeido_paterno VARCHAR(60))");
        jdbc.execute("CREATE TABLE clientes_sin_registro (id INT PRIMARY KEY, nombre_persona VARCHAR(60), apeido_paterno VARCHAR(60))");
        jdbc.execute("CREATE TABLE pedidos (id INT PRIMARY KEY, tipo_pedido VARCHAR(10), estado_pedido VARCHAR(30), total_pedido DOUBLE, "
                + "total_pagado DOUBLE, fecha_pedido DATE, fecha_recogida DATE, entregado BOOLEAN, cliente_id INT NULL, cliente_sin_registro_id INT NULL)");
        jdbc.update("INSERT INTO clientes VALUES (1, 'Ana', 'López')");
        jdbc.update("INSERT INTO clientes_sin_registro VALUES (5, 'Luis', NULL)");
        adaptador = new GraficasJdbcAdapter(jdbc);
    }

    private void pedido(int id, String tipo, String estado, double total, double pagado, String fecha, String recogida,
                        boolean entregado, Integer cliente, Integer sinRegistro) {
        jdbc.update("INSERT INTO pedidos VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", id, tipo, estado, total, pagado,
                java.sql.Date.valueOf(fecha), recogida == null ? null : java.sql.Date.valueOf(recogida), entregado, cliente, sinRegistro);
    }

    @Test
    @DisplayName("por cobrar: solo Apartado e Ir pagando con saldo; con el nombre del cliente, o del cliente sin registro, o «Cliente sin nombre»")
    void saldosPendientes() {
        pedido(1, "FIADO", "FIADO", 400, 100, "2026-10-30", null, true, 1, null);          // saldo 300, Ana López
        pedido(2, "APARTADO", "APARTADO", 200, 0, "2026-09-01", null, false, null, 5);     // saldo 200, Luis (sin registro)
        pedido(3, "FIADO", "FIADO", 100, 100, "2026-10-01", null, true, 1, null);          // saldo 0: no sale
        pedido(4, "NORMAL", "Pendiente", 500, 0, "2026-10-01", null, false, 1, null);      // no es crédito: no sale
        pedido(5, "FIADO", "FIADO", 50, 0, "2026-10-02", null, true, null, null);          // sin cliente: «Cliente sin nombre»
        pedido(6, "FIADO", "PAGADO", 300, 300, "2026-10-03", null, true, 1, null);         // ya pagado: no sale

        List<SaldoPendiente> r = adaptador.saldosPendientes();

        assertThat(r).extracting(SaldoPendiente::pedidoId).containsExactlyInAnyOrder(1, 2, 5);
        assertThat(r).filteredOn(s -> s.pedidoId() == 1).singleElement().satisfies(s -> {
            assertThat(s.cliente()).isEqualTo("Ana López");
            assertThat(s.saldo()).isEqualTo(300.0);
            assertThat(s.tipo()).isEqualTo("FIADO");
            assertThat(s.fechaPedido()).isEqualTo(LocalDate.of(2026, 10, 30));
        });
        assertThat(r).filteredOn(s -> s.pedidoId() == 2).singleElement().satisfies(s -> assertThat(s.cliente()).isEqualTo("Luis"));
        assertThat(r).filteredOn(s -> s.pedidoId() == 5).singleElement().satisfies(s -> assertThat(s.cliente()).isEqualTo("Cliente sin nombre"));
    }

    @Test
    @DisplayName("pedidos por estado: los abiertos se cuentan todos; los terminados solo dentro del periodo")
    void pedidosPorCategoria() {
        pedido(10, "NORMAL", "Pendiente", 100, 0, "2026-10-01", null, false, null, null);        // pendiente
        pedido(18, "NORMAL", "Pendiente", 100, 0, "2025-01-01", null, false, null, null);        // pendiente MUY vieja: abierta, cuenta siempre
        pedido(11, "APARTADO", "APARTADO", 100, 0, "2026-10-01", null, false, null, null);       // apartado
        pedido(12, "FIADO", "FIADO", 100, 20, "2026-10-01", null, true, null, null);             // ir pagando
        pedido(13, "NORMAL", "Entregado", 100, 100, "2026-10-01", null, false, null, null);      // pagado, falta entregar
        pedido(14, "FIADO", "PAGADO", 100, 100, "2026-10-02", null, true, null, null);           // entregado
        pedido(15, "NORMAL", "Entregado", 100, 100, "2026-05-01", null, true, null, null);       // entregado, FUERA del periodo
        pedido(16, "NORMAL", "CANCELADO", 100, 0, "2026-10-05", null, false, null, null);        // cancelado
        pedido(17, "NORMAL", "CANCELADO", 100, 0, "2026-03-01", null, false, null, null);        // cancelado, FUERA del periodo

        Map<CategoriaPedido, Integer> r = adaptador.pedidosPorCategoria(LocalDate.of(2026, 8, 1));

        assertThat(r).containsEntry(CategoriaPedido.PENDIENTE, 2)
                .containsEntry(CategoriaPedido.APARTADO, 1)
                .containsEntry(CategoriaPedido.IR_PAGANDO, 1)
                .containsEntry(CategoriaPedido.PAGADO_POR_ENTREGAR, 1)
                .containsEntry(CategoriaPedido.ENTREGADO, 1)
                .containsEntry(CategoriaPedido.CANCELADO, 1);
    }

    @Test
    @DisplayName("atrasados: sin entregar, sin cancelar y con fecha de recogida ya pasada")
    void atrasados() {
        pedido(10, "NORMAL", "Pendiente", 100, 0, "2026-10-01", "2026-10-20", false, null, null);   // atrasado
        pedido(11, "APARTADO", "APARTADO", 100, 0, "2026-10-01", "2026-11-05", false, null, null);  // aún no es su fecha
        pedido(13, "NORMAL", "Entregado", 100, 100, "2026-10-01", "2026-10-30", false, null, null); // pagado y sin entregar, ya pasó: atrasado
        pedido(14, "FIADO", "PAGADO", 100, 100, "2026-10-02", "2026-10-10", true, null, null);      // ya entregado
        pedido(16, "NORMAL", "CANCELADO", 100, 0, "2026-10-05", "2026-10-06", false, null, null);   // cancelado
        pedido(19, "NORMAL", "Pendiente", 100, 0, "2026-10-05", null, false, null, null);           // sin fecha de recogida
        assertThat(adaptador.pedidosAtrasados(LocalDate.of(2026, 11, 1))).isEqualTo(2);
    }
}
