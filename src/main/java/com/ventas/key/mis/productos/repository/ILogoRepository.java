package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Logo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ILogoRepository extends JpaRepository<Logo, Integer> {
    List<Logo> findAllByOrderByCreadoEnDesc();
    Optional<Logo> findByActivoTrue();
}
