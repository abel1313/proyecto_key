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
        favoritoService.agregar(varianteId);
        return ResponseEntity.ok(new ResponseGeneric<>("Agregado a favoritos"));
    }

    @DeleteMapping("/{varianteId}")
    public ResponseEntity<ResponseGeneric<String>> quitar(@PathVariable Integer varianteId) {
        favoritoService.quitar(varianteId);
        return ResponseEntity.ok(new ResponseGeneric<>("Quitado de favoritos"));
    }

    @GetMapping
    public ResponseEntity<ResponseGeneric<PginaDto<List<VarianteResumenDto>>>> listar(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new ResponseGeneric<>(favoritoService.listar(pagina, size)));
    }

    @GetMapping("/ids")
    public ResponseEntity<ResponseGeneric<List<Integer>>> listarIds() {
        return ResponseEntity.ok(new ResponseGeneric<List<Integer>>(favoritoService.listarIds()));
    }
}
