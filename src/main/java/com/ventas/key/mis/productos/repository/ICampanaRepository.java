package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.CampanaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ICampanaRepository extends JpaRepository<CampanaEntidad, Integer> {

    @Query("SELECT DISTINCT c FROM CampanaEntidad c LEFT JOIN FETCH c.productos ORDER BY c.fechaInicio DESC, c.nombre")
    List<CampanaEntidad> listarConProductos();

    @Query("SELECT c FROM CampanaEntidad c LEFT JOIN FETCH c.productos WHERE c.id = :id")
    Optional<CampanaEntidad> buscarConProductos(@Param("id") Integer id);

    @Query("SELECT DISTINCT c FROM CampanaEntidad c LEFT JOIN FETCH c.productos WHERE c.activa = true")
    List<CampanaEntidad> activasConProductos();

    @Modifying
    @Query("UPDATE CampanaEntidad c SET c.activa = false WHERE c.id <> :id AND c.activa = true")
    int apagarLasDemas(@Param("id") Integer id);
}
