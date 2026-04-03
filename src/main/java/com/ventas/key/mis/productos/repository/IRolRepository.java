package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Roles;
import org.springframework.stereotype.Repository;

import javax.management.relation.Role;
import java.util.Optional;

@Repository
public interface IRolRepository extends BaseRepository<Roles,Integer>{

    Optional<Roles> findByNombreRol(String rol);
}
