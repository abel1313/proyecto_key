package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PageableDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.pedidos.PedidoGenerico;
import com.ventas.key.mis.productos.models.pedidos.PedidoQuery;
import com.ventas.key.mis.productos.models.pedidos.PedidosDTOPedido;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Optional;

public interface IPedidoService extends ICrud<
        Pedido,
        List<Pedido>,
        Optional<Pedido>,
        Integer,
        PginaDto<List<Pedido>>> {


    Pedido savePedido(@RequestBody PedidosDTOPedido requestG, BindingResult result) throws Exception;
    PedidoGenerico updatePedido(int id, PedidoGenerico requestG) throws Exception;
    PageableDto<List<PedidoGenerico>> obtenerPedido(int id, int size, int pageSize);
    PageableDto<List<PedidoGenerico>> obtenerPedidoPorId(int id, int idCliente, int size, int pageSize);
    PageableDto<List<PedidoGenerico>> buscarClientePorPedido(String buscar, int size, int pageSize);
    void deletePedidoById(int id);



}
