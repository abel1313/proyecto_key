package com.ventas.key.hexagonal.cortecaja;

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
 * {@code migration_submenu_corte_caja.sql} corre completa, dos veces seguidas (regla del proyecto), crea la pantalla en el mismo grupo que
 * "Reportes de ventas" y se la da solo a ROLE_ADMIN.
 */
class MigracionSubmenuCorteCajaTest {

    @Test
    @DisplayName("la migración es idempotente: una sola fila, en el grupo de Reportes, solo para el administrador")
    void correDosVeces() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:mig" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE submenu (id INT AUTO_INCREMENT PRIMARY KEY, menu_id INT, nombre VARCHAR(80), ruta VARCHAR(150), "
                + "icono VARCHAR(10), descripcion VARCHAR(255), orden INT, tiene_escritura TINYINT(1) NOT NULL DEFAULT 1)");
        jdbc.execute("CREATE TABLE roles (id INT AUTO_INCREMENT PRIMARY KEY, nombre_rol VARCHAR(40))");
        jdbc.execute("CREATE TABLE rol_submenu (rol_id INT, submenu_id INT)");
        jdbc.update("INSERT INTO submenu (menu_id, nombre, ruta, orden) VALUES (7, 'Reportes de ventas', 'reportes', 2), (7, 'Dashboard', 'dashboard', 1), (3, 'Otro', 'otro', 1)");
        jdbc.update("INSERT INTO roles (nombre_rol) VALUES ('ROLE_ADMIN'), ('ROLE_EMPLEADO')");

        String sql = Files.readString(Path.of("src/main/resources/static/migration_submenu_corte_caja.sql"));
        String[] sentencias = Arrays.stream(sql.split("\n")).filter(l -> !l.trim().startsWith("--")).reduce("", (a, b) -> a + b + "\n").split(";");
        for (int vez = 1; vez <= 2; vez++) {
            for (String s : sentencias) {
                if (!s.isBlank()) jdbc.execute(s);
            }
        }

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM submenu WHERE ruta = 'reportes/corte-caja'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT menu_id FROM submenu WHERE ruta = 'reportes/corte-caja'", Integer.class)).isEqualTo(7);
        assertThat(jdbc.queryForObject("SELECT orden FROM submenu WHERE ruta = 'reportes/corte-caja'", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT tiene_escritura FROM submenu WHERE ruta = 'reportes/corte-caja'", Integer.class)).isZero();
        assertThat(jdbc.queryForList("SELECT ro.nombre_rol FROM rol_submenu rs JOIN roles ro ON ro.id = rs.rol_id "
                + "JOIN submenu s ON s.id = rs.submenu_id WHERE s.ruta = 'reportes/corte-caja'", String.class))
                .containsExactly("ROLE_ADMIN");
    }
}
