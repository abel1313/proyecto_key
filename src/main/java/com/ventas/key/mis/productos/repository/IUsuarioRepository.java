package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.Rifa;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.models.UsuarioDto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUsuarioRepository extends BaseRepository<Usuario,Integer>{
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByRol(String rol);

    @Query("""
    SELECT new com.ventas.key.mis.productos.models.UsuarioDto(
        u.id,u.username
        ) FROM Usuario u
    INNER JOIN  Cliente c
        ON c.usuario.id = u.id
    WHERE c.id = :id
    """)
    Optional<UsuarioDto> findUserByIdCliente(@Param("id") int id);
}
