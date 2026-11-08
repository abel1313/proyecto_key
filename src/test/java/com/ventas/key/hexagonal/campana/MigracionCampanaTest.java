package com.ventas.key.hexagonal.campana;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** {@code migration_campana.sql} corre completa, dos veces seguidas (regla del proyecto). */
class MigracionCampanaTest {

    @Test
    @DisplayName("la migración es idempotente, el producto no se repite en una campaña y la pantalla es solo del administrador")
    void correDosVeces() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:mig" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE producto (id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(80))");
        jdbc.execute("CREATE TABLE submenu (id INT AUTO_INCREMENT PRIMARY KEY, menu_id INT, nombre VARCHAR(80), ruta VARCHAR(150), "
                + "icono VARCHAR(10), descripcion VARCHAR(255), orden INT, tiene_escritura TINYINT(1) NOT NULL DEFAULT 1)");
        jdbc.execute("CREATE TABLE roles (id INT AUTO_INCREMENT PRIMARY KEY, nombre_rol VARCHAR(40))");
        jdbc.execute("CREATE TABLE rol_submenu (rol_id INT, submenu_id INT)");
        jdbc.update("INSERT INTO submenu (menu_id, nombre, ruta, orden) VALUES (4, 'Buscar producto', 'productos/buscar', 2), "
                + "(4, 'Grupos', 'productos/grupos-precio', 3), (3, 'Otro', 'otro', 1)");
        jdbc.update("INSERT INTO roles (nombre_rol) VALUES ('ROLE_ADMIN'), ('ROLE_EMPLEADO')");
        jdbc.update("INSERT INTO producto (nombre) VALUES ('Carro'), ('Muñeca')");

        String sql = Files.readString(Path.of("src/main/resources/static/migration_campana.sql"));
        String[] sentencias = Arrays.stream(sql.split("\n")).filter(l -> !l.trim().startsWith("--")).reduce("", (a, b) -> a + b + "\n").split(";");
        for (int vez = 1; vez <= 2; vez++) {
            for (String s : sentencias) {
                if (!s.isBlank()) jdbc.execute(s);
            }
        }

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM submenu WHERE ruta = 'productos/campanas'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT orden FROM submenu WHERE ruta = 'productos/campanas'", Integer.class)).as("después de Grupos").isEqualTo(4);
        assertThat(jdbc.queryForList("SELECT ro.nombre_rol FROM rol_submenu rs JOIN roles ro ON ro.id = rs.rol_id "
                + "JOIN submenu s ON s.id = rs.submenu_id WHERE s.ruta = 'productos/campanas'", String.class)).containsExactly("ROLE_ADMIN");

        jdbc.update("INSERT INTO campana (nombre, fecha_inicio, fecha_fin) VALUES ('Reyes', '2026-12-20', '2027-01-06'), ('Roscas', '2027-01-07', '2027-01-10')");
        jdbc.update("INSERT INTO campana_producto (campana_id, producto_id, precio_campana) VALUES (1, 1, 100), (2, 1, 90)");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO campana_producto (campana_id, producto_id, precio_campana) VALUES (1, 1, 80)"))
                .as("el mismo producto dos veces en la misma campaña debe rechazarse").isNotNull();

        jdbc.update("DELETE FROM producto WHERE id = 1");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM campana_producto", Integer.class)).isZero();
    }
}
