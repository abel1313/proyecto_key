package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.ConfigurarRifaVarianteDto;
import com.ventas.key.mis.productos.models.ConfigurarRifaVarianteRequest;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.ConfigurarRifaVarianteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/configurarRifaVariante")
@RequiredArgsConstructor
@Slf4j
public class ConfigurarRifaVarianteController {

    private final ConfigurarRifaVarianteService service;

    @PostMapping("/save")
    public ResponseEntity<ResponseGeneric<ConfigurarRifaVarianteDto>> agregar(
            @RequestBody ConfigurarRifaVarianteRequest req) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(service.agregar(req)));
        } catch (Exception e) {
            log.error("Error al agregar variante a rifa: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/porRifa/{rifaId}")
    public ResponseEntity<ResponseGeneric<List<ConfigurarRifaVarianteDto>>> listar(
            @PathVariable Integer rifaId) {
        return ResponseEntity.ok(new ResponseGeneric<List<ConfigurarRifaVarianteDto>>(service.listarPorRifa(rifaId)));
    }

    @GetMapping("/palabrasClave/{rifaId}")
    public ResponseEntity<ResponseGeneric<List<String>>> palabrasClave(
            @PathVariable Integer rifaId) {
        return ResponseEntity.ok(new ResponseGeneric<List<String>>(service.obtenerPalabrasClave(rifaId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseGeneric<String>> eliminar(@PathVariable Integer id) {
        try {
            service.eliminar(id);
            return ResponseEntity.ok(new ResponseGeneric<>("Variante eliminada y stock restaurado"));
        } catch (Exception e) {
            log.error("Error al eliminar variante de rifa: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PutMapping("/{id}/palabraClave")
    public ResponseEntity<ResponseGeneric<ConfigurarRifaVarianteDto>> actualizarPalabraClave(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(
                    service.actualizarPalabraClave(id, body.get("palabraClave"))));
        } catch (Exception e) {
            log.error("Error al actualizar palabraClave: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }
}