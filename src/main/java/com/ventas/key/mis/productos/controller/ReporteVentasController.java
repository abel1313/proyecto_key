package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.reportes.ProductoMasVendidoDto;
import com.ventas.key.mis.productos.models.reportes.ReporteClienteDto;
import com.ventas.key.mis.productos.models.reportes.ReporteDiarioDto;
import com.ventas.key.mis.productos.models.reportes.ReporteMensualDto;
import com.ventas.key.mis.productos.service.api.IReporteVentasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/reportes/ventas")
@RequiredArgsConstructor
@Slf4j
public class ReporteVentasController {

    private final IReporteVentasService reporteVentasService;

    @GetMapping("/diario")
    public ResponseEntity<ResponseGeneric<ReporteDiarioDto>> diario(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(reporteVentasService.reporteDiario(fecha)));
        } catch (Exception e) {
            log.error("Error en reporte diario: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseGeneric<>(null));
        }
    }

    @GetMapping("/mensual")
    public ResponseEntity<ResponseGeneric<ReporteMensualDto>> mensual(@RequestParam String mes) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(reporteVentasService.reporteMensual(mes)));
        } catch (Exception e) {
            ResponseGeneric<ReporteMensualDto> error = new ResponseGeneric<>((ReporteMensualDto) null);
            error.setMensaje("Formato de mes invalido, usar yyyy-MM");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<ResponseGeneric<ReporteClienteDto>> porCliente(@PathVariable int clienteId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(reporteVentasService.reporteCliente(clienteId)));
        } catch (Exception e) {
            ResponseGeneric<ReporteClienteDto> error = new ResponseGeneric<>((ReporteClienteDto) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/productos-mas-vendidos")
    public ResponseEntity<ResponseGeneric<List<ProductoMasVendidoDto>>> productosMasVendidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int limite) {
        try {
            List<ProductoMasVendidoDto> response =
                    reporteVentasService.productosMasVendidos(desde, hasta, limite);
            return ResponseEntity.ok(new ResponseGeneric<List<ProductoMasVendidoDto>>(response));
        } catch (Exception e) {
            log.error("Error en reporte productos mas vendidos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseGeneric<List<ProductoMasVendidoDto>>((List<ProductoMasVendidoDto>) null));
        }
    }
}
