package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.CodigoPostalGuardado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ICodigoPostalGuardadoRepository extends JpaRepository<CodigoPostalGuardado, Integer> {

    Optional<CodigoPostalGuardado> findByCodigo(String codigo);
}
