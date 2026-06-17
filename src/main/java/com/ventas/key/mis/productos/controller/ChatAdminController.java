package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.ChatMensaje;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.chat.SesionActivaDto;
import com.ventas.key.mis.productos.service.api.IChatMensajeService;
import com.ventas.key.mis.productos.service.api.IChatSesionService;
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
        List<SesionActivaDto> sesiones = sesionService.obtenerSesionesActivas().stream()
                .map(s -> {
                    String ultimo = mensajeService.ultimoMensaje(s.getSesionId())
                            .map(ChatMensaje::getContenido)
                            .orElse(null);
                    return SesionActivaDto.builder()
                            .sesionId(s.getSesionId())
                            .nombreUsuario(s.getNombreUsuario())
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
    public ResponseEntity<ResponseGeneric<List<ChatMensaje>>> historial(@PathVariable String sesionId) {
        return ResponseEntity.ok(new ResponseGeneric<List<ChatMensaje>>(mensajeService.obtenerHistorial(sesionId)));
    }

    @PostMapping("/admin/cerrar/{sesionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cerrarSesion(@PathVariable String sesionId) {
        sesionService.cerrarSesion(sesionId);
        return ResponseEntity.noContent().build();
    }
}
