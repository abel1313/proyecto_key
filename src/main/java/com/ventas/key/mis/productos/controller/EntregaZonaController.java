package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.pedidos.EntregaZonaSemanaResponse;
import com.ventas.key.mis.productos.models.pedidos.ProgramarEntregaZonaRequest;
import com.ventas.key.mis.productos.service.EntregaZonaServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/entregas-zona")
@Slf4j
public class EntregaZonaController {

    private final EntregaZonaServiceImpl service;

    public EntregaZonaController(EntregaZonaServiceImpl service) {
        this.service = service;
    }

    @GetMapping("/{lugarEntregaId}/pendientes")
    public ResponseEntity<ResponseGeneric<EntregaZonaSemanaResponse>> pendientes(@PathVariable Integer lugarEntregaId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(service.listarPendientesSemana(lugarEntregaId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>((EntregaZonaSemanaResponse) null, e.getMessage()));
        }
    }

    @PostMapping("/{lugarEntregaId}/programar")
    public ResponseEntity<ResponseGeneric<Integer>> programar(@PathVariable Integer lugarEntregaId,
                                                                @RequestBody ProgramarEntregaZonaRequest request) {
        try {
            int enviados = service.programarEntrega(lugarEntregaId, request);
            return ResponseEntity.ok(new ResponseGeneric<>(enviados, "Se avisó a " + enviados + " cliente(s)"));
        } catch (Exception e) {
            log.warn("Error al programar entrega de zona {}: {}", lugarEntregaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>((Integer) null, e.getMessage()));
        }
    }
}
