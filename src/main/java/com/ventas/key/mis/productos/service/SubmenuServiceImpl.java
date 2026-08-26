package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Submenu;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.ISubmenuRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubmenuServiceImpl extends CrudAbstractServiceImpl<
        Submenu,
        List<Submenu>,
        Optional<Submenu>,
        Integer,
        PginaDto<List<Submenu>>> {

    private final ISubmenuRepository iSubmenuRepository;

    public SubmenuServiceImpl(ISubmenuRepository repository, ErrorGenerico error) {
        super(repository, error);
        this.iSubmenuRepository = repository;
    }

    public List<Submenu> porMenu(Integer menuId) {
        return iSubmenuRepository.findByMenuId(menuId);
    }
}
