package com.ventas.key.mis.productos.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ventas.key.mis.productos.entity.Cliente;

import java.util.Optional;

@Repository
public interface IClienteRepository extends BaseRepository<Cliente,Integer> {


    @Query("""
    SELECT c FROM Cliente c
    JOIN FETCH c.listDirecciones
    WHERE c.usuario.id = :id
    """)
    Optional<Cliente> findClienteById(@Param("id") int id);


}
