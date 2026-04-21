package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.models.ClienteBusquedaDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("""
    SELECT new com.ventas.key.mis.productos.models.ClienteBusquedaDto(
        c.id, c.nombrePersona, c.apeidoPaterno, c.apeidoMaterno, c.correoElectronico, c.numeroTelefonico)
    FROM Cliente c
    WHERE LOWER(c.nombrePersona) LIKE LOWER(CONCAT('%', :nombre, '%'))
       OR LOWER(c.apeidoPaterno) LIKE LOWER(CONCAT('%', :nombre, '%'))
       OR LOWER(c.apeidoMaterno) LIKE LOWER(CONCAT('%', :nombre, '%'))
    """)
    Page<ClienteBusquedaDto> buscarPorNombre(@Param("nombre") String nombre, Pageable pageable);

}
