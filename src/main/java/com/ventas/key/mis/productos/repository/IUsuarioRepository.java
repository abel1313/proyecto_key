package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Rifa;
import com.ventas.key.mis.productos.entity.Usuario;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUsuarioRepository extends BaseRepository<Usuario,Integer>{
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByRol(String rol);
}
