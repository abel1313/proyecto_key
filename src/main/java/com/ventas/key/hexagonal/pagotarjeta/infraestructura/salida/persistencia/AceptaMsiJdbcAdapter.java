package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.ConfigMsiArticulo;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.AceptaMsiPort;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * [Hexagonal: Driven Adapter] [Clean: Interface Adapter]
 *
 * <p>Lee y guarda {@code producto.acepta_msi} (0/1) y {@code variantes.acepta_msi} (NULL = hereda) con SQL directo,
 * sin pasar por las entidades JPA: {@code Producto} y {@code Variantes} no mapean esas columnas a propósito (un
 * ambiente sin la migración tumbaría todo el catálogo). Sin la columna, solo estas operaciones fallan, con un
 * mensaje claro. Es la misma consulta {@code COALESCE(variantes.acepta_msi, producto.acepta_msi)} que usan
 * {@code PedidoCobroAdapter} y {@code PlanesMsiJdbcAdapter} al cobrar.
 */
@Component
public class AceptaMsiJdbcAdapter implements AceptaMsiPort {

    private final JdbcTemplate jdbc;

    public AceptaMsiJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Boolean> productoAcepta(Integer productoId) {
        return conColumna(() -> jdbc.query("SELECT acepta_msi FROM producto WHERE id = ?",
                (rs, n) -> rs.getInt(1) == 1, productoId).stream().findFirst());
    }

    @Override
    @Transactional
    public boolean fijarProducto(Integer productoId, boolean acepta) {
        return conColumna(() -> jdbc.update("UPDATE producto SET acepta_msi = ? WHERE id = ?",
                acepta ? 1 : 0, productoId) > 0 || existeProducto(productoId));
    }

    @Override
    public Optional<ConfigMsiArticulo> articulo(Integer varianteId) {
        return conColumna(() -> jdbc.query(
                "SELECT v.acepta_msi, p.acepta_msi FROM variantes v JOIN producto p ON p.id = v.producto_id "
                        + "WHERE v.id = ?",
                (rs, n) -> {
                    int propio = rs.getInt(1);
                    Boolean propioONull = rs.wasNull() ? null : propio == 1;
                    return new ConfigMsiArticulo(propioONull, rs.getInt(2) == 1);
                }, varianteId).stream().findFirst());
    }

    @Override
    @Transactional
    public boolean fijarArticulo(Integer varianteId, Boolean acepta) {
        return conColumna(() -> jdbc.update("UPDATE variantes SET acepta_msi = ? WHERE id = ?",
                acepta == null ? null : (acepta ? 1 : 0), varianteId) > 0 || existeArticulo(varianteId));
    }

    @Override
    public Map<Integer, Boolean> efectivos(Collection<Integer> varianteIds) {
        if (varianteIds == null || varianteIds.isEmpty()) {
            return Map.of();
        }
        String marcas = String.join(",", java.util.Collections.nCopies(varianteIds.size(), "?"));
        return conColumna(() -> {
            Map<Integer, Boolean> resultado = new HashMap<>();
            jdbc.query("SELECT v.id, COALESCE(v.acepta_msi, p.acepta_msi) FROM variantes v "
                            + "JOIN producto p ON p.id = v.producto_id WHERE v.id IN (" + marcas + ")",
                    rs -> {
                        resultado.put(rs.getInt(1), rs.getInt(2) == 1);
                    }, varianteIds.toArray());
            return resultado;
        });
    }

    // MySQL devuelve "0 filas cambiadas" cuando el valor ya era el mismo: se distingue de "no existe".
    private boolean existeProducto(Integer id) {
        List<Integer> r = jdbc.queryForList("SELECT id FROM producto WHERE id = ?", Integer.class, id);
        return !r.isEmpty();
    }

    private boolean existeArticulo(Integer id) {
        List<Integer> r = jdbc.queryForList("SELECT id FROM variantes WHERE id = ?", Integer.class, id);
        return !r.isEmpty();
    }

    /** Si falta la columna (migración sin correr) se avisa con un mensaje claro en vez de un error técnico. */
    private static <T> T conColumna(java.util.function.Supplier<T> operacion) {
        try {
            return operacion.get();
        } catch (BadSqlGrammarException e) {
            throw PagoConTarjetaException.casillaMsiNoDisponible();
        }
    }
}
