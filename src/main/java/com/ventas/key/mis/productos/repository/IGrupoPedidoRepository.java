package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.GrupoPedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IGrupoPedidoRepository extends JpaRepository<GrupoPedido, Integer> {
}
