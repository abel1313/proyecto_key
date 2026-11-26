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

    /** Los reportes del usuario que de verdad se están armando: en esos estados Y pedidos después de {@code desde} (los más viejos son zombies). */
    long countByUsuarioAndEstadoInAndCreadoAfter(String usuario, java.util.Collection<String> estados, LocalDateTime desde);

    /** Pasa a ERROR los PENDIENTE/GENERANDO pedidos antes de {@code limite} (se quedaron sin terminar: reinicio del pod, tarea perdida). No toca el archivo. */
    @Modifying
    @Query("update ReporteGeneradoEntidad r set r.estado = 'ERROR', r.error = :mensaje, r.terminado = :ahora "
            + "where r.estado in ('PENDIENTE', 'GENERANDO') and r.creado < :limite")
    int marcarErrorLosAtorados(@Param("limite") LocalDateTime limite, @Param("mensaje") String mensaje, @Param("ahora") LocalDateTime ahora);

    /** Pasa a ERROR un reporte que sigue PENDIENTE (no se pudo ni encolar). No toca el archivo. */
    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("update ReporteGeneradoEntidad r set r.estado = 'ERROR', r.error = :mensaje, r.terminado = :ahora "
            + "where r.id = :id and r.estado = 'PENDIENTE'")
    int marcarErrorSiPendiente(@Param("id") int id, @Param("mensaje") String mensaje, @Param("ahora") LocalDateTime ahora);
}
