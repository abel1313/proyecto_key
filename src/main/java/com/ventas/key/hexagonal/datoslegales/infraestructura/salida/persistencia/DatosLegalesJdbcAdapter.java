package com.ventas.key.hexagonal.datoslegales.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.datoslegales.dominio.modelo.DatosLegales;
import com.ventas.key.hexagonal.datoslegales.dominio.puerto.salida.DatosLegalesPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks &amp; Drivers]
 *
 * <p>Tabla de una sola fila {@code datos_legales_negocio} con {@code id = 1} fijo
 * (migration_datos_legales_negocio.sql). Sin entidad JPA: con id fijo y sin AUTO_INCREMENT,
 * heredar BaseId haría fallar el INSERT (lección de tiktok_token en CLAUDE.md).
 */
@Component
@RequiredArgsConstructor
public class DatosLegalesJdbcAdapter implements DatosLegalesPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Optional<DatosLegales> leer() {
        return jdbc.query("""
                        SELECT nombre_responsable, rfc, domicilio, telefono, correo, horario_atencion
                        FROM datos_legales_negocio WHERE id = 1""",
                (rs, i) -> new DatosLegales(
                        rs.getString("nombre_responsable"), rs.getString("rfc"), rs.getString("domicilio"),
                        rs.getString("telefono"), rs.getString("correo"), rs.getString("horario_atencion")))
                .stream().findFirst();
    }

    @Override
    public void guardar(DatosLegales d) {
        jdbc.update("""
                        INSERT INTO datos_legales_negocio
                            (id, nombre_responsable, rfc, domicilio, telefono, correo, horario_atencion, actualizado_en)
                        VALUES (1, :nombre, :rfc, :domicilio, :telefono, :correo, :horario, NOW())
                        ON DUPLICATE KEY UPDATE
                            nombre_responsable = VALUES(nombre_responsable), rfc = VALUES(rfc),
                            domicilio = VALUES(domicilio), telefono = VALUES(telefono), correo = VALUES(correo),
                            horario_atencion = VALUES(horario_atencion), actualizado_en = NOW()""",
                new MapSqlParameterSource()
                        .addValue("nombre", d.nombreResponsable())
                        .addValue("rfc", d.rfc())
                        .addValue("domicilio", d.domicilio())
                        .addValue("telefono", d.telefono())
                        .addValue("correo", d.correo())
                        .addValue("horario", d.horarioAtencion()));
    }
}
