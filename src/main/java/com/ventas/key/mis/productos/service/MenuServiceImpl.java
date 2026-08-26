package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Menu;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IMenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MenuServiceImpl extends CrudAbstractServiceImpl<
        Menu,
        List<Menu>,
        Optional<Menu>,
        Integer,
        PginaDto<List<Menu>>> {

    private final IMenuRepository iMenuRepository;

    public MenuServiceImpl(IMenuRepository repository, ErrorGenerico error) {
        super(repository, error);
        this.iMenuRepository = repository;
    }

    // La implementacion base (CrudAbstractServiceImpl.delete) no hace nada -- hay que
    // sobreescribirla para que si borre (mismo gotcha que LugarEntregaServiceImpl.delete).
    // Los Submenu de este Menu se van con el (FK menu_id ON DELETE CASCADE, ver
    // migration_menu_submenu.sql).
    @Transactional
    @Override
    public Menu delete(Integer id) throws Exception {
        Menu menu = iMenuRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Menu no encontrado: " + id));
        iMenuRepository.delete(menu);
        return menu;
    }
}
