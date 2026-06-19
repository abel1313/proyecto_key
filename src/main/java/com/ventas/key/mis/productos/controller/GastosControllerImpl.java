package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.dto.GastosReporteDto;
import com.ventas.key.mis.productos.dto.GastosRequest;
import com.ventas.key.mis.productos.entity.Gastos;
import com.ventas.key.mis.productos.mapper.GastosMapper;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.api.IGastosService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/gastos")
@Slf4j
public class GastosControllerImpl {

    private final IGastosService iService;

    public GastosControllerImpl(final IGastosService iService) {
        this.iService = iService;
    }

    @PostMapping("/save")
    public ResponseEntity<ResponseGeneric<Gastos>> save(@RequestBody GastosRequest gastos) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(
                    iService.saveGastosNT(GastosMapper.dtoToEntity(gastos))));
        } catch (Exception e) {
            log.error("Error al guardar gasto: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<ResponseGeneric<PginaDto<List<Gastos>>>> buscar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Gastos.CategoriaGasto categoria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            LocalDate desde = fecha != null ? fecha : fechaInicio;
            LocalDate hasta = fecha != null ? fecha : fechaFin;
            return ResponseEntity.ok(new ResponseGeneric<>(
                    iService.buscar(desde, hasta, categoria, page, size)));
        } catch (Exception e) {
            log.error("Error al buscar gastos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseGeneric<Gastos>> editar(
            @PathVariable Integer id,
            @RequestBody GastosRequest gastos) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(
                    iService.editar(id, GastosMapper.dtoToEntity(gastos))));
        } catch (Exception e) {
            log.error("Error al editar gasto {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseGeneric<String>> eliminar(@PathVariable Integer id) {
        try {
            iService.eliminar(id);
            return ResponseEntity.ok(new ResponseGeneric<>("Gasto eliminado"));
        } catch (Exception e) {
            log.error("Error al eliminar gasto {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/reporte")
    public ResponseEntity<ResponseGeneric<GastosReporteDto>> reporte(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(iService.reporte(fechaInicio, fechaFin)));
        } catch (Exception e) {
            log.error("Error al generar reporte: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }
}
