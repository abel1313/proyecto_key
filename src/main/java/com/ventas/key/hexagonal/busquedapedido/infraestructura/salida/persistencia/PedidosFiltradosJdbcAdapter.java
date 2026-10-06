package com.ventas.key.hexagonal.busquedapedido.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.CuandoSeEntrega;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.EstadoBuscado;
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
 * <p>Valores reales de {@code estado_pedido} (se guardan con mayusculas distintas, por eso se compara
 * con UPPER): {@code Pendiente}, {@code Entregado}, {@code APARTADO}, {@code FIADO}, {@code PAGADO},
 * {@code cancelado}.
 */
@Component
@RequiredArgsConstructor
public class PedidosFiltradosJdbcAdapter implements PedidosFiltradosPort {

    /** Apartado o Ir pagando que todavia no se liquida ni se cancela. */
    private static final String CREDITO_ABIERTO = "(p.tipo_pedido IN ('APARTADO','FIADO')"
            + " AND UPPER(COALESCE(p.estado_pedido,'')) NOT IN ('PAGADO','CANCELADO','ENTREGADO'))";

    /** Igual que esperaEntrega() de la card: lo Entregado, Pagado o Cancelado ya no espera entrega. */
    private static final String ESPERA_ENTREGA =
            "UPPER(COALESCE(p.estado_pedido,'')) NOT IN ('ENTREGADO','CANCELADO','PAGADO')";

    private static final String SALDO = "(COALESCE(p.total_pedido,0) - COALESCE(p.total_pagado,0))";

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public PaginaDePedidos buscar(FiltroPedidos filtro, LocalDate hoy) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String desde = """
                FROM pedidos p
                LEFT JOIN clientes c               ON c.id   = p.cliente_id
                LEFT JOIN clientes_sin_registro csr ON csr.id = p.cliente_sin_registro_id
                LEFT JOIN lugares_entrega le       ON le.id  = p.lugar_entrega_id
                WHERE """ + " " + String.join("\n  AND ", condiciones(filtro, hoy, params));

        Long total = jdbc.queryForObject("SELECT COUNT(*) " + desde, params, Long.class);

        params.addValue("tamano", filtro.tamano());
        params.addValue("salto", (long) filtro.pagina() * filtro.tamano());
        List<Integer> ids = jdbc.queryForList(
                "SELECT p.id " + desde + "\nORDER BY " + orden(filtro) + "\nLIMIT :tamano OFFSET :salto",
                params, Integer.class);

        return new PaginaDePedidos(ids, total == null ? 0 : total, filtro.pagina(), filtro.tamano());
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
            params.addValue("formas", f.formas().stream().map(FormaDeCobro::codigo).toList());
            w.add("COALESCE(p.tipo_pedido,'NORMAL') IN (:formas)");
        }

        if (!f.estados().isEmpty()) {
            w.add("(" + String.join(" OR ", f.estados().stream().map(PedidosFiltradosJdbcAdapter::estado).toList()) + ")");
        }

        if (!f.dinero().isEmpty()) {
            w.add("(" + String.join(" OR ", f.dinero().stream().map(PedidosFiltradosJdbcAdapter::dinero).toList()) + ")");
        }

        if (f.totalMinimo() != null) {
            params.addValue("totalMinimo", f.totalMinimo());
            w.add("COALESCE(p.total_pedido,0) >= :totalMinimo");
        }
        if (f.totalMaximo() != null) {
            params.addValue("totalMaximo", f.totalMaximo());
            w.add("COALESCE(p.total_pedido,0) <= :totalMaximo");
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
            w.add("EXISTS (SELECT 1 FROM ramo_pedido_detalle r WHERE r.pedido_id = p.id)");
        }
        if (f.soloConPromocion()) {
            w.add("EXISTS (SELECT 1 FROM detalle_pedidos dpp WHERE dpp.pedido_id = p.id AND dpp.promocion_id IS NOT NULL)");
        }
        return w;
    }

    /**
     * R1. Nombre del cliente (con cuenta o sin registro), nombre de quien recibe, telefono, correo,
     * y nombre o codigo de barras de cualquiera de sus articulos. Un numero ademas es el numero de
     * pedido exacto.
     */
    private static String texto(TextoBuscado t, Integer numero, MapSqlParameterSource params) {
        params.addValue("like", "%" + escaparLike(t.valor()) + "%");
        List<String> o = new ArrayList<>();
        if (numero != null) {
            o.add("p.id = :numero");
        }
        if (t.sirveParaTelefono()) {
            o.add("c.numero_telefonico LIKE :like");
            o.add("csr.numero_telefonico LIKE :like");
        }
        String articulo = "EXISTS (SELECT 1 FROM detalle_pedidos dpt"
                + " JOIN producto prt ON prt.id = dpt.producto_id"
                + " LEFT JOIN codigo_barras cbt ON cbt.id = prt.codigo_barras_id"
                + " WHERE dpt.pedido_id = p.id AND ";
        if (t.esNumero()) {
            if (t.sirveParaTelefono()) {
                o.add(articulo + "cbt.codigo_barras LIKE :like)");
            }
        } else {
            o.add("c.nombre_persona LIKE :like");
            o.add("csr.nombre_persona LIKE :like");
            o.add("p.nombre_receptor LIKE :like");
            o.add("c.correo_electronico LIKE :like");
            o.add("csr.correo_electronico LIKE :like");
            o.add(articulo + "(prt.nombre LIKE :like OR cbt.codigo_barras LIKE :like))");
        }
        return "(" + String.join("\n       OR ", o) + ")";
    }

    /** "50%" no debe volverse un comodin: se buscan el % y el _ tal cual. */
    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** R3. */
    private static String estado(EstadoBuscado e) {
        String estado = "UPPER(COALESCE(p.estado_pedido,''))";
        return switch (e) {
            case PENDIENTE -> estado + " = 'PENDIENTE'";
            case POR_COBRAR -> "(p.tipo_pedido IN ('APARTADO','FIADO') AND " + estado + " IN ('APARTADO','FIADO'))";
            case PAGADO -> estado + " = 'PAGADO'";
            case ENTREGADO -> estado + " = 'ENTREGADO'";
            case CANCELADO -> estado + " = 'CANCELADO'";
        };
    }

    /** R4. */
    private static String dinero(SituacionDeDinero d) {
        return switch (d) {
            case CON_SALDO -> "(" + CREDITO_ABIERTO + " AND " + SALDO + " > 0.005)";
            case SIN_ABONOS -> "(" + CREDITO_ABIERTO
                    + " AND NOT EXISTS (SELECT 1 FROM abono_pedido ap WHERE ap.pedido_id = p.id))";
            // Cancelado: igual que AbonoServiceImpl.cancelarPedido. Un Apartado devuelve lo abonado
            // y un pedido ya pagado es devolucion (pagado >= total). Un Ir pagando que todavia debia
            // NO: se llevo la mercancia y lo que falto es deuda incobrable, no dinero a devolver.
            case SALDO_A_FAVOR -> "(p.tipo_pedido IN ('APARTADO','FIADO') AND COALESCE(p.total_pagado,0) > 0.005 AND ("
                    + "(UPPER(COALESCE(p.estado_pedido,'')) <> 'CANCELADO' AND " + SALDO + " < -0.005)"
                    + " OR (UPPER(COALESCE(p.estado_pedido,'')) = 'CANCELADO'"
                    + " AND (p.tipo_pedido = 'APARTADO' OR " + SALDO + " <= 0.005))))";
        };
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
            case ENTREGA_PROXIMA -> "p.fecha_recogida IS NULL, p.fecha_recogida ASC, p.id DESC";
            case MAYOR_SALDO -> "CASE WHEN UPPER(COALESCE(p.estado_pedido,'')) IN ('CANCELADO','ENTREGADO','PAGADO') THEN 0"
                    + " ELSE " + SALDO + " END DESC, p.id DESC";
        };
    }
}
