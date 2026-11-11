package com.ventas.key.hexagonal.flete.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.flete.dominio.modelo.ProductoConFlete;
import com.ventas.key.hexagonal.flete.dominio.puerto.salida.FletesGuardadosPort;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia.TablasDisponibles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** [Hexagonal: Driven Adapter] Lee el producto y el último cálculo de precio (id mayor) de cada uno. Sin la tabla de cálculos, nadie tiene flete guardado. */
@Component
public class FletesGuardadosJdbcAdapter implements FletesGuardadosPort {

    private static final int LOTE = 500;

    private final JdbcTemplate jdbc;
    private final TablasDisponibles tablas;

    public FletesGuardadosJdbcAdapter(JdbcTemplate jdbc, TablasDisponibles tablas) {
        this.jdbc = jdbc;
        this.tablas = tablas;
    }

    @Override
    public List<ProductoConFlete> productosConFlete(Collection<Integer> productoIds) {
        List<ProductoConFlete> r = new ArrayList<>();
        if (productoIds.isEmpty()) {
            return r;
        }
        boolean hayCalculos = tablas.existe("producto_calculo_precio");
        List<Integer> ids = new ArrayList<>(productoIds);
        for (int i = 0; i < ids.size(); i += LOTE) {
            List<Integer> parte = ids.subList(i, Math.min(ids.size(), i + LOTE));
            String marcas = String.join(",", Collections.nCopies(parte.size(), "?"));
            String calculo = hayCalculos
                    ? "LEFT JOIN producto_calculo_precio c ON c.producto_id = p.id AND c.id = (SELECT MAX(x.id) FROM producto_calculo_precio x WHERE x.producto_id = p.id) "
                    : "";
            String transporte = hayCalculos ? "COALESCE(c.transporte, 0)" : "0";
            jdbc.query("SELECT p.id AS pid, p.nombre AS nombre, cb.codigo_barras AS codigo, COALESCE(p.precio_costo, 0) AS costo, COALESCE(p.stock, 0) AS stock, "
                    + transporte + " AS transporte FROM producto p LEFT JOIN codigo_barras cb ON cb.id = p.codigo_barras_id " + calculo
                    + "WHERE p.id IN (" + marcas + ") ORDER BY p.nombre",
                    rs -> {
                        r.add(new ProductoConFlete(rs.getInt("pid"), rs.getString("nombre"), rs.getString("codigo"), rs.getBigDecimal("costo"),
                                rs.getInt("stock"), rs.getBigDecimal("transporte") == null ? BigDecimal.ZERO : rs.getBigDecimal("transporte")));
                    }, parte.toArray());
        }
        return r;
    }
}
