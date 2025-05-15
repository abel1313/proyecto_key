package com.ventas.key.mis.productos.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.ventas.key.mis.productos.entity.LotesProductos;
import com.ventas.key.mis.productos.entity.Producto;

@Repository
public interface ILostesProductosRepository extends BaseRepository<LotesProductos,Integer>{

        Optional<LotesProductos> findByProducto_CodigoBarras_CodigoBarras(String codigoBarras); // 🔍 Busca por código de barras


}
