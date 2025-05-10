package com.ventas.key.mis.productos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ventas.key.mis.productos.entity.Venta;
import com.ventas.key.mis.productos.models.TotalDetalle;

@Repository
public interface IVentaRepository extends BaseRepository<Venta, Integer>{


}
