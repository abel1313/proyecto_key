package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.PedidoACobrar;
import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IGrupoPedidoMiembroRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * En un ambiente donde todavia no se corrio migration_pago_tarjeta.sql (por ejemplo produccion) la columna
 * {@code acepta_msi} no existe: leer el pedido para cobrar de contado NO debe tronar, solo nadie acepta MSI.
 */
class PedidoCobroAdapterSinColumnaMsiTest {

    @Test
    void sinLaColumnaNoTruena_soloNadieAceptaMsi() {
        IPedidoRepository pedidos = mock(IPedidoRepository.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        IGrupoPedidoMiembroRepository grupos = mock(IGrupoPedidoMiembroRepository.class);
        when(jdbc.queryForList(any(String.class), eq(Integer.class), any(Object[].class)))
                .thenThrow(new InvalidDataAccessResourceUsageException("Unknown column 'acepta_msi'"));
        when(grupos.estaEnGrupoActivo(anyInt())).thenReturn(false);

        Pedido p = new Pedido();
        p.setId(5);
        p.setTipoPedido("NORMAL");
        p.setEstadoPedido("PENDIENTE");
        p.setTotalPedido(450.0);
        Producto producto = new Producto();
        producto.setNombre("Bolsa");
        Variantes variante = new Variantes();
        variante.setId(9);
        variante.setProducto(producto);
        DetallePedido linea = new DetallePedido();
        linea.setVariante(variante);
        linea.setProducto(producto);
        p.setDetalles(List.of(linea));
        when(pedidos.findById(5)).thenReturn(Optional.of(p));

        PedidoCobroAdapter adaptador = new PedidoCobroAdapter(pedidos, null, null, null, null, null, jdbc, grupos, null);
        PedidoACobrar r = adaptador.buscar(5).orElseThrow();

        assertThat(r.articulos()).hasSize(1);
        assertThat(r.articulos().get(0).aceptaMsi()).isFalse();
        assertThat(r.total()).isEqualByComparingTo("450.00");
    }
}
