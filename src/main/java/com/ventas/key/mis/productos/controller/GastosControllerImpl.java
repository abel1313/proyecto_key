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
        return ResponseEntity.ok(new ResponseGeneric<>(
                iService.saveGastosNT(GastosMapper.dtoToEntity(gastos))));
    }

    @GetMapping("/buscar")
    public ResponseEntity<ResponseGeneric<PginaDto<List<Gastos>>>> buscar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Gastos.CategoriaGasto categoria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        LocalDate desde = fecha != null ? fecha : fechaInicio;
        LocalDate hasta = fecha != null ? fecha : fechaFin;
        return ResponseEntity.ok(new ResponseGeneric<>(
                iService.buscar(desde, hasta, categoria, page, size)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseGeneric<Gastos>> editar(
            @PathVariable Integer id,
            @RequestBody GastosRequest gastos) throws Exception {
        return ResponseEntity.ok(new ResponseGeneric<>(
                iService.editar(id, GastosMapper.dtoToEntity(gastos))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseGeneric<String>> eliminar(@PathVariable Integer id) throws Exception {
        iService.eliminar(id);
        return ResponseEntity.ok(new ResponseGeneric<>("Gasto eliminado"));
    }

    @GetMapping("/reporte")
    public ResponseEntity<ResponseGeneric<GastosReporteDto>> reporte(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(new ResponseGeneric<>(iService.reporte(fechaInicio, fechaFin)));
    }
}
