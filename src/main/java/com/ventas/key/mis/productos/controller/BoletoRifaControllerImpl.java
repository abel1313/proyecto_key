package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.BoletoRifa;
import com.ventas.key.mis.productos.models.BoletoRifaRequest;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.BoletoRifaServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/boletoRifa")
@RequiredArgsConstructor
@Slf4j
public class BoletoRifaControllerImpl {

    private final BoletoRifaServiceImpl service;

    @PostMapping("/registrar")
    public ResponseEntity<ResponseGeneric<BoletoRifa>> registrar(@RequestBody BoletoRifaRequest req) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(service.registrar(req)));
        } catch (Exception e) {
            log.error("Error al registrar boleto: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/porConcursante/{concursanteId}")
    public ResponseEntity<ResponseGeneric<List<BoletoRifa>>> porConcursante(@PathVariable Integer concursanteId) {
        return ResponseEntity.ok(new ResponseGeneric<List<BoletoRifa>>(service.listarPorConcursante(concursanteId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseGeneric<String>> eliminar(@PathVariable Integer id) {
        try {
            service.eliminar(id);
            return ResponseEntity.ok(new ResponseGeneric<>("Boleto eliminado"));
        } catch (Exception e) {
            log.error("Error al eliminar boleto {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }
}
