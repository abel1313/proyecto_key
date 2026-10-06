package com.ventas.key.hexagonal.busquedapedido.aplicacion.servicio;

import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.FiltroPedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.PaginaDePedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.puerto.entrada.BuscarPedidosCasoUso;
import com.ventas.key.hexagonal.busquedapedido.dominio.puerto.salida.HoyPort;
import com.ventas.key.hexagonal.busquedapedido.dominio.puerto.salida.PedidosFiltradosPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Use Case interactor]
 *
 * <p>Solo orquesta: el filtro ya llega validado (su constructor) y la consulta la hace el puerto.
 * Sin cache a proposito: la lista cambia con cada abono, venta y cancelacion.
 */
@Service
@RequiredArgsConstructor
public class BuscarPedidosService implements BuscarPedidosCasoUso {

    private final PedidosFiltradosPort pedidos;
    private final HoyPort reloj;

    @Override
    @Transactional(readOnly = true)
    public PaginaDePedidos buscar(FiltroPedidos filtro) {
        return pedidos.buscar(filtro, reloj.hoy());
    }
}
