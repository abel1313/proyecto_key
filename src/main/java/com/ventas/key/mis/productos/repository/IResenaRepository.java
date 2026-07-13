package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Resena;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IResenaRepository extends BaseRepository<Resena, Integer> {

    Optional<Resena> findByCliente_IdAndVariante_Id(Integer clienteId, Integer varianteId);

    boolean existsByCliente_IdAndVariante_Id(Integer clienteId, Integer varianteId);

    Page<Resena> findByVariante_IdOrderByFechaCreacionDesc(Integer varianteId, Pageable pageable);

    Page<Resena> findByCliente_IdOrderByFechaCreacionDesc(Integer clienteId, Pageable pageable);

    @Query("SELECT AVG(r.calificacion), COUNT(r) FROM Resena r WHERE r.variante.id = :varianteId")
    Object[] resumenPorVariante(@Param("varianteId") Integer varianteId);

    @Query("SELECT r.calificacion, COUNT(r) FROM Resena r WHERE r.variante.id = :varianteId GROUP BY r.calificacion")
    List<Object[]> conteoPorEstrella(@Param("varianteId") Integer varianteId);
}
