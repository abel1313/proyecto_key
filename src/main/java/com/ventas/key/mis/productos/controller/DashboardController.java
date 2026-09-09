package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.dashboard.AccesoUsuarioDto;
import com.ventas.key.mis.productos.models.dashboard.DashboardResumenDto;
import com.ventas.key.mis.productos.service.api.IDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final IDashboardService dashboardService;

    @GetMapping("/resumen")
    public ResponseEntity<ResponseGeneric<DashboardResumenDto>> resumen() {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(dashboardService.resumen()));
        } catch (Exception e) {
            log.error("Error generando resumen de dashboard: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseGeneric<>(null));
        }
    }

    // Todo /v1/dashboard/** ya es ADMIN-only en SecurityConfig, no requiere regla nueva.
    // desde/hasta opcionales (yyyy-MM-dd) -- sin ellos trae todo el historico paginado.
    @GetMapping("/accesos")
    public ResponseEntity<ResponseGeneric<PginaDto<List<AccesoUsuarioDto>>>> accesos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(dashboardService.listarAccesos(desde, hasta, pagina, size)));
        } catch (Exception e) {
            log.error("Error listando historial de accesos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseGeneric<>(null));
        }
    }
}
