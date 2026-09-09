package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.AccionSubmenu;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.repository.IAccionSubmenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Catalogo de acciones granulares por pantalla (Fase 3 de permisos, piloto en Modelos
// 2026-08-27) -- ver AccionSubmenu. Solo lectura: el catalogo se da de alta por SQL (mismo
// criterio que el arranque de Menu/Submenu), esta pantalla no tiene un CRUD propio todavia. Lo
// consume Gestión de roles para pintar el checklist de acciones de cada pantalla.
@RestController
@RequestMapping("/v1/accion-submenu")
@RequiredArgsConstructor
public class AccionSubmenuController {

    private final IAccionSubmenuRepository repository;

    @GetMapping("/getAll")
    public ResponseGeneric<List<AccionSubmenu>> getAll() {
        return new ResponseGeneric<List<AccionSubmenu>>(repository.findAll());
    }
}
