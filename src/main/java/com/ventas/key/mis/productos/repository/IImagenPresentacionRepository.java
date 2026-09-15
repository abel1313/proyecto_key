package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.ImagenPresentacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * @deprecated Migrar a micro_imagenes. Gestiona imágenes de presentación (LOGIN/REGISTRO)
 * que deben moverse al microservicio de imágenes. No agregar nueva lógica aquí.
 */
@Deprecated
public interface IImagenPresentacionRepository extends JpaRepository<ImagenPresentacion, Integer> {
    List<ImagenPresentacion> findByTipoAndActivoOrderByOrden(String tipo, boolean activo);

    // Los archivos de presentacion viven en el MISMO directorio que las imagenes de producto
    // (guardar-imagenes.ruta_imagenes), pero en otra tabla. La limpieza de huerfanos de las 4 AM
    // (ReconciliacionImagenService.limpiarDiscoDia) solo conocia los nombres de la tabla `imagen`,
    // asi que borraba estos archivos todas las noches y el login/registro amanecia sin imagenes.
    @Query("SELECT p.nombreArchivo FROM ImagenPresentacion p WHERE p.nombreArchivo IS NOT NULL")
    List<String> findAllNombresArchivo();
}