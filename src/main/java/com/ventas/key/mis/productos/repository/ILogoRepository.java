package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Logo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ILogoRepository extends JpaRepository<Logo, Integer> {
    List<Logo> findAllByOrderByCreadoEnDesc();
    Optional<Logo> findByActivoTrue();

    // Mismo caso que ImagenPresentacion: los logos se guardan en el directorio compartido de
    // imagenes y la limpieza nocturna de huerfanos no los reconocia como validos.
    @Query("SELECT l.nombreArchivo FROM Logo l WHERE l.nombreArchivo IS NOT NULL")
    List<String> findAllNombresArchivo();
}
