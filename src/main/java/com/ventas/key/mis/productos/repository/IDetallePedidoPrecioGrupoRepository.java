package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.DetallePedidoPrecioGrupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface IDetallePedidoPrecioGrupoRepository extends JpaRepository<DetallePedidoPrecioGrupo, Integer> {

    List<DetallePedidoPrecioGrupo> findByDetallePedidoIdIn(Collection<Integer> detalleIds);
}
