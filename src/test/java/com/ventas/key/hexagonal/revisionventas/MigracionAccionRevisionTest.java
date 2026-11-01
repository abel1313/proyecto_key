package com.ventas.key.hexagonal.revisionventas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code migration_accion_reporte_revision.sql} corre completa, dos veces seguidas, y la acción queda solo para ROLE_ADMIN
 * (regla del proyecto: toda migración se prueba en una base desechable y dos veces).
 */
class MigracionAccionRevisionTest {

    @Test
    @DisplayName("la migración es idempotente y le da la acción solo al administrador")
    void correDosVeces() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:mig" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE submenu (id INT AUTO_INCREMENT PRIMARY KEY, ruta VARCHAR(80))");
        jdbc.execute("CREATE TABLE accion_submenu (id INT AUTO_INCREMENT PRIMARY KEY, submenu_id INT, clave VARCHAR(60), "
                + "etiqueta VARCHAR(120), descripcion VARCHAR(255), orden INT)");
        jdbc.execute("CREATE TABLE roles (id INT AUTO_INCREMENT PRIMARY KEY, nombre_rol VARCHAR(40))");
        jdbc.execute("CREATE TABLE rol_accion (rol_id INT, accion_submenu_id INT)");
        jdbc.update("INSERT INTO submenu (ruta) VALUES ('reportes'), ('gastos/buscar')");
        jdbc.update("INSERT INTO roles (nombre_rol) VALUES ('ROLE_ADMIN'), ('ROLE_EMPLEADO')");

        String sql = Files.readString(Path.of("src/main/resources/static/migration_accion_reporte_revision.sql"));
        String[] sentencias = Arrays.stream(sql.split("\n")).filter(l -> !l.trim().startsWith("--")).reduce("", (a, b) -> a + b + "\n").split(";");
        for (int vez = 1; vez <= 2; vez++) {
            for (String s : sentencias) {
                if (!s.isBlank()) jdbc.execute(s);
            }
        }

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM accion_submenu WHERE clave = 'reporte-revision'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rol_accion", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT r.nombre_rol FROM rol_accion ra JOIN roles r ON r.id = ra.rol_id", String.class))
                .isEqualTo("ROLE_ADMIN");
    }
}
