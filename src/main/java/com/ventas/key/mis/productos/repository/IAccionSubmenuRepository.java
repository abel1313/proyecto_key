package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.AccionSubmenu;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IAccionSubmenuRepository extends BaseRepository<AccionSubmenu, Integer> {

    List<AccionSubmenu> findBySubmenuId(Integer submenuId);
}
