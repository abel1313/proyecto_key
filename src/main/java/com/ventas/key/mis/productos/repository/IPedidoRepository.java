package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.LotesProductos;
import com.ventas.key.mis.productos.entity.Pedido;
import org.springframework.stereotype.Repository;

@Repository
public interface IPedidoRepository extends BaseRepository<Pedido,Integer>{
}
