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


    @Query("""
    SELECT new com.ventas.key.mis.productos.models.UsuarioDto(
        u.id,u.username
        ) FROM Usuario u
    INNER JOIN  Cliente c
        ON c.usuario.id = u.id
    WHERE c.id = :id
    """)
    Optional<UsuarioDto> findUserByIdCliente(@Param("id") int id);



    @Query("""
    SELECT u
    FROM Usuario u
    WHERE u.username LIKE CONCAT('%', :buscar, '%')
""")
    Page<Usuario> findAllPage(@Param("buscar") String buscar, Pageable pageable);


    @Query("SELECT u.cliente.id FROM Usuario u WHERE u.id = :id")
    int existsUsuarioByClienteId(@Param("id") int id);



}
