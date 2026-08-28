package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.entity.ChatMensaje;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.chat.ChatHistorialPaginadoDto;
import com.ventas.key.mis.productos.models.chat.SesionActivaDto;
import com.ventas.key.mis.productos.service.api.IChatMensajeService;
import com.ventas.key.mis.productos.service.api.IChatSesionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/v1/chat")
public class ChatAdminController {

    private final IChatSesionService sesionService;
    private final IChatMensajeService mensajeService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public ChatAdminController(IChatSesionService sesionService, IChatMensajeService mensajeService) {
        this.sesionService = sesionService;
        this.mensajeService = mensajeService;
    }

    @GetMapping("/admin/sesiones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseGeneric<List<SesionActivaDto>>> sesionesActivas() {
        List<SesionActivaDto> sesiones = sesionService.obtenerSesionesRecientes().stream()
                .map(s -> {
                    String ultimo = mensajeService.ultimoMensaje(s.getSesionId())
                            .map(ChatMensaje::getContenido)
                            .orElse(null);
                    return SesionActivaDto.builder()
                            .sesionId(s.getSesionId())
                            .nombreUsuario(s.getNombreUsuario())
                            .estado(s.getEstado())
                            .fechaInicio(s.getFechaInicio().format(FMT))
                            .ultimaActividad(s.getUltimaActividad().format(FMT))
                            .ultimoMensaje(ultimo)
                            .build();
                })
                .toList();
        return ResponseEntity.ok(new ResponseGeneric<List<SesionActivaDto>>(sesiones));
    }

    @GetMapping("/admin/historial/{sesionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseGeneric<ChatHistorialPaginadoDto>> historial(
            @PathVariable String sesionId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ResponseGeneric<>(mensajeService.obtenerHistorialPaginado(sesionId, pagina, size)));
    }

    @GetMapping("/historial/usuario/{usuarioId}")
    public ResponseEntity<ResponseGeneric<ChatHistorialPaginadoDto>> historialPorUsuarioId(
            @PathVariable Integer usuarioId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int size) {
        // Sin este chequeo cualquier usuario logueado podia leer el chat privado de OTRO usuario
        // solo cambiando el id en la URL -- ver nota en SecurityConfig, esta ruta ya no es publica
        // pero seguia sin validar dueno (encontrado 2026-08-27).
        if (!AuthenticationUtils.isAdminContext()) {
            Usuario actual = AuthenticationUtils.currentUsuario();
            if (!actual.getId().equals(usuarioId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseGeneric<>(null, "No autorizado"));
            }
        }
        return ResponseEntity.ok(new ResponseGeneric<>(mensajeService.obtenerHistorialPorUsuarioId(usuarioId, pagina, size)));
    }

    @GetMapping("/historial/cliente/{clienteId}")
    public ResponseEntity<ResponseGeneric<ChatHistorialPaginadoDto>> historialPorClienteId(
            @PathVariable String clienteId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int size) {
        // Mismo chequeo que historialPorUsuarioId() -- ver esa nota.
        if (!AuthenticationUtils.isAdminContext()) {
            Usuario actual = AuthenticationUtils.currentUsuario();
            boolean esDueno = actual.getCliente() != null
                    && String.valueOf(actual.getCliente().getId()).equals(clienteId);
            if (!esDueno) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseGeneric<>(null, "No autorizado"));
            }
        }
        return ResponseEntity.ok(new ResponseGeneric<>(mensajeService.obtenerHistorialPorClienteId(clienteId, pagina, size)));
    }

    @GetMapping("/historial/{sesionId}")
    public ResponseEntity<ResponseGeneric<ChatHistorialPaginadoDto>> historialUsuario(
            @PathVariable String sesionId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int size) {
        if (!sesionService.existeSesion(sesionId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(new ResponseGeneric<>(mensajeService.obtenerHistorialPaginado(sesionId, pagina, size)));
    }

    @PostMapping("/admin/cerrar/{sesionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cerrarSesion(@PathVariable String sesionId) {
        sesionService.cerrarSesion(sesionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/version")
    public ResponseEntity<String> version() {
        return ResponseEntity.ok("chat-v3-usuarioId-2026-06-18");
    }
}
