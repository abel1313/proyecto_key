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

/** {@code migration_precio_base_linea_grupo.sql} corre completa, dos veces seguidas (regla del proyecto), y su fila se borra sola con la línea. */
class MigracionPrecioBaseLineaGrupoTest {

    @Test
    @DisplayName("la migración es idempotente y la fila se borra sola cuando se borra la línea del pedido")
    void correDosVeces() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:mig" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE detalle_pedidos (id INT AUTO_INCREMENT PRIMARY KEY, cantidad INT)");
        jdbc.update("INSERT INTO detalle_pedidos (cantidad) VALUES (1), (2)");

        String sql = Files.readString(Path.of("src/main/resources/static/migration_precio_base_linea_grupo.sql"));
        String[] sentencias = Arrays.stream(sql.split("\n")).filter(l -> !l.trim().startsWith("--")).reduce("", (a, b) -> a + b + "\n").split(";");
        for (int vez = 1; vez <= 2; vez++) {
            for (String s : sentencias) {
                if (!s.isBlank()) jdbc.execute(s);
            }
        }

        jdbc.update("INSERT INTO detalle_pedido_precio_grupo (detalle_pedido_id, precio_sin_grupo) VALUES (1, 250.00), (2, 300.00)");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM detalle_pedido_precio_grupo", Integer.class)).isEqualTo(2);

        jdbc.update("DELETE FROM detalle_pedidos WHERE id = 1");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM detalle_pedido_precio_grupo", Integer.class)).isEqualTo(1);
    }
}
