package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.promociones.PromocionActivaDto;
import com.ventas.key.mis.productos.models.promociones.PromocionActivoRequestDto;
import com.ventas.key.mis.productos.models.promociones.PromocionRequestDto;
import com.ventas.key.mis.productos.models.promociones.PromocionResponseDto;
import com.ventas.key.mis.productos.service.PromocionServiceImpl;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("v1/promociones")
public class PromocionController {

    private final PromocionServiceImpl promocionService;

    public PromocionController(PromocionServiceImpl promocionService) {
        this.promocionService = promocionService;
    }

    @PostMapping
    public ResponseEntity<ResponseGeneric<PromocionResponseDto>> crear(@RequestBody PromocionRequestDto request) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(promocionService.crear(request)));
        } catch (Exception e) {
            log.error("Error al crear promocion: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseGeneric<PromocionResponseDto>> editar(
            @PathVariable Integer id, @RequestBody PromocionRequestDto request) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(promocionService.editar(id, request)));
        } catch (Exception e) {
            log.error("Error al editar promocion {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PutMapping("/{id}/activo")
    public ResponseEntity<ResponseGeneric<PromocionResponseDto>> cambiarActivo(
            @PathVariable Integer id, @RequestBody PromocionActivoRequestDto request) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(promocionService.cambiarActivo(id, Boolean.TRUE.equals(request.getActivo()))));
        } catch (Exception e) {
            log.error("Error al cambiar estado de promocion {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/admin")
    public ResponseEntity<ResponseGeneric<PginaDto<List<PromocionResponseDto>>>> listarAdmin(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(promocionService.listarAdmin(pagina, size)));
        } catch (Exception e) {
            log.error("Error al listar promociones (admin): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseGeneric<>(null, "Error al listar promociones"));
        }
    }

    @GetMapping("/activas")
    public ResponseEntity<ResponseGeneric<PginaDto<List<PromocionActivaDto>>>> listarActivas(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(promocionService.listarActivas(pagina, size)));
        } catch (Exception e) {
            log.error("Error al listar promociones activas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseGeneric<>(null, "Error al listar promociones"));
        }
    }
}
