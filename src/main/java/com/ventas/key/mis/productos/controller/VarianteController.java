package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.dto.variantes.IndependizarVarianteRequestDto;
import com.ventas.key.mis.productos.dto.variantes.RequestVarianteDto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.models.DiagnosticoImagenVarianteDto;
import com.ventas.key.mis.productos.models.FiltrosDisponiblesDto;
import com.ventas.key.mis.productos.models.HabilitarLoteRequest;
import com.ventas.key.mis.productos.models.ImagenUpdateDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.VarianteDetalle;
import com.ventas.key.mis.productos.models.VarianteResumenDto;
import com.ventas.key.mis.productos.models.variantes.IndependizarVarianteResponseDto;
import com.ventas.key.mis.productos.models.variantes.ProductoIdDto;
import com.ventas.key.mis.productos.models.variantes.VarianteDto;
import com.ventas.key.mis.productos.service.VarianteServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/v1/variantes")
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

    // getAll, getOne, save, update y delete NO se declaran aqui: los publica AbstractController
    // y ya salen bajo /v1/variantes por el @RequestMapping de esta clase. Declararlos otra vez
    // arranca en "Ambiguous mapping" y la app no levanta.

    @GetMapping("/porProducto/{productoId}")
    public ResponseEntity<ResponseGeneric<List<VarianteDto>>> getPorProducto(@PathVariable Integer productoId) {
        return ResponseEntity.ok(new ResponseGeneric<List<VarianteDto>>(sGenerico.buscarPorProducto(productoId)));
    }

    // Publico a proposito (cae en el permitAll de GET /tienda/** de SecurityConfig, no matchea
    // los patrones getAll/getOne que se cerraron a ADMIN). Resuelve varianteId -> productoId para
    // la ficha de producto cuando el cliente entra por un link directo/marcador y no trae el
    // productoId a mano -- el resto de los datos los sigue sacando de /v1/porProducto/{productoId},
    // que ya es publico.
    @GetMapping("/variante/{varianteId}/producto-id")
    public ResponseEntity<ResponseGeneric<ProductoIdDto>> getProductoIdPorVariante(@PathVariable Integer varianteId) {
        return ResponseEntity.ok(new ResponseGeneric<>(new ProductoIdDto(sGenerico.resolverProductoId(varianteId))));
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
            @RequestParam(required = false) String termino,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.buscarVariantes(termino, pagina, size)));
    }

    // Catalogo publico con filtros combinables. A diferencia de /v1/buscar (que hace cascada
    // codigo -> palabra clave -> nombre y truena si no hay resultados), este endpoint combina
    // termino + precio/talla/color/marca con AND y simplemente devuelve lista vacia si no matchea.
    @GetMapping("/buscar-filtrado")
    public ResponseEntity<ResponseGeneric<PginaDto<List<VarianteResumenDto>>>> buscarFiltrado(
            @RequestParam(required = false) String termino,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) String talla,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String marca,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.buscarVariantesPublicoFiltrado(
                termino, precioMin, precioMax, talla, color, marca, pagina, size)));
    }

    @GetMapping("/filtros-disponibles")
    public ResponseEntity<ResponseGeneric<FiltrosDisponiblesDto>> filtrosDisponibles() {
        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.filtrosDisponiblesPublico()));
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

    /**
     * @deprecated Usar GET /tienda/v1/imagenes/{varianteId} — no verifica existencia en micro
     */
    @Deprecated
    @GetMapping("/v3/imagenes/{varianteId}")
    public ResponseEntity<ResponseGeneric<List<ImagenUpdateDto>>> getImagenes(@PathVariable Integer varianteId) {
        return ResponseEntity.ok(new ResponseGeneric<List<ImagenUpdateDto>>(sGenerico.getImagenesPorVariante(varianteId)));
    }

    @GetMapping("/imagenes/{varianteId}")
    public ResponseEntity<ResponseGeneric<List<ImagenUpdateDto>>> getImagenesV2(@PathVariable Integer varianteId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<List<ImagenUpdateDto>>(sGenerico.getImagenesPorVarianteV2(varianteId)));
        } catch (Exception e) {
            log.error("Error obteniendo imagenes de varianteId={}: {}", varianteId, e.getMessage(), e);
            List<ImagenUpdateDto> vacio = List.of();
            return ResponseEntity.ok(new ResponseGeneric<List<ImagenUpdateDto>>(vacio));
        }
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

    /** @deprecated Usar DELETE /tienda/v1/imagenes */
    @Deprecated
    @DeleteMapping("/v3/imagenes")
    public ResponseEntity<ResponseGeneric<String>> eliminarImagenesDeVariantes(@RequestBody List<Integer> varianteIds) {
        sGenerico.eliminarImagenesDeVariantes(varianteIds);
        return ResponseEntity.ok(new ResponseGeneric<>("Imágenes eliminadas correctamente"));
    }

    @DeleteMapping("/imagenes")
    public ResponseEntity<ResponseGeneric<String>> eliminarImagenesDeVariantesV2(@RequestBody List<Integer> varianteIds) {
        sGenerico.eliminarImagenesDeVariantes(varianteIds);
        return ResponseEntity.ok(new ResponseGeneric<>("Imágenes eliminadas correctamente"));
    }

    /** @deprecated Usar DELETE /tienda/v1/{varianteId}/imagenes */
    @Deprecated
    @DeleteMapping("/v3/{varianteId}/imagenes")
    public ResponseEntity<ResponseGeneric<String>> eliminarImagenesEspecificas(
            @PathVariable Integer varianteId,
            @RequestBody List<Long> imagenIds) {
        sGenerico.eliminarImagenesEspecificas(varianteId, imagenIds);
        return ResponseEntity.ok(new ResponseGeneric<>("Imágenes eliminadas correctamente"));
    }

    @DeleteMapping("/{varianteId}/imagenes")
    public ResponseEntity<ResponseGeneric<String>> eliminarImagenesEspecificasV2(
            @PathVariable Integer varianteId,
            @RequestBody List<Long> imagenIds) {
        sGenerico.eliminarImagenesEspecificas(varianteId, imagenIds);
        return ResponseEntity.ok(new ResponseGeneric<>("Imágenes eliminadas correctamente"));
    }

    @GetMapping("/admin/sin-stock")
    public ResponseEntity<ResponseGeneric<PginaDto<List<VarianteResumenDto>>>> getVariantesSinStockDeshabilitadas(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.getVariantesSinStockDeshabilitadas(pagina, size)));
    }

    // Filtro combinado de admin: nombreOCodigo + conStock + conImagenes + habilitado +
    // codigoGenerado son todos opcionales e independientes entre si (AND). Cada uno es tri-estado
    // via Boolean nullable: null = cualquiera, true/false = con/sin. codigoGenerado filtra por el
    // codigo de barras autogenerado de la carga rapida (producto padre); habilitado usa el estado
    // efectivo variante+producto.
    @GetMapping("/admin/filtrar")
    public ResponseEntity<ResponseGeneric<PginaDto<List<VarianteResumenDto>>>> filtrarVariantesAdmin(
            @RequestParam(required = false) String nombreOCodigo,
            @RequestParam(required = false) Boolean conStock,
            @RequestParam(required = false) Boolean conImagenes,
            @RequestParam(required = false) Boolean habilitado,
            @RequestParam(required = false) Boolean codigoGenerado,
            // Mismo filtro de fecha que ProductosControllerImpl.filtrarProductosAdmin -- ver
            // comentario ahi para el motivo (codigo de barras al azar de la carga rapida).
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.filtrarVariantesAdmin(
                nombreOCodigo, conStock, conImagenes, habilitado, codigoGenerado, fechaDesde, fechaHasta, pagina, size)));
    }

    // Faltaba: el front (Tienda → Buscar) llama esto para el toggle individual de una sola
    // variante desde hace rato -- solo existia el de lote, asi que ese boton daba 404 siempre
    // (encontrado 2026-08-27, auditoria de correctitud). Mismo patron que
    // ProductosControllerImpl.habilitarDeshabilitarProducto, reusando el metodo de lote con una
    // lista de un solo id para no duplicar la logica de guardado/relectura.
    @PutMapping("/{id}/habilitar")
    public ResponseEntity<Map<String, Object>> habilitarDeshabilitarVariante(
            @PathVariable Integer id,
            @RequestParam boolean habilitar) {
        log.info("Cambiar estado habilitado de la variante id={} habilitar={}", id, habilitar);
        sGenerico.habilitarDeshabilitarVariantesLote(List.of(id), habilitar);
        return ResponseEntity.ok(Map.of(
                "id", id,
                "habilitado", habilitar,
                "mensaje", habilitar ? "Variante habilitada correctamente" : "Variante deshabilitada correctamente"
        ));
    }

    @PutMapping("/admin/habilitar-lote")
    public ResponseEntity<ResponseGeneric<String>> habilitarDeshabilitarVariantesLote(
            @Validated @RequestBody HabilitarLoteRequest request) {
        try {
            String diagnostico = sGenerico.habilitarDeshabilitarVariantesLote(request.getIds(), request.isHabilitar());
            String mensaje = (request.isHabilitar()
                    ? "Variantes habilitadas correctamente. "
                    : "Variantes deshabilitadas correctamente. ") + diagnostico;
            return ResponseEntity.ok(new ResponseGeneric<>(mensaje));
        } catch (Exception e) {
            log.error("Error al habilitar/deshabilitar variantes en lote: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/admin/diagnostico-imagenes/{varianteId}")
    public ResponseEntity<ResponseGeneric<DiagnosticoImagenVarianteDto>> diagnosticarImagenesVariante(
            @PathVariable Integer varianteId) {
        log.info("Diagnóstico de imágenes para variante id={}", varianteId);
        return ResponseEntity.ok(new ResponseGeneric<>(sGenerico.diagnosticarImagenesVariante(varianteId)));
    }

    @PutMapping("/imagenes/{varianteImagenId}/principal")
    public ResponseEntity<ResponseGeneric<String>> marcarImagenPrincipal(@PathVariable Integer varianteImagenId) {
        log.info("Marcar imagen principal varianteImagenId={}", varianteImagenId);
        sGenerico.marcarImagenPrincipalVariante(varianteImagenId);
        return ResponseEntity.ok(new ResponseGeneric<>("Imagen marcada como principal correctamente"));
    }

    @PostMapping("/inicializarDesdeProducto")
    public ResponseEntity<ResponseGeneric<String>> guardarVariantesInicializarDesdeProducto(  @RequestPart("request") RequestVarianteDto requestVarianteDto,
                                                                                              @RequestPart(value = "files[]", required = false) MultipartFile[] files) {
        sGenerico.guardarVariantesPorProductoConImagenes(requestVarianteDto, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseGeneric<>("Variantes"));
    }

    @PostMapping("/{varianteId}/independizar")
    public ResponseEntity<ResponseGeneric<IndependizarVarianteResponseDto>> independizarVariante(
            @PathVariable Integer varianteId,
            @RequestBody IndependizarVarianteRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseGeneric<>(sGenerico.independizarVariante(varianteId, request)));
    }
}
