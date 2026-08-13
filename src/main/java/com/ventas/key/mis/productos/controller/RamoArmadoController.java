package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.floreseternas.RamoArmadoActivoRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoArmadoRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoArmadoResponseDto;
import com.ventas.key.mis.productos.service.RamoArmadoServiceImpl;
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
@RequestMapping("v1/ramos-armados")
public class RamoArmadoController {

    private final RamoArmadoServiceImpl ramoArmadoService;

    public RamoArmadoController(RamoArmadoServiceImpl ramoArmadoService) {
        this.ramoArmadoService = ramoArmadoService;
    }

    @PostMapping
    public ResponseEntity<ResponseGeneric<RamoArmadoResponseDto>> crear(@RequestBody RamoArmadoRequestDto request) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ramoArmadoService.crear(request)));
        } catch (Exception e) {
            log.error("Error al crear ramo armado: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseGeneric<RamoArmadoResponseDto>> editar(
            @PathVariable Integer id, @RequestBody RamoArmadoRequestDto request) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ramoArmadoService.editar(id, request)));
        } catch (Exception e) {
            log.error("Error al editar ramo armado {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PutMapping("/{id}/activo")
    public ResponseEntity<ResponseGeneric<RamoArmadoResponseDto>> cambiarActivo(
            @PathVariable Integer id, @RequestBody RamoArmadoActivoRequestDto request) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ramoArmadoService.cambiarActivo(id, Boolean.TRUE.equals(request.getActivo()))));
        } catch (Exception e) {
            log.error("Error al cambiar estado del ramo armado {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/admin")
    public ResponseEntity<ResponseGeneric<PginaDto<List<RamoArmadoResponseDto>>>> listarAdmin(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ramoArmadoService.listarAdmin(pagina, size)));
        } catch (Exception e) {
            log.error("Error al listar ramos armados (admin): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseGeneric<>(null, "Error al listar ramos armados"));
        }
    }

    @GetMapping("/activos")
    public ResponseEntity<ResponseGeneric<PginaDto<List<RamoArmadoResponseDto>>>> listarActivos(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ramoArmadoService.listarActivos(pagina, size)));
        } catch (Exception e) {
            log.error("Error al listar ramos armados activos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseGeneric<>(null, "Error al listar ramos armados"));
        }
    }
}
