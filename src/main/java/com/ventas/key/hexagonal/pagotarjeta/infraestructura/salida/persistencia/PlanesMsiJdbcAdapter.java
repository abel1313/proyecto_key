package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.PlanMsi;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.ReglasMsi;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PlanesMsiPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * [Hexagonal: Driven Adapter] [Clean: Interface Adapter]
 *
 * <p>Lee los planes de meses sin intereses de {@code meses_intereses} + {@code pagos_y_meses} (solo los
 * enlazados a una forma de pago de tarjeta de CREDITO: el debito no tiene meses sin intereses y la venta solo
 * registra planes de credito). El texto del plazo se interpreta con una regex estricta
 * ("3", "6 msi", "9 meses"): "3 y 6" no es 36 meses. Solo se aceptan 3, 6, 9 y 12; si un plazo aparece
 * varias veces se queda con la comision mas alta (lo seguro para el margen). El minimo de compra sale
 * de {@code pagos.tarjeta.msi-minimo} (default 300).
 */
@Component
public class PlanesMsiJdbcAdapter implements PlanesMsiPort {

    private static final Pattern PLAZO = Pattern.compile("^\\s*(\\d{1,2})\\s*(?:msi|mes(?:es)?)?\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final List<Integer> PLAZOS_VALIDOS = List.of(3, 6, 9, 12);

    private final JdbcTemplate jdbc;
    private final BigDecimal minimo;

    public PlanesMsiJdbcAdapter(JdbcTemplate jdbc,
                                @Value("${pagos.tarjeta.msi-minimo:300}") BigDecimal minimo) {
        this.jdbc = jdbc;
        this.minimo = minimo != null ? minimo : ReglasMsi.MINIMO_POR_DEFECTO;
    }

    @Override
    public List<PlanMsi> planes() {
        List<Map<String, Object>> filas = jdbc.queryForList(
                "SELECT DISTINCT m.meses AS meses, t.tarifa AS tarifa FROM meses_intereses m "
                        + "JOIN pagos_y_meses pm ON pm.meses_intereses_id = m.id "
                        + "JOIN tipo_pago tp ON tp.id = pm.tipo_pago_id "
                        + "LEFT JOIN tarifa_terminal t ON t.id = m.tarifa_id "
                        + "WHERE LOWER(tp.forma_pago) LIKE '%credito%' OR LOWER(tp.forma_pago) LIKE '%crédito%'");
        Map<Integer, PlanMsi> porPlazo = new TreeMap<>();
        for (Map<String, Object> fila : filas) {
            Integer plazo = interpretar(fila.get("meses"));
            if (plazo == null || !PLAZOS_VALIDOS.contains(plazo)) {
                continue;
            }
            BigDecimal comision = fila.get("tarifa") == null
                    ? BigDecimal.ZERO : new BigDecimal(fila.get("tarifa").toString());
            PlanMsi previo = porPlazo.get(plazo);
            if (previo == null || comision.compareTo(previo.comision()) > 0) {
                porPlazo.put(plazo, new PlanMsi(plazo, true, comision));
            }
        }
        return List.copyOf(porPlazo.values());
    }

    @Override
    public BigDecimal minimoParaMsi() {
        return minimo;
    }

    static Integer interpretar(Object texto) {
        if (texto == null) {
            return null;
        }
        Matcher m = PLAZO.matcher(texto.toString());
        return m.matches() ? Integer.valueOf(m.group(1)) : null;
    }
}
