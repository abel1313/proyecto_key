package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.resenas.ResenaEditarDto;
import com.ventas.key.mis.productos.models.resenas.ResenaRequestDto;
import com.ventas.key.mis.productos.models.resenas.ResenaResponseDto;
import com.ventas.key.mis.productos.models.resenas.ResenaResumenDto;
import com.ventas.key.mis.productos.service.ResenaServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("v1/resenas")
public class ResenaController {

    private final ResenaServiceImpl resenaService;

    public ResenaController(ResenaServiceImpl resenaService) {
        this.resenaService = resenaService;
    }

    @PostMapping
    public ResponseEntity<ResponseGeneric<ResenaResponseDto>> crear(@RequestBody ResenaRequestDto request) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(resenaService.crear(request)));
        } catch (Exception e) {
            log.error("Error al crear resena: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseGeneric<ResenaResponseDto>> editar(
            @PathVariable Integer id, @RequestBody ResenaEditarDto request) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(resenaService.editar(id, request)));
        } catch (Exception e) {
            log.error("Error al editar resena {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseGeneric<String>> eliminar(@PathVariable Integer id) {
        try {
            resenaService.eliminar(id);
            return ResponseEntity.ok(new ResponseGeneric<>("Resena eliminada"));
        } catch (Exception e) {
            log.error("Error al eliminar resena {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/variante/{varianteId}")
    public ResponseEntity<ResponseGeneric<PginaDto<List<ResenaResponseDto>>>> listarPorVariante(
            @PathVariable Integer varianteId,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(resenaService.listarPorVariante(varianteId, pagina, size)));
        } catch (Exception e) {
            log.error("Error al listar resenas de variante {}: {}", varianteId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseGeneric<>(null, "Error al listar resenas"));
        }
    }

    @GetMapping("/variante/{varianteId}/resumen")
    public ResponseEntity<ResponseGeneric<ResenaResumenDto>> resumenPorVariante(@PathVariable Integer varianteId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(resenaService.resumenPorVariante(varianteId)));
        } catch (Exception e) {
            log.error("Error al obtener resumen de resenas de variante {}: {}", varianteId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseGeneric<>(null, "Error al obtener resumen"));
        }
    }

    @GetMapping("/mis-resenas")
    public ResponseEntity<ResponseGeneric<PginaDto<List<ResenaResponseDto>>>> misResenas(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(resenaService.misResenas(pagina, size)));
        } catch (Exception e) {
            log.error("Error al listar mis resenas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }
}
