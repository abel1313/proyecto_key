package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Permiso;
import com.ventas.key.mis.productos.entity.Roles;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.mapper.UserDto;
import com.ventas.key.mis.productos.mapper.UserUpdate;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.UsuarioServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("usuarios")
public class UsuarioController extends AbstractController<
        Usuario,
        Optional<Usuario>,
        List<Usuario>,
        Integer,
        PginaDto<List<Usuario>>,
        UsuarioServiceImpl> {

    private final UsuarioServiceImpl usu;

    public UsuarioController(UsuarioServiceImpl usuarioService) {
        super(usuarioService);
        this.usu = usuarioService;
    }

    @GetMapping("/getAllPage")
    public ResponseEntity<ResponseGeneric<PginaDto<List<UserDto>>>> findAllPage(
            @RequestParam String buscar,
            @RequestParam int page,
            @RequestParam int size) {
        PginaDto<List<UserDto>> result = usu.findAllPage(page, size, buscar);
        return ResponseEntity.ok(new ResponseGeneric<>(result));
    }

    @PutMapping("/updateUsuario/{id}")
    public ResponseEntity<ResponseGeneric<UserUpdate>> updateUsuario(
            @RequestBody UserUpdate usuarioDto,
            @PathVariable int id) {
        UserUpdate result = usu.updateUserDto(usuarioDto, id);
        return ResponseEntity.ok(new ResponseGeneric<>(result));
    }

    @DeleteMapping("/eliminarUsuarioDto/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable int id) {
        usu.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscarClientePorIdUsuario/{idUsuario}")
    public ResponseEntity<Integer> existeClientePorIdUsuario(@PathVariable int idUsuario) {
        return ResponseEntity.ok(usu.existeClientePorIdUsuario(idUsuario));
    }

    // ── Gestión de roles y permisos (solo ADMIN / USUARIOS_GESTIONAR) ──────────

    @GetMapping("/roles")
    public ResponseEntity<List<Roles>> listarRoles() {
        return ResponseEntity.ok(usu.listarRoles());
    }

    @GetMapping("/permisos")
    public ResponseEntity<List<Permiso>> listarPermisos() {
        return ResponseEntity.ok(usu.listarPermisos());
    }

    @PutMapping("/{usuarioId}/rol/{rolId}")
    public ResponseEntity<UserDto> cambiarRol(
            @PathVariable Integer usuarioId,
            @PathVariable Integer rolId) {
        return ResponseEntity.ok(usu.cambiarRol(usuarioId, rolId));
    }

    @PostMapping("/{usuarioId}/permisos/{permisoId}")
    public ResponseEntity<UserDto> agregarPermisoExtra(
            @PathVariable Integer usuarioId,
            @PathVariable Integer permisoId) {
        return ResponseEntity.ok(usu.agregarPermisoExtra(usuarioId, permisoId));
    }

    @DeleteMapping("/{usuarioId}/permisos/{permisoId}")
    public ResponseEntity<UserDto> quitarPermisoExtra(
            @PathVariable Integer usuarioId,
            @PathVariable Integer permisoId) {
        return ResponseEntity.ok(usu.quitarPermisoExtra(usuarioId, permisoId));
    }
}