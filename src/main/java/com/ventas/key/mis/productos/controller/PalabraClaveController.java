package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.PalabraClave;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.service.PalabraClaveServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/palabras-clave")
public class PalabraClaveController extends AbstractController<
        PalabraClave,
        Optional<PalabraClave>,
        List<PalabraClave>,
        Integer,
        PginaDto<List<PalabraClave>>,
        PalabraClaveServiceImpl> {

    public PalabraClaveController(PalabraClaveServiceImpl sGenerico) {
        super(sGenerico);
    }

    @GetMapping("/buscar")
    public ResponseEntity<PginaDto<List<PalabraClave>>> buscar(
            @RequestParam String nombre,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(sGenerico.buscarPorNombre(nombre, pagina, size));
    }
}