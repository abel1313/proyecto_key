package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.ColorFlor;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.ColorFlorServiceImpl;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/colores-flor")
public class ColorFlorController extends AbstractController<
        ColorFlor,
        Optional<ColorFlor>,
        List<ColorFlor>,
        Integer,
        PginaDto<List<ColorFlor>>,
        ColorFlorServiceImpl> {

    public ColorFlorController(ColorFlorServiceImpl sGenerico) {
        super(sGenerico);
    }

    // Usado por el configurador: una vez elegida la cantidad (a nivel especie), se listan los
    // colores disponibles de ESE tipo de flor para que el cliente elija uno o varios.
    @GetMapping("/por-tipo-flor/{tipoFlorId}")
    public ResponseEntity<ResponseGeneric<ColorFlor>> porTipoFlor(@PathVariable Integer tipoFlorId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(this.sGenerico.listarPorTipoFlor(tipoFlorId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseGeneric<>((List<ColorFlor>) null, "Error al listar colores"));
        }
    }
}
