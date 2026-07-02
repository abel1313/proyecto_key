package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.dto.variantes.RequestVarianteDto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.models.DiagnosticoImagenVarianteDto;
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
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    // Antes de esto, GET /variantes/getAll (heredado de AbstractController) no llevaba /v1/
    // porque esta clase no tiene el prefijo en su @RequestMapping. GET /variantes/getAll
    // sigue vivo por compatibilidad, pero el front debe migrar a este.
    @GetMapping("/v1/getAll")
    public ResponseEntity<ResponseGeneric<List<Variantes>>> findAllV2(
            @RequestParam int page, @RequestParam int size) {
        return super.findAll(page, size);
    }

    @GetMapping("/v1/getOne/{tipoDato}")
    public ResponseEntity<ResponseGeneric<Optional<Variantes>>> findByV2(@PathVariable Integer tipoDato) {
        return super.findBy(tipoDato);
    }

    @PostMapping("/v1/save")
    public ResponseEntity<ResponseGeneric<Variantes>> saveV2(
            @Validated @RequestBody Variantes requestG, BindingResult result) {
        return super.save(requestG, result);
    }

    @PutMapping("/v1/update/{tipoDato}")
    public ResponseEntity<ResponseGeneric<Variantes>> updateV2(
            @PathVariable Integer tipoDato,
            @Validated @RequestBody Variantes requestG,
            BindingResult result) throws Exception {
        return super.update(tipoDato, requestG, result);
    }

    @DeleteMapping("/v1/delete")
    public ResponseEntity<ResponseGeneric<Variantes>> deleteV2(@RequestBody Integer requestG) {
        return super.delete(requestG);
    }

    @GetMapping("/v1/porProducto/{productoId}")
    public ResponseEntity<ResponseGeneric<List<VarianteDto>>> getPorProducto(@PathVariable Integer productoId) {
        return ResponseEntity.ok(new ResponseGeneric<List<VarianteDto>>(sGenerico.buscarPorProducto(productoId)));
    }

    @GetMapping("/v1/porProducto/{productoId}/paginado")
    public ResponseEntity<ResponseGeneric<PginaDto<List<Variantes>>>> getPorProductoPaginado(
            @PathVariable Integer productoId,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.buscarPorProductoPaginado(productoId, pagina, size)));
    }

    @GetMapping("/v1/buscar")
    public ResponseEntity<ResponseGeneric<PginaDto<List<VarianteResumenDto>>>> buscar(
            @RequestParam(required = false) String termino,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.buscarVariantes(termino, pagina, size)));
    }

    @PostMapping("/v1/guardarConImagenes")
    public ResponseEntity<ResponseGeneric<List<Variantes>>> guardarConImagenes(@RequestBody List<VarianteDetalle> detalles) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<List<Variantes>>(sGenerico.guardarConImagenes(detalles)));
        } catch (Exception e) {
            log.error("Error al guardar variantes con imágenes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    /**
     * @deprecated Usar GET /variantes/v1/imagenes/{varianteId} — no verifica existencia en micro
     */
    @Deprecated
    @GetMapping("/v3/imagenes/{varianteId}")
    public ResponseEntity<ResponseGeneric<List<ImagenUpdateDto>>> getImagenes(@PathVariable Integer varianteId) {
        return ResponseEntity.ok(new ResponseGeneric<List<ImagenUpdateDto>>(sGenerico.getImagenesPorVariante(varianteId)));
    }

    @GetMapping("/v1/imagenes/{varianteId}")
    public ResponseEntity<ResponseGeneric<List<ImagenUpdateDto>>> getImagenesV2(@PathVariable Integer varianteId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<List<ImagenUpdateDto>>(sGenerico.getImagenesPorVarianteV2(varianteId)));
        } catch (Exception e) {
            log.error("Error obteniendo imagenes de varianteId={}: {}", varianteId, e.getMessage(), e);
            return ResponseEntity.ok(new ResponseGeneric<List<ImagenUpdateDto>>(List.of()));
        }
    }

    @GetMapping("/v1/imagenes/{varianteId}/paginado")
    public ResponseEntity<ResponseGeneric<PginaDto<List<ImagenUpdateDto>>>> getImagenesPaginado(
            @PathVariable Integer varianteId,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.getImagenesPorVariantePaginado(varianteId, pagina, size)));
    }

    @GetMapping("/v1/porProducto/{productoId}/paginado/resumen")
    public ResponseEntity<ResponseGeneric<PginaDto<List<VarianteResumenDto>>>> getPorProductoPaginadoResumen(
            @PathVariable Integer productoId,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.buscarPorProductoPaginadoResumen(productoId, pagina, size)));
    }

    /** @deprecated Usar DELETE /variantes/v1/imagenes */
    @Deprecated
    @DeleteMapping("/v3/imagenes")
    public ResponseEntity<ResponseGeneric<String>> eliminarImagenesDeVariantes(@RequestBody List<Integer> varianteIds) {
        sGenerico.eliminarImagenesDeVariantes(varianteIds);
        return ResponseEntity.ok(new ResponseGeneric<>("Imágenes eliminadas correctamente"));
    }

    @DeleteMapping("/v1/imagenes")
    public ResponseEntity<ResponseGeneric<String>> eliminarImagenesDeVariantesV2(@RequestBody List<Integer> varianteIds) {
        sGenerico.eliminarImagenesDeVariantes(varianteIds);
        return ResponseEntity.ok(new ResponseGeneric<>("Imágenes eliminadas correctamente"));
    }

    /** @deprecated Usar DELETE /variantes/v1/{varianteId}/imagenes */
    @Deprecated
    @DeleteMapping("/v3/{varianteId}/imagenes")
    public ResponseEntity<ResponseGeneric<String>> eliminarImagenesEspecificas(
            @PathVariable Integer varianteId,
            @RequestBody List<Long> imagenIds) {
        sGenerico.eliminarImagenesEspecificas(varianteId, imagenIds);
        return ResponseEntity.ok(new ResponseGeneric<>("Imágenes eliminadas correctamente"));
    }

    @DeleteMapping("/v1/{varianteId}/imagenes")
    public ResponseEntity<ResponseGeneric<String>> eliminarImagenesEspecificasV2(
            @PathVariable Integer varianteId,
            @RequestBody List<Long> imagenIds) {
        sGenerico.eliminarImagenesEspecificas(varianteId, imagenIds);
        return ResponseEntity.ok(new ResponseGeneric<>("Imágenes eliminadas correctamente"));
    }

    @GetMapping("/v1/admin/sin-stock")
    public ResponseEntity<ResponseGeneric<PginaDto<List<VarianteResumenDto>>>> getVariantesSinStockDeshabilitadas(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.getVariantesSinStockDeshabilitadas(pagina, size)));
    }

    @GetMapping("/v1/admin/diagnostico-imagenes/{varianteId}")
    public ResponseEntity<ResponseGeneric<DiagnosticoImagenVarianteDto>> diagnosticarImagenesVariante(
            @PathVariable Integer varianteId) {
        log.info("Diagnóstico de imágenes para variante id={}", varianteId);
        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.diagnosticarImagenesVariante(varianteId)));
    }

    @PutMapping("/v1/imagenes/{varianteImagenId}/principal")
    public ResponseEntity<ResponseGeneric<String>> marcarImagenPrincipal(@PathVariable Integer varianteImagenId) {
        log.info("Marcar imagen principal varianteImagenId={}", varianteImagenId);
        sGenerico.marcarImagenPrincipalVariante(varianteImagenId);
        return ResponseEntity.ok(new ResponseGeneric<>("Imagen marcada como principal correctamente"));
    }

    @PostMapping("/v1/inicializarDesdeProducto")
    public ResponseEntity<ResponseGeneric<String>> guardarVariantesInicializarDesdeProducto(  @RequestPart("request") RequestVarianteDto requestVarianteDto,
                                                                                              @RequestPart(value = "files[]", required = false) MultipartFile[] files) {
        sGenerico.guardarVariantesPorProductoConImagenes(requestVarianteDto, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseGeneric<>("Variantes"));
    }
}
