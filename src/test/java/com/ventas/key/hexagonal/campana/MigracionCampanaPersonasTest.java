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

/** {@code migration_campana_personas.sql} corre completa, dos veces seguidas, sobre las tablas de {@code migration_campana.sql}. */
class MigracionCampanaPersonasTest {

    private static void correr(JdbcTemplate jdbc, String archivo) throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/static/" + archivo));
        String[] sentencias = Arrays.stream(sql.split("\n")).filter(l -> !l.trim().startsWith("--")).reduce("", (a, b) -> a + b + "\n").split(";");
        for (String s : sentencias) {
            if (!s.isBlank()) jdbc.execute(s);
        }
    }

    @Test
    @DisplayName("idempotente; el uuid es único; borrar a la persona borra sus ligas pero no el pedido")
    void correDosVeces() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:mig" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE producto (id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(80))");
        jdbc.execute("CREATE TABLE pedidos (id INT AUTO_INCREMENT PRIMARY KEY, total DOUBLE)");
        jdbc.execute("CREATE TABLE submenu (id INT AUTO_INCREMENT PRIMARY KEY, menu_id INT, nombre VARCHAR(80), ruta VARCHAR(150), "
                + "icono VARCHAR(10), descripcion VARCHAR(255), orden INT, tiene_escritura TINYINT(1) NOT NULL DEFAULT 1)");
        jdbc.execute("CREATE TABLE roles (id INT AUTO_INCREMENT PRIMARY KEY, nombre_rol VARCHAR(40))");
        jdbc.execute("CREATE TABLE rol_submenu (rol_id INT, submenu_id INT)");
        jdbc.update("INSERT INTO submenu (menu_id, nombre, ruta, orden) VALUES (4, 'Buscar producto', 'productos/buscar', 2)");
        jdbc.update("INSERT INTO roles (nombre_rol) VALUES ('ROLE_ADMIN')");
        for (int vez = 1; vez <= 2; vez++) {
            correr(jdbc, "migration_campana.sql");
            correr(jdbc, "migration_campana_personas.sql");
        }

        jdbc.update("INSERT INTO campana (nombre, fecha_inicio, fecha_fin) VALUES ('Reyes', '2026-12-20', '2027-01-06')");
        jdbc.update("INSERT INTO pedidos (total) VALUES (500), (800)");
        jdbc.update("INSERT INTO campana_persona (uuid, nombre, correo, consentimiento) VALUES ('u-1', 'Ana', 'a@m.com', 1)");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO campana_persona (uuid, nombre) VALUES ('u-1', 'Otra')"))
                .as("el código es único").isNotNull();
        jdbc.update("INSERT INTO campana_persona_pedido (campana_id, persona_id, pedido_id, monto) VALUES (1, 1, 1, 500), (1, 1, 2, 800)");
        assertThat(jdbc.queryForObject("SELECT SUM(monto) FROM campana_persona_pedido WHERE persona_id = 1", Integer.class)).isEqualTo(1300);

        jdbc.update("DELETE FROM campana_persona WHERE id = 1");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM campana_persona_pedido", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pedidos", Integer.class)).as("los pedidos no se tocan").isEqualTo(2);
    }
}
