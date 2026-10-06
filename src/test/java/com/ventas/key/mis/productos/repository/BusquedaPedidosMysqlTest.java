package com.ventas.key.mis.productos.repository;

import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.CuandoSeEntrega;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.EstadoBuscado;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.FiltroPedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.FormaDeCobro;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.ModoDeEntrega;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.OrdenDePedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.PaginaDePedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.PedidosUnidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.RangoDeFechas;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.SituacionDeDinero;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.TextoBuscado;
import com.ventas.key.hexagonal.busquedapedido.infraestructura.salida.persistencia.PedidosFiltradosJdbcAdapter;
import com.ventas.key.hexagonal.busquedapedido.infraestructura.salida.persistencia.TarjetasDePedidoLector;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.entrada.ConsultarGruposCasoUso;
import com.ventas.key.mis.productos.models.pedidos.PedidoGenerico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Los filtros de la lista de pedidos (dominio busquedapedido) contra MySQL 8 con el esquema real:
 * el SQL es nativo de MySQL (JSON_OBJECT, LIKE sin acentos), asi que en H2 no corre. Regla de
 * CLAUDE.md: ningun SQL se entrega sin correrlo. Se corre asi:
 *
 * <pre>
 * mvn test -Dtest=BusquedaPedidosMysqlTest \
 *   -Dspring.datasource.url=jdbc:mysql://localhost/inventario_key_qa \
 *   -Dspring.datasource.username=root -Dspring.datasource.password= \
 *   -Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver \
 *   -Dspring.jpa.hibernate.ddl-auto=none -Dspring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
 * </pre>
 *
 * <p>Todo lo que inserta se deshace al terminar cada prueba (@DataJpaTest). Los ids empiezan en
 * 900001 y cada prueba mira solo esos, por si la base ya trae pedidos.
 *
 * <p>Los pedidos de prueba ("hoy" = 6 de octubre de 2026):
 * <pre>
 * #   forma       estado      cliente            total pagado registro       entrega  otros
 * P1  Contado     Pendiente   Maria Lopez         100    0    01/10 10:00    06/10    envio a Zacazonapan
 * P2  Contado     Entregado   Rosa Perez (s/r)    200  200    02/10          02/10    articulo Pantalon
 * P3  Apartado    APARTADO    Maria Lopez         300    0    03/10          03/10    (atrasado)
 * P4  Ir pagando  FIADO       Rosa Perez (s/r)    500  100    04/10 09:00    07/10    recibe Juana; recoge en tienda; 1 abono
 * P5  Ir pagando  PAGADO      Maria Lopez         100  150    04/10 18:00             saldo a favor
 * P6  Apartado    cancelado   Maria Lopez         200   50    05/10 10:00             cancelado con dinero
 * P7  Ir pagando  FIADO       Maria Lopez         100    0    05/10 12:00             titular de grupo; promocion
 * P8  Ir pagando  FIADO       Rosa Perez (s/r)    100    0    05/10 13:00             miembro del grupo (oculto)
 * P9  Contado     Pendiente   Flor Ramirez (s/r)  150    0    06/10 08:00             ramo de flores
 * </pre>
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration({JdbcTemplateAutoConfiguration.class, JacksonAutoConfiguration.class})
@Import({PedidosFiltradosJdbcAdapter.class, TarjetasDePedidoLector.class})
@EnabledIfSystemProperty(named = "spring.datasource.url", matches = "jdbc:mysql:.*")
class BusquedaPedidosMysqlTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 6);
    private static final int P1 = 900001, P2 = 900002, P3 = 900003, P4 = 900004, P5 = 900005,
            P6 = 900006, P7 = 900007, P8 = 900008, P9 = 900009;

    @Autowired private PedidosFiltradosJdbcAdapter adapter;
    @Autowired private TarjetasDePedidoLector lector;
    @Autowired private JdbcTemplate jdbc;

    @MockBean private ConsultarGruposCasoUso consultarGrupos;

    @BeforeEach
    void datos() {
        when(consultarGrupos.gruposActivosDe(any())).thenReturn(java.util.Map.of());

        jdbc.update("INSERT INTO codigo_barras (id, codigo_barras) VALUES (900001,'7509990000001'),(900002,'7509990000002')");
        jdbc.update("""
                INSERT INTO producto (id, nombre, habilitado, es_catalogo_interno, piezas, precio_costo, precio_rebaja,
                                      precio_venta, stock, codigo_barras_id)
                VALUES (900001,'Blusa lino','1',0,0,0,0,100,10,900001),
                       (900002,'Pantalon mezclilla','1',0,0,0,0,200,10,900002)""");
        jdbc.update("INSERT INTO variantes (id, producto_id, stock, habilitado) VALUES (900001,900001,10,'1'),(900002,900002,10,'1')");
        jdbc.update("""
                INSERT INTO clientes (id, nombre_persona, apeido_paterno, numero_telefonico, correo_electronico,
                                      recibir_correos, recibir_promociones)
                VALUES (900001,'María López','López','5512345678','maria@correo.com',0,0)""");
        jdbc.update("""
                INSERT INTO clientes_sin_registro (id, nombre_persona, numero_telefonico)
                VALUES (900001,'Rosa Pérez','5598765432'),(900002,'Flor Ramírez','5511112222')""");
        jdbc.update("INSERT INTO lugares_entrega (id, nombre, es_recoger_en_tienda) VALUES (900001,'Zacazonapan',0),(900002,'Tienda centro',1)");
        jdbc.update("INSERT INTO promociones (id, descripcion, activo, fecha_vencimiento) VALUES (900001,'2x1 blusas',1,'2030-01-01')");
        jdbc.update("INSERT INTO tipo_flor (id, nombre, activo, precio_por_flor) VALUES (900001,'Rosa eterna',1,10)");

        pedido(P1, "NORMAL", "Pendiente", 900001, null, 100, 0, "2026-10-01 10:00:00", "2026-10-06", 900001, null);
        pedido(P2, "NORMAL", "Entregado", null, 900001, 200, 200, "2026-10-02 10:00:00", "2026-10-02", null, null);
        pedido(P3, "APARTADO", "APARTADO", 900001, null, 300, 0, "2026-10-03 10:00:00", "2026-10-03", null, null);
        pedido(P4, "FIADO", "FIADO", null, 900001, 500, 100, "2026-10-04 09:00:00", "2026-10-07", 900002, "Juana Receptora");
        pedido(P5, "FIADO", "PAGADO", 900001, null, 100, 150, "2026-10-04 18:00:00", null, null, null);
        pedido(P6, "APARTADO", "cancelado", 900001, null, 200, 50, "2026-10-05 10:00:00", null, null, null);
        pedido(P7, "FIADO", "FIADO", 900001, null, 100, 0, "2026-10-05 12:00:00", null, null, null);
        pedido(P8, "FIADO", "FIADO", null, 900001, 100, 0, "2026-10-05 13:00:00", null, null, null);
        pedido(P9, "NORMAL", "Pendiente", null, 900002, 150, 0, "2026-10-06 08:00:00", null, null, null);

        for (int id : List.of(P1, P3, P4, P5, P6, P8, P9)) {
            linea(id, 900001, null);
        }
        linea(P2, 900002, null);
        linea(P7, 900001, 900001);

        jdbc.update("INSERT INTO abono_pedido (pedido_id, monto, fecha_pago, metodo_pago) VALUES (?,100,'2026-10-04','EFECTIVO')", P4);
        jdbc.update("INSERT INTO abono_pedido (pedido_id, monto, fecha_pago, metodo_pago) VALUES (?,150,'2026-10-04','EFECTIVO')", P5);
        jdbc.update("INSERT INTO abono_pedido (pedido_id, monto, fecha_pago, metodo_pago) VALUES (?,50,'2026-10-05','EFECTIVO')", P6);

        jdbc.update("INSERT INTO grupo_pedido (id, activo, fecha_creacion, pedido_titular_id) VALUES (900001,1,NOW(),?)", P7);
        jdbc.update("INSERT INTO grupo_pedido_miembro (grupo_id, pedido_id) VALUES (900001,?),(900001,?)", P7, P8);

        jdbc.update("""
                INSERT INTO ramo_pedido_detalle (pedido_id, tipo_flor_id, cantidad_final, anticipo_pagado, anticipo_requerido,
                                                 cargo_urgente_aplicado, es_urgente, fecha_creacion, frase_liston_estado,
                                                 recoger_en_local)
                VALUES (?,900001,12,0,0,0,0,NOW(),'SIN_FRASE',1)""", P9);
    }

    private void pedido(int id, String tipo, String estado, Integer cliente, Integer sinRegistro, double total,
                        double pagado, String registro, String recogida, Integer lugar, String receptor) {
        jdbc.update("""
                INSERT INTO pedidos (id, tipo_pedido, estado_pedido, cliente_id, cliente_sin_registro_id, total_pedido,
                                     total_pagado, fecha_hora_registro, fecha_pedido, fecha_recogida, lugar_entrega_id,
                                     nombre_receptor)
                VALUES (?,?,?,?,?,?,?,?,DATE(?),?,?,?)""",
                id, tipo, estado, cliente, sinRegistro, total, pagado, registro, registro, recogida, lugar, receptor);
    }

    private void linea(int pedido, int producto, Integer promocion) {
        jdbc.update("""
                INSERT INTO detalle_pedidos (pedido_id, producto_id, variante_id, cantidad, precio_unitario, sub_total, promocion_id)
                VALUES (?,?,?,1,100,100,?)""", pedido, producto, producto, promocion);
    }

    // ── Ayudas ────────────────────────────────────────────────────────────────────────────

    /** Un filtro con todo vacio; cada prueba cambia lo que necesita. */
    private record F(String texto, Set<FormaDeCobro> formas, Set<EstadoBuscado> estados, Set<SituacionDeDinero> dinero,
                     Double min, Double max, RangoDeFechas registro, CuandoSeEntrega entrega, Integer lugar,
                     ModoDeEntrega modo, PedidosUnidos unidos, boolean conRamos, boolean conPromo, OrdenDePedidos orden) {
        static F nada() {
            return new F(null, Set.of(), Set.of(), Set.of(), null, null, null, null, null, null, null, false, false, null);
        }
        F texto(String t) { return new F(t, formas, estados, dinero, min, max, registro, entrega, lugar, modo, unidos, conRamos, conPromo, orden); }
        F formas(FormaDeCobro... v) { return new F(texto, Set.of(v), estados, dinero, min, max, registro, entrega, lugar, modo, unidos, conRamos, conPromo, orden); }
        F estados(EstadoBuscado... v) { return new F(texto, formas, Set.of(v), dinero, min, max, registro, entrega, lugar, modo, unidos, conRamos, conPromo, orden); }
        F dinero(SituacionDeDinero... v) { return new F(texto, formas, estados, Set.of(v), min, max, registro, entrega, lugar, modo, unidos, conRamos, conPromo, orden); }
        F total(Double a, Double b) { return new F(texto, formas, estados, dinero, a, b, registro, entrega, lugar, modo, unidos, conRamos, conPromo, orden); }
        F registro(LocalDate a, LocalDate b) { return new F(texto, formas, estados, dinero, min, max, new RangoDeFechas(a, b), entrega, lugar, modo, unidos, conRamos, conPromo, orden); }
        F entrega(CuandoSeEntrega e) { return new F(texto, formas, estados, dinero, min, max, registro, e, lugar, modo, unidos, conRamos, conPromo, orden); }
        F lugar(Integer l) { return new F(texto, formas, estados, dinero, min, max, registro, entrega, l, modo, unidos, conRamos, conPromo, orden); }
        F modo(ModoDeEntrega m) { return new F(texto, formas, estados, dinero, min, max, registro, entrega, lugar, m, unidos, conRamos, conPromo, orden); }
        F unidos(PedidosUnidos u) { return new F(texto, formas, estados, dinero, min, max, registro, entrega, lugar, modo, u, conRamos, conPromo, orden); }
        F ramos() { return new F(texto, formas, estados, dinero, min, max, registro, entrega, lugar, modo, unidos, true, conPromo, orden); }
        F promo() { return new F(texto, formas, estados, dinero, min, max, registro, entrega, lugar, modo, unidos, conRamos, true, orden); }
        F orden(OrdenDePedidos o) { return new F(texto, formas, estados, dinero, min, max, registro, entrega, lugar, modo, unidos, conRamos, conPromo, o); }

        FiltroPedidos filtro(int pagina, int tamano) {
            return new FiltroPedidos(TextoBuscado.de(texto), formas, estados, dinero, min, max, registro, entrega, lugar,
                    modo, unidos, conRamos, conPromo, orden, pagina, tamano);
        }
    }

    /** Los pedidos de prueba que salen, en orden. */
    private List<Integer> ids(F f) {
        return adapter.buscar(f.filtro(0, 50), HOY).pedidoIds().stream().filter(id -> id >= 900001 && id <= 900009).toList();
    }

    // ── Pruebas ───────────────────────────────────────────────────────────────────────────

    @Test
    void r9_r11_sin_filtros_salen_todos_menos_el_miembro_del_grupo_del_mas_nuevo_al_mas_viejo() {
        assertThat(ids(F.nada())).containsExactly(P9, P7, P6, P5, P4, P3, P2, P1);
    }

    @Test
    void r1_busca_por_nombre_sin_acentos_ni_mayusculas() {
        assertThat(ids(F.nada().texto("maria"))).containsExactly(P7, P6, P5, P3, P1);
        assertThat(ids(F.nada().texto("ROSA PEREZ"))).containsExactly(P4, P2);
    }

    @Test
    void r1_busca_por_quien_recibe_telefono_correo_y_articulo() {
        assertThat(ids(F.nada().texto("Juana"))).containsExactly(P4);
        assertThat(ids(F.nada().texto("98765"))).containsExactly(P4, P2);
        assertThat(ids(F.nada().texto("maria@correo"))).containsExactly(P7, P6, P5, P3, P1);
        assertThat(ids(F.nada().texto("pantal"))).containsExactly(P2);
        assertThat(ids(F.nada().texto("7509990000002"))).containsExactly(P2);
    }

    @Test
    void r1_r9_el_numero_exacto_abre_hasta_al_miembro_oculto_del_grupo() {
        assertThat(ids(F.nada().texto("900008"))).containsExactly(P8);
        assertThat(ids(F.nada().texto("#900003"))).containsExactly(P3);
    }

    @Test
    void r1_el_porcentaje_no_es_comodin() {
        assertThat(ids(F.nada().texto("50%"))).isEmpty();
    }

    @Test
    void r2_forma_de_cobro() {
        assertThat(ids(F.nada().formas(FormaDeCobro.IR_PAGANDO))).containsExactly(P7, P5, P4);
        assertThat(ids(F.nada().formas(FormaDeCobro.CONTADO, FormaDeCobro.APARTADO))).containsExactly(P9, P6, P3, P2, P1);
    }

    @Test
    void r3_estados_como_los_dice_la_card() {
        assertThat(ids(F.nada().estados(EstadoBuscado.PENDIENTE))).containsExactly(P9, P1);
        assertThat(ids(F.nada().estados(EstadoBuscado.POR_COBRAR))).containsExactly(P7, P4, P3);
        assertThat(ids(F.nada().estados(EstadoBuscado.PAGADO))).containsExactly(P5);
        assertThat(ids(F.nada().estados(EstadoBuscado.ENTREGADO))).containsExactly(P2);
        assertThat(ids(F.nada().estados(EstadoBuscado.CANCELADO))).containsExactly(P6);
    }

    @Test
    void r4_dinero() {
        assertThat(ids(F.nada().dinero(SituacionDeDinero.CON_SALDO))).containsExactly(P7, P4, P3);
        assertThat(ids(F.nada().dinero(SituacionDeDinero.SIN_ABONOS))).containsExactly(P7, P3);
        assertThat(ids(F.nada().dinero(SituacionDeDinero.SALDO_A_FAVOR))).containsExactly(P6, P5);
    }

    @Test
    void r4_un_ir_pagando_cancelado_que_debia_no_es_saldo_a_favor_y_uno_pagado_si() {
        pedido(900010, "FIADO", "cancelado", 900001, null, 100, 40, "2026-10-05 15:00:00", null, null, null);   // incobrable
        pedido(900011, "FIADO", "cancelado", 900001, null, 100, 100, "2026-10-05 16:00:00", null, null, null);  // devolucion
        linea(900010, 900001, null);
        linea(900011, 900001, null);

        List<Integer> aFavor = adapter.buscar(F.nada().dinero(SituacionDeDinero.SALDO_A_FAVOR).filtro(0, 50), HOY)
                .pedidoIds().stream().filter(id -> id >= 900001 && id <= 900011).toList();
        assertThat(aFavor).containsExactly(900011, P6, P5);
    }

    @Test
    void r5_rango_de_total() {
        assertThat(ids(F.nada().total(200.0, 300.0))).containsExactly(P6, P3, P2);
        assertThat(ids(F.nada().total(400.0, null))).containsExactly(P4);
    }

    @Test
    void r6_dia_de_registro_incluye_todo_el_dia() {
        LocalDate cuatro = LocalDate.of(2026, 10, 4);
        assertThat(ids(F.nada().registro(cuatro, cuatro))).containsExactly(P5, P4);
    }

    @Test
    void r7_fecha_de_entrega_solo_de_lo_que_espera_entrega() {
        assertThat(ids(F.nada().entrega(CuandoSeEntrega.HOY))).containsExactly(P1);
        assertThat(ids(F.nada().entrega(CuandoSeEntrega.MANANA))).containsExactly(P4);
        assertThat(ids(F.nada().entrega(CuandoSeEntrega.ESTA_SEMANA))).containsExactly(P4, P1);
        // P2 tambien tiene fecha pasada, pero ya se entrego: no esta atrasado.
        assertThat(ids(F.nada().entrega(CuandoSeEntrega.ATRASADOS))).containsExactly(P3);
    }

    @Test
    void r8_lugar_y_modo_de_entrega() {
        assertThat(ids(F.nada().lugar(900001))).containsExactly(P1);
        assertThat(ids(F.nada().modo(ModoDeEntrega.ENVIO))).containsExactly(P1);
        assertThat(ids(F.nada().modo(ModoDeEntrega.RECOGE_EN_TIENDA))).containsExactly(P9, P7, P6, P5, P4, P3, P2);
    }

    @Test
    void r9_unidos_y_sin_unir() {
        assertThat(ids(F.nada().unidos(PedidosUnidos.SOLO_UNIDOS))).containsExactly(P7);
        assertThat(ids(F.nada().unidos(PedidosUnidos.SIN_UNIR))).containsExactly(P9, P6, P5, P4, P3, P2, P1);
    }

    @Test
    void r10_ramos_y_promociones() {
        assertThat(ids(F.nada().ramos())).containsExactly(P9);
        assertThat(ids(F.nada().promo())).containsExactly(P7);
    }

    @Test
    void r13_los_filtros_se_combinan_con_y() {
        assertThat(ids(F.nada().texto("rosa").formas(FormaDeCobro.IR_PAGANDO))).containsExactly(P4);
        assertThat(ids(F.nada().texto("maria").estados(EstadoBuscado.POR_COBRAR).dinero(SituacionDeDinero.SIN_ABONOS)))
                .containsExactly(P7, P3);
    }

    @Test
    void r11_ordenes() {
        assertThat(ids(F.nada().orden(OrdenDePedidos.ANTIGUOS))).containsExactly(P1, P2, P3, P4, P5, P6, P7, P9);
        assertThat(ids(F.nada().orden(OrdenDePedidos.ENTREGA_PROXIMA))).containsExactly(P2, P3, P1, P4, P9, P7, P6, P5);
        assertThat(ids(F.nada().orden(OrdenDePedidos.MAYOR_SALDO)).subList(0, 3)).containsExactly(P4, P3, P9);
    }

    @Test
    void r12_las_paginas_no_repiten_ni_brincan() {
        // El rango de registro de los datos de prueba, por si la base ya trae otros pedidos.
        F rango = F.nada().registro(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 6));
        PaginaDePedidos uno = adapter.buscar(rango.filtro(0, 3), HOY);
        PaginaDePedidos dos = adapter.buscar(rango.filtro(1, 3), HOY);
        PaginaDePedidos tres = adapter.buscar(rango.filtro(2, 3), HOY);

        assertThat(uno.totalRegistros()).isGreaterThanOrEqualTo(8);
        assertThat(uno.pedidoIds()).hasSize(3);
        assertThat(dos.pedidoIds()).doesNotContainAnyElementsOf(uno.pedidoIds());
        assertThat(tres.pedidoIds()).doesNotContainAnyElementsOf(uno.pedidoIds()).doesNotContainAnyElementsOf(dos.pedidoIds());
    }

    @Test
    void la_card_sale_con_el_mismo_json_y_en_el_orden_pedido() {
        List<PedidoGenerico> cards = lector.tarjetas(List.of(P4, P1));

        assertThat(cards).extracting(c -> c.getPedido().getId()).containsExactly(P4, P1);
        assertThat(cards.get(0).getCliente().getNombreCliente()).isEqualTo("Rosa Pérez");
        assertThat(cards.get(0).getPedido().getTipoPedido()).isEqualTo("FIADO");
        assertThat(cards.get(1).getPedido().getLugarEntregaNombre()).isEqualTo("Zacazonapan");
    }
}
