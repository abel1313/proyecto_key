package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.ImagenPresentacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @deprecated Migrar a micro_imagenes. Gestiona imágenes de presentación (LOGIN/REGISTRO)
 * que deben moverse al microservicio de imágenes. No agregar nueva lógica aquí.
 */
@Deprecated
public interface IImagenPresentacionRepository extends JpaRepository<ImagenPresentacion, Integer> {
    List<ImagenPresentacion> findByTipoAndActivoOrderByOrden(String tipo, boolean activo);
}