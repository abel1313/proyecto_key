package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.RedSocialNegocio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IRedSocialNegocioRepository extends JpaRepository<RedSocialNegocio, Integer> {
    List<RedSocialNegocio> findByActivoTrue();
}
