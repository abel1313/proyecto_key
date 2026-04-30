package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.ImagenPresentacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IImagenPresentacionRepository extends JpaRepository<ImagenPresentacion, Integer> {
    List<ImagenPresentacion> findByTipoAndActivoOrderByOrden(String tipo, boolean activo);
}