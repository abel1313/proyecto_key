package com.ventas.key.mis.productos.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.ventas.key.mis.productos.entity.DetalleVenta;
import com.ventas.key.mis.productos.entity.LotesProductos;
import com.ventas.key.mis.productos.entity.Producto;

@Repository
public interface IDetalleVentaRepository extends BaseRepository<DetalleVenta, Integer>{

}
