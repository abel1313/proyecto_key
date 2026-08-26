package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Submenu;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.SubmenuServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

// CRUD del catalogo de items de menu (ej. "Modelos" -> productos/buscar) -- ver
// PLAN_PERMISOS_PANTALLAS.md. "Dar de alta" un submenu nuevo se hace con el POST /save heredado.
@RestController
@RequestMapping("/v1/submenu")
public class SubmenuController extends AbstractController<
        Submenu,
        Optional<Submenu>,
        List<Submenu>,
        Integer,
        PginaDto<List<Submenu>>,
        SubmenuServiceImpl> {

    public SubmenuController(SubmenuServiceImpl sGenerico) {
        super(sGenerico);
    }

    // Submenus de un solo grupo -- lo usara la pantalla de "Gestion de roles" al desplegar
    // un grupo del acordeon (ver flujo descrito en PLAN_PERMISOS_PANTALLAS.md).
    @GetMapping("/porMenu/{menuId}")
    public ResponseEntity<ResponseGeneric<List<Submenu>>> porMenu(@PathVariable Integer menuId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<List<Submenu>>(sGenerico.porMenu(menuId)));
        } catch (Exception e) {
            ResponseGeneric<List<Submenu>> error = new ResponseGeneric<List<Submenu>>((List<Submenu>) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
