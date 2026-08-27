package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Roles;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.RolesServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

// CRUD de roles (crear/editar/borrar) -- antes solo existia GET /v1/usuarios/roles para listar.
// Fase 1 de PLAN_PERMISOS_PANTALLAS.md (repo compartido): asignacion de pantallas (Submenu) por
// rol, para que los roles dejen de ser los 4 fijos y el admin pueda crear los que necesite.
@RestController
@RequestMapping("/v1/roles")
public class RolesController extends AbstractController<
        Roles,
        Optional<Roles>,
        List<Roles>,
        Integer,
        PginaDto<List<Roles>>,
        RolesServiceImpl> {

    public RolesController(RolesServiceImpl sGenerico) {
        super(sGenerico);
    }

    @PostMapping("/{rolId}/submenus/{submenuId}")
    public ResponseEntity<ResponseGeneric<Roles>> agregarSubmenu(
            @PathVariable Integer rolId, @PathVariable Integer submenuId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.agregarSubmenu(rolId, submenuId)));
        } catch (Exception e) {
            ResponseGeneric<Roles> error = new ResponseGeneric<>((Roles) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/{rolId}/submenus/{submenuId}")
    public ResponseEntity<ResponseGeneric<Roles>> quitarSubmenu(
            @PathVariable Integer rolId, @PathVariable Integer submenuId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.quitarSubmenu(rolId, submenuId)));
        } catch (Exception e) {
            ResponseGeneric<Roles> error = new ResponseGeneric<>((Roles) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Fase 2 de permisos de accion: ademas de VER la pantalla (arriba), puede ESCRIBIR en ella
    // (crear/editar/borrar). Requiere tener ya el "ver" -- ver RolesServiceImpl.agregarSubmenuEscritura.
    @PostMapping("/{rolId}/submenus/{submenuId}/escritura")
    public ResponseEntity<ResponseGeneric<Roles>> agregarSubmenuEscritura(
            @PathVariable Integer rolId, @PathVariable Integer submenuId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.agregarSubmenuEscritura(rolId, submenuId)));
        } catch (Exception e) {
            ResponseGeneric<Roles> error = new ResponseGeneric<>((Roles) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/{rolId}/submenus/{submenuId}/escritura")
    public ResponseEntity<ResponseGeneric<Roles>> quitarSubmenuEscritura(
            @PathVariable Integer rolId, @PathVariable Integer submenuId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.quitarSubmenuEscritura(rolId, submenuId)));
        } catch (Exception e) {
            ResponseGeneric<Roles> error = new ResponseGeneric<>((Roles) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Fase 3 de permisos (piloto en Modelos): acciones puntuales dentro de una pantalla (ej.
    // "eliminar", "habilitar"), independientes del Editar general. Requiere tener ya el "ver" de
    // la pantalla dueña de la acción -- ver RolesServiceImpl.agregarAccion.
    @PostMapping("/{rolId}/acciones/{accionId}")
    public ResponseEntity<ResponseGeneric<Roles>> agregarAccion(
            @PathVariable Integer rolId, @PathVariable Integer accionId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.agregarAccion(rolId, accionId)));
        } catch (Exception e) {
            ResponseGeneric<Roles> error = new ResponseGeneric<>((Roles) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/{rolId}/acciones/{accionId}")
    public ResponseEntity<ResponseGeneric<Roles>> quitarAccion(
            @PathVariable Integer rolId, @PathVariable Integer accionId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.quitarAccion(rolId, accionId)));
        } catch (Exception e) {
            ResponseGeneric<Roles> error = new ResponseGeneric<>((Roles) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
