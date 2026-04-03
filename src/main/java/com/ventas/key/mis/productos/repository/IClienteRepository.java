package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Cliente;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IClienteRepository extends BaseRepository<Cliente,Integer> {


    @Query("""
    SELECT c FROM Cliente c
    WHERE c.usuario.id = :id
    """)
    Optional<Cliente> findClienteById(@Param("id") int id);


}
