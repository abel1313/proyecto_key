package com.ventas.key.hexagonal.preciocantidad;

import com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia.GruposDePrecioJpaAdapter;
import com.ventas.key.mis.productos.repository.IGrupoPrecioCantidadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * En un ambiente donde NO se ha corrido {@code migration_grupo_precio_cantidad.sql}, el cálculo del precio por cantidad no consulta las
 * tablas (si lo hiciera, el error dentro de la transacción de la venta la deshace): devuelve «ningún grupo» y la venta sigue como siempre.
 */
class SinMigracionNoRompeLasVentasTest {

    @Test
    void sinTablas_noHayGrupos_yNoSeTocaLaBase() {
        var datos = new DriverManagerDataSource("jdbc:h2:mem:sinmig" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var repositorio = mock(IGrupoPrecioCantidadRepository.class);
        var adaptador = new GruposDePrecioJpaAdapter(repositorio, datos);

        assertThat(adaptador.activosConProductos(Set.of(1, 2))).isEmpty();
        verifyNoInteractions(repositorio);
    }

    @Test
    void conTablas_consultaNormalmente() {
        var datos = new DriverManagerDataSource("jdbc:h2:mem:conmig" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(datos);
        jdbc.execute("CREATE TABLE grupo_precio_cantidad (id INT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE grupo_precio_cantidad_producto (id INT PRIMARY KEY)");
        var repositorio = mock(IGrupoPrecioCantidadRepository.class);
        var adaptador = new GruposDePrecioJpaAdapter(repositorio, datos);

        adaptador.activosConProductos(Set.of(1));
        org.mockito.Mockito.verify(repositorio).activosConProductos(Set.of(1));
    }
}
