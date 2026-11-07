package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.GrupoPrecioCantidadEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IGrupoPrecioCantidadRepository extends JpaRepository<GrupoPrecioCantidadEntidad, Integer> {

    @Query("SELECT DISTINCT g FROM GrupoPrecioCantidadEntidad g LEFT JOIN FETCH g.productos ORDER BY g.nombre")
    List<GrupoPrecioCantidadEntidad> listarConProductos();

    @Query("SELECT g FROM GrupoPrecioCantidadEntidad g LEFT JOIN FETCH g.productos WHERE g.id = :id")
    Optional<GrupoPrecioCantidadEntidad> buscarConProductos(@Param("id") Integer id);

    @Query("SELECT DISTINCT g FROM GrupoPrecioCantidadEntidad g LEFT JOIN FETCH g.productos gp "
            + "WHERE g.activo = true AND g.id IN (SELECT x.grupo.id FROM GrupoPrecioCantidadProductoEntidad x WHERE x.producto.id IN :productoIds)")
    List<GrupoPrecioCantidadEntidad> activosConProductos(@Param("productoIds") Collection<Integer> productoIds);

    @Query("SELECT g FROM GrupoPrecioCantidadEntidad g LEFT JOIN FETCH g.productos gp WHERE g.id = "
            + "(SELECT x.grupo.id FROM GrupoPrecioCantidadProductoEntidad x WHERE x.producto.id = :productoId)")
    Optional<GrupoPrecioCantidadEntidad> deProducto(@Param("productoId") Integer productoId);
}
