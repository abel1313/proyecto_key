package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.DetallePago;
import com.ventas.key.mis.productos.entity.DetallePagoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IDetallePagoRepository extends JpaRepository<DetallePago, DetallePagoId> {
}