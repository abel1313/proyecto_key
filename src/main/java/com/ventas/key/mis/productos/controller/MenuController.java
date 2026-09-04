package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Menu;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.service.MenuServiceImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

// CRUD del catalogo de grupos de menu (ej. "Catalogo", "Envios", "Sistema") -- ver
// PLAN_PERMISOS_PANTALLAS.md. "Dar de alta" un menu nuevo se hace con el POST /save heredado.
@RestController
@RequestMapping("/v1/menu")
public class MenuController extends AbstractController<
        Menu,
        Optional<Menu>,
        List<Menu>,
        Integer,
        PginaDto<List<Menu>>,
        MenuServiceImpl> {

    public MenuController(MenuServiceImpl sGenerico) {
        super(sGenerico);
    }
}
