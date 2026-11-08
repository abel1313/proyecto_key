package com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ¿Ya corrió la migración de esta tabla? Se pregunta por una conexión APARTE de la venta: si la tabla no existe y la consulta fallara dentro de
 * la transacción de la venta o de la edición, la marcaría para deshacerse y se caería todo. Una tabla que sí existe se recuerda; una que no, se
 * vuelve a mirar cada minuto (así, al correr la migración, empieza sola sin reiniciar).
 */
@Component
public class TablasDisponibles {

    private static final Logger log = LoggerFactory.getLogger(TablasDisponibles.class);
    private static final long REVISAR_CADA_MS = 60_000L;

    private final DataSource dataSource;
    private final Map<String, Boolean> existentes = new ConcurrentHashMap<>();
    private final Map<String, Long> ultimaRevision = new ConcurrentHashMap<>();

    public TablasDisponibles(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean existe(String tabla) {
        if (Boolean.TRUE.equals(existentes.get(tabla))) {
            return true;
        }
        long ahora = System.currentTimeMillis();
        Long antes = ultimaRevision.get(tabla);
        if (antes != null && ahora - antes < REVISAR_CADA_MS) {
            return false;
        }
        ultimaRevision.put(tabla, ahora);
        boolean hay = false;
        try (Connection c = dataSource.getConnection();
             ResultSet rs = c.getMetaData().getTables(c.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next() && !hay) {
                hay = tabla.equalsIgnoreCase(rs.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
            log.warn("No se pudo revisar si existe la tabla {}: {}", tabla, e.getMessage());
        }
        existentes.put(tabla, hay);
        if (!hay) {
            log.warn("La tabla {} no existe: esa parte del precio por cantidad no se aplica (falta correr su migracion)", tabla);
        }
        return hay;
    }
}
