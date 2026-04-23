package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.models.ImagenUpdateDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.VarianteDetalle;
import com.ventas.key.mis.productos.service.VarianteServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("variantes")
public class VarianteController extends AbstractController<
                                        Variantes,
                                        Optional<Variantes>,
                                        List<Variantes>,
                                        Integer,
                                        PginaDto<List<Variantes>>,
                                        VarianteServiceImpl
                                        >{
    protected VarianteController(VarianteServiceImpl sGenerico) {
        super(sGenerico);
    }

    @GetMapping("/porProducto/{productoId}")
    public ResponseEntity<ResponseGeneric<List<Variantes>>> getPorProducto(@PathVariable Integer productoId) {
        return ResponseEntity.ok(new ResponseGeneric<List<Variantes>>(sGenerico.buscarPorProducto(productoId)));
    }

    @GetMapping("/porProducto/{productoId}/paginado")
    public ResponseEntity<ResponseGeneric<PginaDto<List<Variantes>>>> getPorProductoPaginado(
            @PathVariable Integer productoId,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.buscarPorProductoPaginado(productoId, pagina, size)));
    }

    @GetMapping("/buscar")
    public ResponseEntity<?> buscar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String codigoBarras,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        if (codigoBarras != null && !codigoBarras.isBlank()) {
            return ResponseEntity.ok(new ResponseGeneric<List<Variantes>>(sGenerico.buscarPorCodigoBarras(codigoBarras)));
        }
        if (nombre != null && !nombre.isBlank()) {
            return ResponseEntity.ok(new ResponseGeneric<List<Variantes>>(sGenerico.buscarPorNombre(nombre)));
        }
        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.findAllNew(pagina, size)));
    }

    @PostMapping("/guardarConImagenes")
    public ResponseEntity<ResponseGeneric<Variantes>> guardarConImagenes(@RequestBody VarianteDetalle detalle) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.guardarConImagenes(detalle)));
        } catch (Exception e) {
            log.error("Error al guardar variante con imágenes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/imagenes/{varianteId}")
    public ResponseEntity<ResponseGeneric<List<ImagenUpdateDto>>> getImagenes(@PathVariable Integer varianteId) {
        return ResponseEntity.ok(new ResponseGeneric<List<ImagenUpdateDto>>(sGenerico.getImagenesPorVariante(varianteId)));
    }
}
