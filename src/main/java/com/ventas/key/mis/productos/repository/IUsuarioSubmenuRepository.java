package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.UsuarioSubmenu;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IUsuarioSubmenuRepository extends BaseRepository<UsuarioSubmenu, Integer> {

    List<UsuarioSubmenu> findByUsuarioId(Integer usuarioId);

    Optional<UsuarioSubmenu> findByUsuarioIdAndSubmenuId(Integer usuarioId, Integer submenuId);
}
