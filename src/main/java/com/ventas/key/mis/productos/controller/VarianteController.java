package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.models.ImagenUpdateDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.VarianteDetalle;
import com.ventas.key.mis.productos.models.VarianteResumenDto;
import com.ventas.key.mis.productos.models.variantes.VarianteDto;
import com.ventas.key.mis.productos.service.VarianteServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    public ResponseEntity<ResponseGeneric<List<VarianteDto>>> getPorProducto(@PathVariable Integer productoId) {
        return ResponseEntity.ok(new ResponseGeneric<List<VarianteDto>>(sGenerico.buscarPorProducto(productoId)));
    }

    @GetMapping("/porProducto/{productoId}/paginado")
    public ResponseEntity<ResponseGeneric<PginaDto<List<Variantes>>>> getPorProductoPaginado(
            @PathVariable Integer productoId,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.buscarPorProductoPaginado(productoId, pagina, size)));
    }

    @GetMapping("/buscar")
    public ResponseEntity<ResponseGeneric<PginaDto<List<VarianteResumenDto>>>> buscar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String codigoBarras,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.buscarVariantes(nombre, pagina, size)));
    }

    @PostMapping("/guardarConImagenes")
    public ResponseEntity<ResponseGeneric<List<Variantes>>> guardarConImagenes(@RequestBody List<VarianteDetalle> detalles) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<List<Variantes>>(sGenerico.guardarConImagenes(detalles)));
        } catch (Exception e) {
            log.error("Error al guardar variantes con imágenes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/imagenes/{varianteId}")
    public ResponseEntity<ResponseGeneric<List<ImagenUpdateDto>>> getImagenes(@PathVariable Integer varianteId) {
        return ResponseEntity.ok(new ResponseGeneric<List<ImagenUpdateDto>>(sGenerico.getImagenesPorVariante(varianteId)));
    }

    @GetMapping("/imagenes/{varianteId}/paginado")
    public ResponseEntity<ResponseGeneric<PginaDto<List<ImagenUpdateDto>>>> getImagenesPaginado(
            @PathVariable Integer varianteId,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.getImagenesPorVariantePaginado(varianteId, pagina, size)));
    }

    @GetMapping("/porProducto/{productoId}/paginado/resumen")
    public ResponseEntity<ResponseGeneric<PginaDto<List<VarianteResumenDto>>>> getPorProductoPaginadoResumen(
            @PathVariable Integer productoId,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.buscarPorProductoPaginadoResumen(productoId, pagina, size)));
    }

    @DeleteMapping("/imagenes")
    public ResponseEntity<ResponseGeneric<String>> eliminarImagenesDeVariantes(@RequestBody List<Integer> varianteIds) {
        sGenerico.eliminarImagenesDeVariantes(varianteIds);
        return ResponseEntity.ok(new ResponseGeneric<>("Imágenes eliminadas correctamente"));
    }

    @DeleteMapping("/{varianteId}/imagenes")
    public ResponseEntity<ResponseGeneric<String>> eliminarImagenesEspecificas(
            @PathVariable Integer varianteId,
            @RequestBody List<Long> imagenIds) {
        sGenerico.eliminarImagenesEspecificas(varianteId, imagenIds);
        return ResponseEntity.ok(new ResponseGeneric<>("Imágenes eliminadas correctamente"));
    }
}
