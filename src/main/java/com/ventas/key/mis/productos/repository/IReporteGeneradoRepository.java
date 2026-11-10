package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.ReporteGeneradoEntidad;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IReporteGeneradoRepository extends JpaRepository<ReporteGeneradoEntidad, Integer> {

    /** La lista no trae el archivo (pesa): cada fila es [id, estado, formato, descripcion, nombreArchivo, error, creado, terminado, expira, tamaño]. */
    @Query("select r.id, r.estado, r.formato, r.descripcion, r.nombreArchivo, r.error, r.creado, r.terminado, r.expira, "
            + "coalesce(r.tamano, 0) "
            + "from ReporteGeneradoEntidad r where r.usuario = :usuario order by r.id desc")
    List<Object[]> resumenesDe(@Param("usuario") String usuario, Pageable pageable);

    @Modifying
    @Query("delete from ReporteGeneradoEntidad r where r.expira < :ahora")
    int borrarVencidos(@Param("ahora") LocalDateTime ahora);

    long countByUsuarioAndEstadoIn(String usuario, java.util.Collection<String> estados);
}
