package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Favorito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IFavoritoRepository extends BaseRepository<Favorito, Integer> {

    Optional<Favorito> findByCliente_IdAndVariante_Id(Integer clienteId, Integer varianteId);

    boolean existsByCliente_IdAndVariante_Id(Integer clienteId, Integer varianteId);

    void deleteByCliente_IdAndVariante_Id(Integer clienteId, Integer varianteId);

    @Query("SELECT f.variante.id FROM Favorito f WHERE f.cliente.id = :clienteId ORDER BY f.fechaAgregado DESC")
    Page<Integer> findVarianteIdsByClienteId(@Param("clienteId") Integer clienteId, Pageable pageable);

    @Query("SELECT f.variante.id FROM Favorito f WHERE f.cliente.id = :clienteId ORDER BY f.fechaAgregado DESC")
    List<Integer> findAllVarianteIdsByClienteId(@Param("clienteId") Integer clienteId);
}
