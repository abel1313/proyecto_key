package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.dto.negocio.ContactosUpdateDto;
import com.ventas.key.mis.productos.dto.negocio.HorarioUpdateDto;
import com.ventas.key.mis.productos.dto.negocio.NegocioConfigDto;
import com.ventas.key.mis.productos.dto.negocio.NegocioEstadoDto;
import com.ventas.key.mis.productos.entity.ConfiguracionNegocio;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.NegocioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/negocio")
@RequiredArgsConstructor
@Slf4j
public class NegocioController {

    private final NegocioService negocioService;

    /** Público — el frontend lo llama al cargar la página */
    @GetMapping("/estado")
    public ResponseEntity<ResponseGeneric<NegocioEstadoDto>> getEstado() {
        return ResponseEntity.ok(new ResponseGeneric<>(negocioService.getEstado()));
    }

    /** Solo ADMIN — ver configuración completa */
    @GetMapping("/config")
    public ResponseEntity<ResponseGeneric<NegocioConfigDto>> getConfig() {
        return ResponseEntity.ok(new ResponseGeneric<>(negocioService.getConfig()));
    }

    /** Solo ADMIN — abrir el negocio */
    @PostMapping("/abrir")
    public ResponseEntity<ResponseGeneric<ConfiguracionNegocio>> abrir() {
        log.info("Admin abrió el negocio");
        return ResponseEntity.ok(new ResponseGeneric<>(negocioService.abrir()));
    }

    /** Solo ADMIN — cerrar el negocio */
    @PostMapping("/cerrar")
    public ResponseEntity<ResponseGeneric<ConfiguracionNegocio>> cerrar() {
        log.info("Admin cerró el negocio");
        return ResponseEntity.ok(new ResponseGeneric<>(negocioService.cerrar()));
    }

    /** Solo ADMIN — guardar horario de apertura y cierre */
    @PutMapping("/horario")
    public ResponseEntity<ResponseGeneric<NegocioConfigDto>> actualizarHorario(
            @RequestBody HorarioUpdateDto dto) {
        return ResponseEntity.ok(new ResponseGeneric<>(negocioService.actualizarHorario(dto)));
    }

    /** Solo ADMIN — actualizar WhatsApp y/o Facebook */
    @PutMapping("/contactos")
    public ResponseEntity<ResponseGeneric<ConfiguracionNegocio>> actualizarContactos(
            @RequestBody ContactosUpdateDto dto) {
        return ResponseEntity.ok(new ResponseGeneric<>(negocioService.actualizarContactos(dto)));
    }
}