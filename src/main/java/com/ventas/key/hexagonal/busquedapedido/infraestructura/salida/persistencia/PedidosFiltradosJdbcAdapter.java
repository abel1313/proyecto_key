package com.ventas.key.hexagonal.busquedapedido.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.EstadoBuscado;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.CuandoSeEntrega;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.FiltroPedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.FormaDeCobro;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.PaginaDePedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.RangoDeFechas;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.SituacionDeDinero;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.TextoBuscado;
import com.ventas.key.hexagonal.busquedapedido.dominio.puerto.salida.PedidosFiltradosPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 *
 * <p>Arma el SQL con <b>solo</b> las condiciones de los filtros que vienen. Con un query fijo de
 * {@code (:x IS NULL OR ...)} por cada uno de los 13 filtros MySQL no puede usar indices y cada
 * filtro nuevo alarga una consulta que ya era dificil de leer. Los valores siempre van como
 * parametros (nunca pegados al texto), asi que armar el texto no abre inyeccion de SQL.
 *
 * <p>Regresa solo los numeros de pedido de la pagina; la card la arma {@link TarjetasDePedidoLector}.
 *
 * <p><b>R14 — un pedido unido se filtra por lo que muestra su card.</b> De un grupo activo solo sale
 * el titular, y su card muestra el total, lo pagado y lo que falta <i>de todo el grupo</i>. Por eso
 * el estado, el dinero, el total, la fecha de entrega y el orden "los que mas deben" del titular se
 * calculan con el grupo ({@code grp}), y el texto, los ramos y las promociones buscan en todos sus
 * pedidos. Antes se miraba solo al titular: como un abono al grupo liquida primero al pedido mas
 * viejo, el titular quedaba Pagado y el grupo desaparecia de "Por cobrar" aunque siguiera debiendo.
 *
 * <p>Valores reales de {@code estado_pedido} (se guardan con mayusculas distintas, por eso se compara
 * con UPPER): {@code Pendiente}, {@code Entregado}, {@code APARTADO}, {@code FIADO}, {@code PAGADO},
 * {@code cancelado}.
 */
@Component
@RequiredArgsConstructor
public class PedidosFiltradosJdbcAdapter implements PedidosFiltradosPort {

    private static final String ESTADO_P = "UPPER(COALESCE(p.estado_pedido,''))";

    /** Apartado o Ir pagando que todavia no se liquida ni se cancela. */
    private static final String CREDITO_ABIERTO = "(p.tipo_pedido IN ('APARTADO','FIADO')"
            + " AND " + ESTADO_P + " NOT IN ('PAGADO','CANCELADO','ENTREGADO'))";

    private static final String SALDO = "(COALESCE(p.total_pedido,0) - COALESCE(p.total_pagado,0))";

    /**
     * Los numeros de los grupos activos, por titular. Mismas cuentas que {@code GrupoPedidos} (lo
     * que muestra la card): los cancelados no suman total ni deuda, y un contado Entregado ya no
     * debe aunque su totalPagado siga en 0 (R13 de grupopedido).
     */
    private static final String GRUPOS = """
            LEFT JOIN (
              SELECT g.pedido_titular_id AS titular_id,
                SUM(CASE WHEN UPPER(COALESCE(pm.estado_pedido,'')) NOT IN ('PAGADO','CANCELADO','ENTREGADO') THEN 1 ELSE 0 END) AS abiertos,
                MAX(CASE WHEN UPPER(COALESCE(pm.estado_pedido,'')) NOT IN ('PAGADO','CANCELADO','ENTREGADO')
                          AND pm.tipo_pedido IN ('APARTADO','FIADO') THEN 1 ELSE 0 END) AS credito_abierto,
                SUM(CASE WHEN UPPER(COALESCE(pm.estado_pedido,'')) = 'PAGADO' THEN 1 ELSE 0 END) AS pagados,
                SUM(CASE WHEN UPPER(COALESCE(pm.estado_pedido,'')) = 'ENTREGADO' THEN 1 ELSE 0 END) AS entregados,
                SUM(CASE WHEN UPPER(COALESCE(pm.estado_pedido,'')) = 'CANCELADO' THEN 0
                         ELSE COALESCE(pm.total_pedido,0) END) AS total,
                SUM(CASE WHEN UPPER(COALESCE(pm.estado_pedido,'')) IN ('CANCELADO','ENTREGADO') THEN 0
                         ELSE GREATEST(0, COALESCE(pm.total_pedido,0) - COALESCE(pm.total_pagado,0)) END) AS saldo,
                SUM(CASE WHEN UPPER(COALESCE(pm.estado_pedido,'')) = 'CANCELADO' THEN 0
                         ELSE (SELECT COUNT(*) FROM abono_pedido ag WHERE ag.pedido_id = pm.id) END) AS abonos,
                MAX(CASE WHEN %s THEN 1 ELSE 0 END) AS a_favor,
                SUM(CASE WHEN UPPER(COALESCE(pm.estado_pedido,'')) = 'CANCELADO' THEN 0 ELSE 1 END) AS vivos,
                SUM(CASE WHEN UPPER(COALESCE(pm.estado_pedido,'')) <> 'CANCELADO' AND pm.entregado = 0
                         THEN 1 ELSE 0 END) AS sin_entregar
              FROM grupo_pedido g
              JOIN grupo_pedido_miembro gm ON gm.grupo_id = g.id
              JOIN pedidos pm ON pm.id = gm.pedido_id
              WHERE g.activo = 1
              GROUP BY g.pedido_titular_id
            ) grp ON grp.titular_id = p.id""".formatted(saldoAFavor("pm"));

    /** R3 con las palabras de la card. Un titular toma el estado de su grupo (R14). */
    private static final String ESTADO_CARD = "(CASE"
            + " WHEN grp.titular_id IS NOT NULL THEN (CASE"
            + "   WHEN grp.abiertos > 0 THEN (CASE WHEN grp.credito_abierto = 1 THEN 'POR_COBRAR' ELSE 'PENDIENTE' END)"
            + "   WHEN grp.pagados > 0 THEN 'PAGADO'"
            + "   WHEN grp.entregados > 0 THEN 'ENTREGADO'"
            + "   ELSE 'CANCELADO' END)"
            + " WHEN " + ESTADO_P + " = 'CANCELADO' THEN 'CANCELADO'"
            // Igual que estadoBadge() de la card: un Apartado / Ir pagando es Pagado o Por cobrar,
            // diga lo que diga estado_pedido. Antes un Apartado con estado 'Pendiente' (el cobro de
            // la frase de liston nacia asi) salia en el filtro "Pendiente" y la card decia
            // "Por cobrar" (reportado en QA 2026-10-06).
            + " WHEN p.tipo_pedido IN ('APARTADO','FIADO') THEN"
            + "   (CASE WHEN " + ESTADO_P + " = 'PAGADO' THEN 'PAGADO' ELSE 'POR_COBRAR' END)"
            + " WHEN " + ESTADO_P + " IN ('PAGADO','ENTREGADO','PENDIENTE') THEN " + ESTADO_P
            + " ELSE 'OTRO' END)";

    /** El pago como lo dice la card: Falta pagar, Pagado o Cancelado (un titular, el de su grupo). */
    private static final String PAGO_CARD = "(CASE " + ESTADO_CARD
            + " WHEN 'CANCELADO' THEN 'CANCELADO'"
            + " WHEN 'PAGADO' THEN 'PAGADO'"
            + " WHEN 'ENTREGADO' THEN 'PAGADO'"
            + " ELSE 'FALTA_PAGAR' END)";

    /**
     * La entrega como la dice la card (columna {@code entregado}). Un titular esta Entregado solo si
     * todos los de su grupo, sin cancelados, lo estan (E7). Un cancelado no tiene entrega (NULL).
     */
    private static final String ENTREGA_CARD = "(CASE"
            + " WHEN " + ESTADO_P + " = 'CANCELADO' AND grp.titular_id IS NULL THEN NULL"
            + " WHEN grp.titular_id IS NOT NULL THEN (CASE WHEN grp.sin_entregar > 0 THEN 'FALTA_ENTREGAR'"
            + "   WHEN grp.vivos > 0 THEN 'ENTREGADO' ELSE NULL END)"
            + " WHEN p.entregado = 1 THEN 'ENTREGADO'"
            + " ELSE 'FALTA_ENTREGAR' END)";

    /** Espera entrega: no esta cancelado y todavia no se lo lleva (antes: no Entregado ni Pagado). */
    private static final String ESPERA_ENTREGA = "COALESCE(" + ENTREGA_CARD + ", '') = 'FALTA_ENTREGAR'";

    /** El total que muestra la card: el del pedido, o el de todo el grupo si es titular (R5). */
    private static final String TOTAL_CARD =
            "(CASE WHEN grp.titular_id IS NULL THEN COALESCE(p.total_pedido,0) ELSE grp.total END)";

    /** Lo que falta cobrar, igual que la card ("Falta $…"). */
    private static final String SALDO_CARD = "(CASE"
            + " WHEN grp.titular_id IS NOT NULL THEN grp.saldo"
            + " WHEN " + ESTADO_P + " IN ('CANCELADO','ENTREGADO','PAGADO') THEN 0"
            + " ELSE " + SALDO + " END)";

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public PaginaDePedidos buscar(FiltroPedidos filtro, LocalDate hoy) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String desde = """
                FROM pedidos p
                LEFT JOIN clientes c               ON c.id   = p.cliente_id
                LEFT JOIN clientes_sin_registro csr ON csr.id = p.cliente_sin_registro_id
                LEFT JOIN lugares_entrega le       ON le.id  = p.lugar_entrega_id
                """ + GRUPOS + "\nWHERE " + String.join("\n  AND ", condiciones(filtro, hoy, params));

        Long total = jdbc.queryForObject("SELECT COUNT(*) " + desde, params, Long.class);

        params.addValue("tamano", filtro.tamano());
        params.addValue("salto", (long) filtro.pagina() * filtro.tamano());
        List<Integer> ids = jdbc.queryForList(
                "SELECT p.id " + desde + "\nORDER BY " + orden(filtro) + "\nLIMIT :tamano OFFSET :salto",
                params, Integer.class);

        return new PaginaDePedidos(ids, total == null ? 0 : total, filtro.pagina(), filtro.tamano());
    }

    /**
     * Pendiente y Contado guardan los dos {@code tipo_pedido = 'NORMAL'}: los separa el estado. El
     * pedido que el cliente hace desde su cuenta queda 'Pendiente' hasta que se cobra (Entregado /
     * PAGADO) o se cancela. Constantes fijas: no entra texto del usuario al SQL.
     */
    private static String forma(FormaDeCobro forma) {
        String esNormal = "COALESCE(p.tipo_pedido,'NORMAL') = 'NORMAL'";
        return switch (forma) {
            case PENDIENTE -> "(" + esNormal + " AND " + ESTADO_P + " = 'PENDIENTE')";
            case CONTADO -> "(" + esNormal + " AND " + ESTADO_P + " <> 'PENDIENTE')";
            case APARTADO -> "p.tipo_pedido = 'APARTADO'";
            case IR_PAGANDO -> "p.tipo_pedido = 'FIADO'";
        };
    }

    private static List<String> condiciones(FiltroPedidos f, LocalDate hoy, MapSqlParameterSource params) {
        List<String> w = new ArrayList<>();

        // Mismo criterio que la lista de siempre: un pedido sin articulos no sale.
        w.add("EXISTS (SELECT 1 FROM detalle_pedidos dp0 WHERE dp0.pedido_id = p.id)");

        // R9: de un grupo activo solo sale el titular, salvo que se busque su numero exacto.
        Integer numero = f.numeroExacto();
        String noEsMiembroOculto = "NOT EXISTS (SELECT 1 FROM grupo_pedido_miembro gm"
                + " JOIN grupo_pedido g ON g.id = gm.grupo_id"
                + " WHERE gm.pedido_id = p.id AND g.activo = 1 AND g.pedido_titular_id <> p.id)";
        if (numero != null) {
            params.addValue("numero", numero);
            w.add("(" + noEsMiembroOculto + " OR p.id = :numero)");
        } else {
            w.add(noEsMiembroOculto);
        }

        if (f.texto() != null) {
            w.add(texto(f.texto(), numero, params));
        }

        if (!f.formas().isEmpty()) {
            w.add("(" + String.join(" OR ", f.formas().stream().map(PedidosFiltradosJdbcAdapter::forma).toList()) + ")");
        }

        // Pago y Entrega (dominio entrega, 2026-10-06): OR dentro de cada bloque, AND entre los dos.
        List<String> pago = f.estados().stream().filter(e -> !e.esDeEntrega())
                .map(e -> e == EstadoBuscado.PENDIENTE || e == EstadoBuscado.POR_COBRAR
                        ? EstadoBuscado.FALTA_PAGAR.name() : e.name())
                .distinct().toList();
        List<String> entrega = f.estados().stream().filter(EstadoBuscado::esDeEntrega)
                .map(Enum::name).distinct().toList();
        if (!pago.isEmpty()) {
            params.addValue("pago", pago);
            w.add(PAGO_CARD + " IN (:pago)");
        }
        if (!entrega.isEmpty()) {
            params.addValue("entrega", entrega);
            w.add(ENTREGA_CARD + " IN (:entrega)");
        }

        if (!f.dinero().isEmpty()) {
            w.add("(" + String.join(" OR ", f.dinero().stream().map(PedidosFiltradosJdbcAdapter::dinero).toList()) + ")");
        }

        if (f.totalMinimo() != null) {
            params.addValue("totalMinimo", f.totalMinimo());
            w.add(TOTAL_CARD + " >= :totalMinimo");
        }
        if (f.totalMaximo() != null) {
            params.addValue("totalMaximo", f.totalMaximo());
            w.add(TOTAL_CARD + " <= :totalMaximo");
        }

        RangoDeFechas registro = f.registro();
        if (registro != null) {
            String fecha = "COALESCE(p.fecha_hora_registro, p.fecha_pedido)";
            if (registro.desde() != null) {
                params.addValue("registroDesde", registro.desde().atStartOfDay());
                w.add(fecha + " >= :registroDesde");
            }
            if (registro.hasta() != null) {
                // < el dia siguiente: incluye todo el dia "hasta", hasta las 23:59:59.
                params.addValue("registroAntesDe", registro.hasta().plusDays(1).atStartOfDay());
                w.add(fecha + " < :registroAntesDe");
            }
        }

        if (f.entrega() != null) {
            w.add(entrega(f.entrega(), hoy, params));
        }

        if (f.lugarEntregaId() != null) {
            params.addValue("lugarEntregaId", f.lugarEntregaId());
            w.add("p.lugar_entrega_id = :lugarEntregaId");
        }
        if (f.modoEntrega() != null) {
            w.add(switch (f.modoEntrega()) {
                case RECOGE_EN_TIENDA -> "(le.id IS NULL OR le.es_recoger_en_tienda = 1)";
                case ENVIO -> "(le.id IS NOT NULL AND COALESCE(le.es_recoger_en_tienda,0) = 0)";
            });
        }

        if (f.unidos() != null) {
            String enGrupo = "EXISTS (SELECT 1 FROM grupo_pedido_miembro gu JOIN grupo_pedido gg ON gg.id = gu.grupo_id"
                    + " WHERE gu.pedido_id = p.id AND gg.activo = 1)";
            w.add(switch (f.unidos()) {
                case SOLO_UNIDOS -> enGrupo;
                case SIN_UNIR -> "NOT " + enGrupo;
            });
        }

        if (f.soloRamos()) {
            w.add("EXISTS (SELECT 1 FROM ramo_pedido_detalle r WHERE " + deLaCard("r.pedido_id") + ")");
        }
        if (f.soloConPromocion()) {
            w.add("EXISTS (SELECT 1 FROM detalle_pedidos dpp WHERE " + deLaCard("dpp.pedido_id")
                    + " AND dpp.promocion_id IS NOT NULL)");
        }
        return w;
    }

    /**
     * Los pedidos que muestra la card de {@code p}: el mismo y, si es titular de un grupo activo,
     * todos los de su grupo (R14).
     */
    private static String deLaCard(String columnaPedido) {
        return "(" + columnaPedido + " = p.id OR " + columnaPedido + " IN (SELECT gc.pedido_id FROM grupo_pedido_miembro gc"
                + " JOIN grupo_pedido gcg ON gcg.id = gc.grupo_id WHERE gcg.activo = 1 AND gcg.pedido_titular_id = p.id))";
    }

    /**
     * R1. Nombre del cliente (con cuenta o sin registro), nombre de quien recibe, telefono, correo,
     * y nombre o codigo de barras de cualquiera de sus articulos. Un numero ademas es el numero de
     * pedido exacto. En un titular tambien busca en los demas pedidos del grupo (R14): si no, el
     * pedido unido de otro cliente no se encontraba por su nombre (el miembro no sale en la lista).
     */
    private static String texto(TextoBuscado t, Integer numero, MapSqlParameterSource params) {
        params.addValue("like", "%" + escaparLike(t.valor()) + "%");
        List<String> o = new ArrayList<>();
        if (numero != null) {
            o.add("p.id = :numero");
        }
        List<String> persona = datosDePersona(t, "p", "c", "csr");
        o.addAll(persona);
        if (!persona.isEmpty()) {
            o.add("EXISTS (SELECT 1 FROM grupo_pedido gt"
                    + " JOIN grupo_pedido_miembro gmt ON gmt.grupo_id = gt.id"
                    + " JOIN pedidos pt ON pt.id = gmt.pedido_id"
                    + " LEFT JOIN clientes ct ON ct.id = pt.cliente_id"
                    + " LEFT JOIN clientes_sin_registro csrt ON csrt.id = pt.cliente_sin_registro_id"
                    + " WHERE gt.activo = 1 AND gt.pedido_titular_id = p.id AND pt.id <> p.id AND ("
                    + String.join(" OR ", datosDePersona(t, "pt", "ct", "csrt")) + "))");
        }
        String articulo = "EXISTS (SELECT 1 FROM detalle_pedidos dpt"
                + " JOIN producto prt ON prt.id = dpt.producto_id"
                + " LEFT JOIN codigo_barras cbt ON cbt.id = prt.codigo_barras_id"
                + " WHERE " + deLaCard("dpt.pedido_id") + " AND ";
        if (t.esNumero()) {
            if (t.sirveParaTelefono()) {
                o.add(articulo + "cbt.codigo_barras LIKE :like)");
            }
        } else {
            o.add(articulo + "(prt.nombre LIKE :like OR cbt.codigo_barras LIKE :like))");
        }
        return "(" + String.join("\n       OR ", o) + ")";
    }

    /** Nombre, telefono y correo de quien hizo el pedido y de quien recibe. */
    private static List<String> datosDePersona(TextoBuscado t, String pedido, String cliente, String sinRegistro) {
        List<String> o = new ArrayList<>();
        if (t.sirveParaTelefono()) {
            o.add(cliente + ".numero_telefonico LIKE :like");
            o.add(sinRegistro + ".numero_telefonico LIKE :like");
        }
        if (!t.esNumero()) {
            o.add(cliente + ".nombre_persona LIKE :like");
            o.add(sinRegistro + ".nombre_persona LIKE :like");
            o.add(pedido + ".nombre_receptor LIKE :like");
            o.add(cliente + ".correo_electronico LIKE :like");
            o.add(sinRegistro + ".correo_electronico LIKE :like");
        }
        return o;
    }

    /** "50%" no debe volverse un comodin: se buscan el % y el _ tal cual. */
    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** R4. Un titular se mide con su grupo (R14). */
    private static String dinero(SituacionDeDinero d) {
        String suelto = "grp.titular_id IS NULL AND ";
        String grupo = "grp.titular_id IS NOT NULL AND ";
        return switch (d) {
            case CON_SALDO -> "((" + suelto + CREDITO_ABIERTO + " AND " + SALDO + " > 0.005)"
                    + " OR (" + grupo + "grp.credito_abierto = 1 AND grp.saldo > 0.005))";
            case SIN_ABONOS -> "((" + suelto + CREDITO_ABIERTO
                    + " AND NOT EXISTS (SELECT 1 FROM abono_pedido ap WHERE ap.pedido_id = p.id))"
                    + " OR (" + grupo + "grp.credito_abierto = 1 AND grp.abonos = 0))";
            case SALDO_A_FAVOR -> "((" + suelto + saldoAFavor("p") + ") OR (" + grupo + "grp.a_favor = 1))";
        };
    }

    /**
     * Hay que devolverle dinero al cliente. Igual que AbonoServiceImpl.cancelarPedido: un Apartado
     * cancelado devuelve lo abonado y un pedido ya pagado es devolucion (pagado >= total). Un Ir
     * pagando cancelado que todavia debia NO: se llevo la mercancia y lo que falto es deuda
     * incobrable, no dinero a devolver.
     */
    private static String saldoAFavor(String a) {
        String estado = "UPPER(COALESCE(" + a + ".estado_pedido,''))";
        String saldo = "(COALESCE(" + a + ".total_pedido,0) - COALESCE(" + a + ".total_pagado,0))";
        return "(" + a + ".tipo_pedido IN ('APARTADO','FIADO') AND COALESCE(" + a + ".total_pagado,0) > 0.005 AND ("
                + "(" + estado + " <> 'CANCELADO' AND " + saldo + " < -0.005)"
                + " OR (" + estado + " = 'CANCELADO'"
                + " AND (" + a + ".tipo_pedido = 'APARTADO' OR " + saldo + " <= 0.005))))";
    }

    /** R7. */
    private static String entrega(CuandoSeEntrega cuando, LocalDate hoy, MapSqlParameterSource params) {
        RangoDeFechas rango = cuando.rango(hoy);
        List<String> y = new ArrayList<>(List.of(ESPERA_ENTREGA, "p.fecha_recogida IS NOT NULL"));
        if (rango.desde() != null) {
            params.addValue("entregaDesde", rango.desde());
            y.add("p.fecha_recogida >= :entregaDesde");
        }
        params.addValue("entregaHasta", rango.hasta());
        y.add("p.fecha_recogida <= :entregaHasta");
        return "(" + String.join(" AND ", y) + ")";
    }

    /** R11: siempre con el numero de pedido al final para que el orden no cambie entre paginas. */
    private static String orden(FiltroPedidos f) {
        String registro = "COALESCE(p.fecha_hora_registro, p.fecha_pedido)";
        return switch (f.orden()) {
            case RECIENTES -> registro + " DESC, p.id DESC";
            case ANTIGUOS -> registro + " ASC, p.id ASC";
            // Primero lo que falta entregar, de la fecha mas vieja (atrasados) a la mas lejana. Lo ya
            // entregado, pagado, cancelado o sin fecha va despues, del mas reciente al mas viejo: antes
            // salian primero los entregados de hace meses, porque su fecha era la mas antigua.
            case ENTREGA_PROXIMA -> {
                String pendiente = "(p.fecha_recogida IS NOT NULL AND " + ESPERA_ENTREGA + ")";
                yield "CASE WHEN " + pendiente + " THEN 0 ELSE 1 END, "
                        + "CASE WHEN " + pendiente + " THEN p.fecha_recogida END ASC, "
                        + registro + " DESC, p.id DESC";
            }
            case MAYOR_SALDO -> SALDO_CARD + " DESC, p.id DESC";
        };
    }
}
