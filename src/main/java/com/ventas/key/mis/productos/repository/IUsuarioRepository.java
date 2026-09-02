package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.models.UsuarioDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUsuarioRepository extends BaseRepository<Usuario,Integer>{
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<Usuario> findFirstByEmailIgnoreCase(String email);


    @Query("""
    SELECT new com.ventas.key.mis.productos.models.UsuarioDto(
        u.id,u.username
        ) FROM Usuario u
    INNER JOIN  Cliente c
        ON c.usuario.id = u.id
    WHERE c.id = :id
    """)
    Optional<UsuarioDto> findUserByIdCliente(@Param("id") int id);



    // 'eliminar' un usuario es soft-delete (enabled=false, ver UsuarioServiceImpl.eliminarUsuario)
    // -- sin el filtro de enabled aqui, un usuario "eliminado" desde el admin seguia apareciendo
    // en el listado para siempre, aunque se recargara la pagina (encontrado 2026-08-25).
    // JOIN FETCH u.roles: evita el N+1 de traer el rol con un SELECT aparte por cada fila de la
    // página (roles es *-to-one, un JOIN FETCH acá es seguro -- no cambia el conteo de filas ni
    // pagina en memoria, a diferencia de hacerlo sobre una colección como permisosExtra).
    @Query("""
    SELECT u
    FROM Usuario u
    JOIN FETCH u.roles
    WHERE u.enabled = true AND u.username LIKE CONCAT('%', :buscar, '%')
""")
    Page<Usuario> findAllPage(@Param("buscar") String buscar, Pageable pageable);

    Page<Usuario> findByEnabledTrue(Pageable pageable);

    // Contraparte para el filtro "ver desactivados" -- deja consultar a quien se le hizo soft-delete
    // (por si fue sin querer) para poder reactivarlo, ver UsuarioServiceImpl.activarUsuario.
    @Query("""
    SELECT u
    FROM Usuario u
    JOIN FETCH u.roles
    WHERE u.enabled = false AND u.username LIKE CONCAT('%', :buscar, '%')
""")
    Page<Usuario> findAllPageInactivos(@Param("buscar") String buscar, Pageable pageable);

    Page<Usuario> findByEnabledFalse(Pageable pageable);


    @Query("SELECT u.cliente.id FROM Usuario u WHERE u.id = :id")
    Integer existsUsuarioByClienteId(@Param("id") Integer id);

    // Usado por RolesServiceImpl.delete() para bloquear el borrado de un rol que todavia
    // tiene usuarios asignados -- sin este chequeo, RolesServiceImpl.delete() no validaba
    // nada antes de borrar (encontrado 2026-08-27, hallazgo del usuario).
    long countByRolesId(Integer rolId);



}
