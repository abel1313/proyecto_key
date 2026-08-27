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

    // Filtra por c.id (el id de Cliente de verdad) -- el nombre del metodo, el @Operation de
    // ClienteControllerImpl.findByIdCliente() y su chequeo de dueno (esDueno comparando contra
    // actual.getCliente().getId()) siempre asumieron esto. Antes filtraba por c.usuario.id, lo
    // que dejaba el metodo con DOS semanticas distintas dentro del mismo endpoint: el chequeo de
    // seguridad esperaba clienteId (y por eso mandar usuarioId daba "No autorizado", ver el
    // historial de fixes en mis-pedidos.component.ts/detalle-productos.component.ts del front) y
    // la consulta esperaba usuarioId -- asi que mandando el clienteId correcto (el que SI pasaba
    // el chequeo de dueno) la consulta no encontraba nada real, salvo que Cliente.id coincidiera
    // por casualidad con Usuario.id (encontrado 2026-08-27, auditoria de correctitud). El unico
    // caller interno que de verdad queria buscar por usuario.id (ClienteServiceImpl.
    // crearClienteDesdeRegistro) ahora usa findByUsuarioId(), abajo.
    @Query("""
    SELECT c FROM Cliente c
    WHERE c.id = :id
    """)
    Optional<Cliente> findClienteById(@Param("id") int id);

    @Query("""
    SELECT c FROM Cliente c
    WHERE c.usuario.id = :usuarioId
    """)
    Optional<Cliente> findByUsuarioId(@Param("usuarioId") int usuarioId);

    @Query("""
    SELECT new com.ventas.key.mis.productos.models.ClienteBusquedaDto(
        c.id, c.nombrePersona, c.apeidoPaterno, c.apeidoMaterno, c.correoElectronico, c.numeroTelefonico, c.correoVerificado)
    FROM Cliente c
    WHERE LOWER(CONCAT(
            COALESCE(c.nombrePersona, ''), ' ',
            COALESCE(c.apeidoPaterno, ''), ' ',
            COALESCE(c.apeidoMaterno, '')
          )) LIKE LOWER(CONCAT('%', :nombre, '%'))
    """)
    Page<ClienteBusquedaDto> buscarPorNombre(@Param("nombre") String nombre, Pageable pageable);

}
