package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Menu;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IMenuRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MenuServiceImpl extends CrudAbstractServiceImpl<
        Menu,
        List<Menu>,
        Optional<Menu>,
        Integer,
        PginaDto<List<Menu>>> {

    public MenuServiceImpl(IMenuRepository repository, ErrorGenerico error) {
        super(repository, error);
    }
}
