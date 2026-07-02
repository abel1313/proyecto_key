package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IVentaRepository extends BaseRepository<Venta, Integer> {

    Optional<Venta> findByPedidoId(int pedidoId);

    List<Venta> findByClienteIdOrderByFechaVentaDesc(int clienteId);

    @Query("SELECT v FROM Venta v WHERE v.fechaVenta BETWEEN :desde AND :hasta ORDER BY v.fechaVenta DESC")
    Page<Venta> buscarPorFecha(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);

    @Query("SELECT SUM(v.totalVenta), SUM(v.gananciaTotal), COUNT(v) FROM Venta v " +
           "WHERE v.fechaVenta BETWEEN :desde AND :hasta")
    Object[] sumVentas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT FUNCTION('DATE', v.fechaVenta), SUM(v.totalVenta), SUM(v.gananciaTotal), COUNT(v) " +
           "FROM Venta v WHERE v.fechaVenta BETWEEN :desde AND :hasta " +
           "GROUP BY FUNCTION('DATE', v.fechaVenta) ORDER BY FUNCTION('DATE', v.fechaVenta)")
    List<Object[]> sumVentasPorDia(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
