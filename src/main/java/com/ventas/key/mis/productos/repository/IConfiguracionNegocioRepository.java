package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.ConfiguracionNegocio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IConfiguracionNegocioRepository extends JpaRepository<ConfiguracionNegocio, Integer> {
}