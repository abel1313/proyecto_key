package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.dashboard.DashboardResumenDto;
import com.ventas.key.mis.productos.service.api.IDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
