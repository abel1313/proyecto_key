package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.VarianteResumenDto;
import com.ventas.key.mis.productos.service.FavoritoServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("v1/favoritos")
public class FavoritoController {

    private final FavoritoServiceImpl favoritoService;

    public FavoritoController(FavoritoServiceImpl favoritoService) {
        this.favoritoService = favoritoService;
    }

    @PostMapping("/{varianteId}")
    public ResponseEntity<ResponseGeneric<String>> agregar(@PathVariable Integer varianteId) {
        try {
            favoritoService.agregar(varianteId);
            return ResponseEntity.ok(new ResponseGeneric<>("Agregado a favoritos"));
        } catch (Exception e) {
            log.error("Error al agregar favorito, variante {}: {}", varianteId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @DeleteMapping("/{varianteId}")
    public ResponseEntity<ResponseGeneric<String>> quitar(@PathVariable Integer varianteId) {
        try {
            favoritoService.quitar(varianteId);
            return ResponseEntity.ok(new ResponseGeneric<>("Quitado de favoritos"));
        } catch (Exception e) {
            log.error("Error al quitar favorito, variante {}: {}", varianteId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ResponseGeneric<PginaDto<List<VarianteResumenDto>>>> listar(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(favoritoService.listar(pagina, size)));
        } catch (Exception e) {
            log.error("Error al listar favoritos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/ids")
    public ResponseEntity<ResponseGeneric<List<Integer>>> listarIds() {
        try {
            return ResponseEntity.ok(new ResponseGeneric<List<Integer>>(favoritoService.listarIds()));
        } catch (Exception e) {
            log.error("Error al listar ids de favoritos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }
}
