package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.PagosYMeses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IPagosYMesesRepository extends JpaRepository<PagosYMeses, Integer> {

    List<PagosYMeses> findByTipoPago_Id(Integer tipoPagoId);
}