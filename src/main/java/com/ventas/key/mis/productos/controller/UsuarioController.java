package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Permiso;
import com.ventas.key.mis.productos.entity.Roles;
import com.ventas.key.mis.productos.entity.Submenu;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.entity.UsuarioSubmenu;
import com.ventas.key.mis.productos.mapper.UserDto;
import com.ventas.key.mis.productos.mapper.UserUpdate;
import com.ventas.key.mis.productos.models.CambioCorreoPendienteResponseDto;
import com.ventas.key.mis.productos.models.ConfirmarCambioCorreoRequest;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.SolicitarCambioCorreoRequest;
import com.ventas.key.mis.productos.service.UsuarioServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/usuarios")
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
            @RequestParam int size,
            @RequestParam(defaultValue = "true") boolean activos) {
        PginaDto<List<UserDto>> result = usu.findAllPage(page, size, buscar, activos);
        return ResponseEntity.ok(new ResponseGeneric<>(result));
    }

    // Contraparte de eliminarUsuarioDto -- reactiva a alguien desactivado por error o que volvió
    // a hacer falta, sin tener que tocar la base a mano.
    @PutMapping("/{id}/activar")
    public ResponseEntity<ResponseGeneric<UserDto>> activarUsuario(@PathVariable int id) {
        return ResponseEntity.ok(new ResponseGeneric<>(usu.activarUsuario(id)));
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

    @PutMapping("/{id}/resetear-password")
    public ResponseEntity<ResponseGeneric<String>> resetearPassword(@PathVariable Integer id) {
        String nuevaPassword = usu.resetearPasswordAleatoria(id);
        return ResponseEntity.ok(new ResponseGeneric<>(nuevaPassword,
                "Contrasena reseteada. Comparte esta contrasena con el usuario; debera cambiarla en su siguiente login."));
    }

    // ── Cambio de correo de OTRO usuario (admin) — verificar antes de guardar ──
    // El email real no cambia hasta confirmar-cambio-correo con el codigo correcto.

    @PostMapping("/{id}/solicitar-cambio-correo")
    public ResponseEntity<ResponseGeneric<String>> solicitarCambioCorreo(@PathVariable Integer id,
                                                    @Valid @RequestBody SolicitarCambioCorreoRequest request) {
        try {
            boolean enviado = usu.solicitarCambioCorreo(id, request.getCorreoNuevo());
            String mensaje = enviado
                    ? "Codigo enviado al correo nuevo"
                    : "Ya tienes un codigo vigente enviado a ese correo, revisa tu bandeja";
            return ResponseEntity.ok(new ResponseGeneric<>(mensaje));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PostMapping("/{id}/confirmar-cambio-correo")
    public ResponseEntity<ResponseGeneric<String>> confirmarCambioCorreo(@PathVariable Integer id,
                                                    @Valid @RequestBody ConfirmarCambioCorreoRequest request) {
        try {
            usu.confirmarCambioCorreo(id, request.getCodigo());
            return ResponseEntity.ok(new ResponseGeneric<>("Correo actualizado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/{id}/cambio-correo-pendiente")
    public ResponseEntity<ResponseGeneric<CambioCorreoPendienteResponseDto>> obtenerCambioCorreoPendiente(
            @PathVariable Integer id) {
        return ResponseEntity.ok(new ResponseGeneric<>(usu.obtenerCambioCorreoPendiente(id)));
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

    // ── Excepciones de pantalla por usuario (encima de lo que ya da su rol) ─────

    @GetMapping("/{usuarioId}/submenus/efectivos")
    public ResponseEntity<ResponseGeneric<List<Submenu>>> submenusEfectivos(@PathVariable Integer usuarioId) {
        return ResponseEntity.ok(new ResponseGeneric<List<Submenu>>(List.copyOf(usu.submenusEfectivos(usuarioId))));
    }

    @GetMapping("/{usuarioId}/submenus/excepciones")
    public ResponseEntity<ResponseGeneric<List<UsuarioSubmenu>>> excepcionesSubmenu(@PathVariable Integer usuarioId) {
        return ResponseEntity.ok(new ResponseGeneric<List<UsuarioSubmenu>>(usu.listarExcepcionesSubmenu(usuarioId)));
    }

    @PostMapping("/{usuarioId}/submenus/{submenuId}")
    public ResponseEntity<ResponseGeneric<UsuarioSubmenu>> agregarSubmenuUsuario(
            @PathVariable Integer usuarioId,
            @PathVariable Integer submenuId,
            @RequestParam(defaultValue = "true") boolean concedido) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(usu.agregarSubmenuUsuario(usuarioId, submenuId, concedido)));
        } catch (Exception e) {
            ResponseGeneric<UsuarioSubmenu> error = new ResponseGeneric<>((UsuarioSubmenu) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/{usuarioId}/submenus/{submenuId}")
    public ResponseEntity<Void> quitarSubmenuUsuario(
            @PathVariable Integer usuarioId,
            @PathVariable Integer submenuId) {
        usu.quitarSubmenuUsuario(usuarioId, submenuId);
        return ResponseEntity.noContent().build();
    }
}