package com.ventas.key.hexagonal.graficasventas.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.graficasventas.dominio.modelo.FormaDePagoVentas;
import com.ventas.key.hexagonal.graficasventas.dominio.modelo.VentasDelMes;
import com.ventas.key.hexagonal.graficasventas.dominio.puerto.salida.GraficasPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [Hexagonal: Driven Adapter] [Clean: Interface Adapter]
 *
 * <p>SQL de solo lectura para las gráficas del Dashboard. Criterios iguales a las tarjetas del Dashboard: las ventas con
 * estado «Devuelta» no cuentan y los gastos son {@code gastos_surtir.precio_gasto}. La comisión usa la misma cuenta con la
 * que se calcula la ganancia al vender: {@code total × (tarifa + IVA de la terminal) / 100}.
 * Solo SQL estándar ({@code EXTRACT}, {@code COALESCE}), para probarlo con H2 y correr en MySQL.
 */
@Component
public class GraficasJdbcAdapter implements GraficasPort {

    /** Constante fija (no viene de nadie): va dentro del SQL para que SELECT y GROUP BY sean idénticos en MySQL. */
    private static final String SIN_FORMA = "Sin forma de pago registrada";

    private final JdbcTemplate jdbc;

    public GraficasJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormaDePagoVentas> ventasPorFormaDePago(LocalDate desde, LocalDate hasta) {
        return jdbc.query(
                "SELECT COALESCE(tp.forma_pago, '" + SIN_FORMA + "') AS forma, COUNT(*) AS ventas, "
                        + "COALESCE(SUM(v.total_venta), 0) AS total, "
                        + "COALESCE(SUM(v.total_venta * (COALESCE(tt.tarifa, 0) + COALESCE(it.iva, 0)) / 100.0), 0) AS comision "
                        + "FROM ventas v "
                        + "LEFT JOIN pagos_y_meses pm ON pm.id = v.pagos_y_meses_id "
                        + "LEFT JOIN tipo_pago tp ON tp.id = pm.tipo_pago_id "
                        + "LEFT JOIN meses_intereses mi ON mi.id = pm.meses_intereses_id "
                        + "LEFT JOIN tarifa_terminal tt ON tt.id = mi.tarifa_id "
                        + "LEFT JOIN iva_terminal it ON it.id = mi.iva_id "
                        + "WHERE v.fecha_venta >= ? AND v.fecha_venta < ? AND v.estado_venta <> 'Devuelta' "
                        + "GROUP BY COALESCE(tp.forma_pago, '" + SIN_FORMA + "')",
                (rs, n) -> new FormaDePagoVentas(rs.getString("forma"), rs.getInt("ventas"), rs.getDouble("total"), rs.getDouble("comision")),
                inicio(desde), finExclusivo(hasta));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<YearMonth, VentasDelMes> ventasPorMes(LocalDate desde, LocalDate hasta) {
        Map<YearMonth, VentasDelMes> resultado = new HashMap<>();
        jdbc.query(
                "SELECT EXTRACT(YEAR FROM v.fecha_venta) AS anio, EXTRACT(MONTH FROM v.fecha_venta) AS mes, COUNT(*) AS ventas, "
                        + "COALESCE(SUM(v.total_venta), 0) AS total, COALESCE(SUM(v.ganancia_total), 0) AS ganancia "
                        + "FROM ventas v WHERE v.fecha_venta >= ? AND v.fecha_venta < ? AND v.estado_venta <> 'Devuelta' "
                        + "GROUP BY EXTRACT(YEAR FROM v.fecha_venta), EXTRACT(MONTH FROM v.fecha_venta)",
                rs -> {
                    resultado.put(YearMonth.of(rs.getInt("anio"), rs.getInt("mes")),
                            new VentasDelMes(rs.getInt("ventas"), rs.getDouble("total"), rs.getDouble("ganancia")));
                },
                inicio(desde), finExclusivo(hasta));
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<YearMonth, Double> gastosPorMes(LocalDate desde, LocalDate hasta) {
        Map<YearMonth, Double> resultado = new HashMap<>();
        jdbc.query(
                "SELECT EXTRACT(YEAR FROM g.fecha) AS anio, EXTRACT(MONTH FROM g.fecha) AS mes, COALESCE(SUM(g.precio_gasto), 0) AS total "
                        + "FROM gastos_surtir g WHERE g.fecha BETWEEN ? AND ? "
                        + "GROUP BY EXTRACT(YEAR FROM g.fecha), EXTRACT(MONTH FROM g.fecha)",
                rs -> {
                    resultado.put(YearMonth.of(rs.getInt("anio"), rs.getInt("mes")), rs.getDouble("total"));
                },
                java.sql.Date.valueOf(desde), java.sql.Date.valueOf(hasta));
        return resultado;
    }

    private static Timestamp inicio(LocalDate dia) {
        return Timestamp.valueOf(dia.atStartOfDay());
    }

    private static Timestamp finExclusivo(LocalDate dia) {
        return Timestamp.valueOf(dia.plusDays(1).atStartOfDay());
    }
}
