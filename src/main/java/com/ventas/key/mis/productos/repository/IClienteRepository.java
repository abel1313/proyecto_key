package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.models.ClienteBusquedaDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IClienteRepository extends BaseRepository<Cliente,Integer> {

    // Usado por ClienteServiceImpl.solicitarCambioCorreo/verificarCorreo para validar que el
    // correo nuevo no pertenezca ya a otro cliente ANTES de guardarlo -- mismo criterio que
    // IUsuarioRepository.findFirstByEmailIgnoreCase (hotfix 2026-09-05, ver ExceptionGlobal).
    Optional<Cliente> findFirstByCorreoElectronicoIgnoreCase(String correoElectronico);

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

    // UPDATE directo (no entity.save()) a proposito -- confirmarCambioCorreo/verificarCorreo
    // NO deben disparar la validacion Bean Validation de la entidad COMPLETA (numeroTelefonico
    // @NotBlank/@Pattern), que revienta el commit si el cliente tiene el telefono vacio o mal
    // formado (dato viejo, de antes de esa validacion) aunque no se este tocando ese campo
    // (encontrado 2026-09-05, hotfix urgente en prod -- ConstraintViolationException en
    // numeroTelefonico al confirmar un cambio de correo que no lo toca para nada).
    @Modifying
    @Query("UPDATE Cliente c SET c.correoElectronico = :correo WHERE c.id = :id")
    void actualizarCorreoElectronico(@Param("id") Integer id, @Param("correo") String correo);

    @Modifying
    @Query("""
        UPDATE Cliente c SET c.correoElectronico = :correo, c.correoPendiente = null,
               c.correoVerificado = true, c.codigoVerificacion = null, c.codigoVerificacionExpira = null
        WHERE c.id = :id
        """)
    void confirmarCorreoConCambioPendiente(@Param("id") Integer id, @Param("correo") String correo);

    @Modifying
    @Query("""
        UPDATE Cliente c SET c.correoVerificado = true, c.codigoVerificacion = null,
               c.codigoVerificacionExpira = null
        WHERE c.id = :id
        """)
    void confirmarCorreoSinCambioPendiente(@Param("id") Integer id);

    // Mismos UPDATE directos que arriba, para los otros 2 puntos de ClienteServiceImpl que
    // hacian entity.setX(...) + save() de la entidad completa (mismo riesgo de
    // ConstraintViolationException en numeroTelefonico, hotfix 2026-09-05).
    @Modifying
    @Query("""
        UPDATE Cliente c SET c.correoPendiente = :correoPendiente, c.codigoVerificacion = :codigo,
               c.codigoVerificacionExpira = :expira
        WHERE c.id = :id
        """)
    void actualizarCorreoPendienteConCodigo(@Param("id") Integer id,
            @Param("correoPendiente") String correoPendiente, @Param("codigo") String codigo,
            @Param("expira") java.time.LocalDateTime expira);

    @Modifying
    @Query("""
        UPDATE Cliente c SET c.codigoVerificacion = :codigo, c.codigoVerificacionExpira = :expira
        WHERE c.id = :id
        """)
    void actualizarCodigoVerificacion(@Param("id") Integer id, @Param("codigo") String codigo,
            @Param("expira") java.time.LocalDateTime expira);

    @Modifying
    @Query("""
        UPDATE Cliente c SET c.correoPendiente = null, c.codigoVerificacion = null,
               c.codigoVerificacionExpira = null
        WHERE c.id = :id
        """)
    void limpiarCorreoPendiente(@Param("id") Integer id);

    // Usado por PromocionServiceImpl.enviarCorreoPromocionAsync -- solo clientes que activaron
    // el checkbox de promociones Y ya tienen el correo verificado (no tiene caso mandarle una
    // promocion a un correo que ni siquiera se confirmo que existe). Orden explicito por id para
    // que la paginacion en lotes de 10 sea estable entre paginas.
    @Query("""
    SELECT c FROM Cliente c
    WHERE c.recibirPromociones = true AND c.correoVerificado = true
    ORDER BY c.id
    """)
    Page<Cliente> findElegiblesParaCorreoPromociones(Pageable pageable);

    @Query("""
    SELECT COUNT(c) FROM Cliente c
    WHERE c.recibirPromociones = true AND c.correoVerificado = true
    """)
    long contarElegiblesParaCorreoPromociones();

}
