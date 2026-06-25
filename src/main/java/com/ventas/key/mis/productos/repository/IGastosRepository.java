package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Gastos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IGastosRepository extends BaseRepository<Gastos, Integer> {

    @Query("SELECT g FROM Gastos g WHERE g.fecha BETWEEN :desde AND :hasta " +
           "AND (:categoria IS NULL OR g.categoria = :categoria) " +
           "ORDER BY g.fecha DESC")
    Page<Gastos> buscar(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("categoria") Gastos.CategoriaGasto categoria,
            Pageable pageable);

    @Query("SELECT g.categoria, SUM(g.monto) FROM Gastos g " +
           "WHERE g.fecha BETWEEN :desde AND :hasta GROUP BY g.categoria")
    List<Object[]> sumPorCategoria(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT SUM(g.monto) FROM Gastos g WHERE g.fecha BETWEEN :desde AND :hasta")
    Double sumTotal(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
