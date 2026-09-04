package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Submenu;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ISubmenuRepository extends BaseRepository<Submenu, Integer> {

    List<Submenu> findByMenuId(Integer menuId);

    long countByMenuId(Integer menuId);
}
