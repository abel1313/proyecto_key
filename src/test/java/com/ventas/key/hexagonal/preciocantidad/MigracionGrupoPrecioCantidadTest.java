package com.ventas.key.hexagonal.preciocantidad;

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

/**
 * {@code migration_grupo_precio_cantidad.sql} corre completa, dos veces seguidas (regla del proyecto): crea las dos tablas, hace único al
 * producto (un producto, un grupo), borra su fila si se borra el producto y da la pantalla solo a ROLE_ADMIN, una sola vez.
 */
class MigracionGrupoPrecioCantidadTest {

    @Test
    @DisplayName("la migración es idempotente, un producto no puede estar en dos grupos y la pantalla es solo del administrador")
    void correDosVeces() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:mig" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE producto (id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(80))");
        jdbc.execute("CREATE TABLE submenu (id INT AUTO_INCREMENT PRIMARY KEY, menu_id INT, nombre VARCHAR(80), ruta VARCHAR(150), "
                + "icono VARCHAR(10), descripcion VARCHAR(255), orden INT, tiene_escritura TINYINT(1) NOT NULL DEFAULT 1)");
        jdbc.execute("CREATE TABLE roles (id INT AUTO_INCREMENT PRIMARY KEY, nombre_rol VARCHAR(40))");
        jdbc.execute("CREATE TABLE rol_submenu (rol_id INT, submenu_id INT)");
        jdbc.update("INSERT INTO submenu (menu_id, nombre, ruta, orden) VALUES (4, 'Buscar producto', 'productos/buscar', 2), (3, 'Otro', 'otro', 1)");
        jdbc.update("INSERT INTO roles (nombre_rol) VALUES ('ROLE_ADMIN'), ('ROLE_EMPLEADO')");
        jdbc.update("INSERT INTO producto (nombre) VALUES ('Bolsa A'), ('Bolsa B')");

        String sql = Files.readString(Path.of("src/main/resources/static/migration_grupo_precio_cantidad.sql"));
        String[] sentencias = Arrays.stream(sql.split("\n")).filter(l -> !l.trim().startsWith("--")).reduce("", (a, b) -> a + b + "\n").split(";");
        for (int vez = 1; vez <= 2; vez++) {
            for (String s : sentencias) {
                if (!s.isBlank()) jdbc.execute(s);
            }
        }

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM submenu WHERE ruta = 'productos/grupos-precio'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT menu_id FROM submenu WHERE ruta = 'productos/grupos-precio'", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForList("SELECT ro.nombre_rol FROM rol_submenu rs JOIN roles ro ON ro.id = rs.rol_id "
                + "JOIN submenu s ON s.id = rs.submenu_id WHERE s.ruta = 'productos/grupos-precio'", String.class))
                .containsExactly("ROLE_ADMIN");

        // Un producto, un grupo.
        jdbc.update("INSERT INTO grupo_precio_cantidad (nombre, minimo_piezas) VALUES ('G1', 3), ('G2', 2)");
        jdbc.update("INSERT INTO grupo_precio_cantidad_producto (grupo_id, producto_id, precio_promocion) VALUES (1, 1, 200)");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO grupo_precio_cantidad_producto (grupo_id, producto_id, precio_promocion) VALUES (2, 1, 150)"))
                .as("el mismo producto en otro grupo debe rechazarse").isNotNull();

        // Si se borra el producto, su fila en el grupo se borra sola.
        jdbc.update("DELETE FROM producto WHERE id = 1");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM grupo_precio_cantidad_producto", Integer.class)).isZero();
    }
}
