package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.models.*;
import com.ventas.key.mis.productos.service.ProductosServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/v1/productos")
@RequiredArgsConstructor
@Slf4j
public class ProductosControllerImpl {

    private final ProductosServiceImpl pServiceImpl;

    @GetMapping("obtenerProductos")
    public ResponseEntity<PginaDto<List<ProductoDTO>>> obtenerProductos(
            @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "1") int page) {
        log.info("obtener productos cahce size {} page {}", size, page);
        return ResponseEntity.status(HttpStatus.OK).body(this.pServiceImpl.getAll(size,page) );
    }

    @GetMapping("buscarNombreOrCodigoBarra")
    public ResponseEntity<PginaDto<List<ProductoDTO>>> buscarNombreOrCodigoBarra(
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam String nombre) {
        log.info("buscarNombreOrCodigoBarras {} page {} nombre {}", size, page, nombre);
        return ResponseEntity.status(HttpStatus.OK).body(this.pServiceImpl.findNombreOrCodigoBarra(size,page,nombre) );
    }

    @PostMapping("save")
    public ResponseEntity<Producto> save(@RequestBody ProductoDetalle producto) throws Exception{
        log.info("Inicia el guardado del producto {}", producto);
        return ResponseEntity.status(HttpStatus.OK).body(this.pServiceImpl.saveProductoLote(producto) );
    }

    @PutMapping("update")
    public ResponseEntity<Producto> update(@RequestBody ProductoDetalle producto) throws IOException {
        log.info("se actualizo el producto {}", producto);
        return ResponseEntity.status(HttpStatus.OK).body(this.pServiceImpl.saveProductoLote(producto));
    }

    @GetMapping("findById/{id}")
    public ResponseEntity<Optional<ProductoResumen>> update(@PathVariable int id){
        log.info("Se busca producto por ID {}",id);
        return ResponseEntity.status(HttpStatus.OK).body(this.pServiceImpl.getResumen(id) );
    }

    @DeleteMapping("deleteBy/{id}")
    public ResponseEntity<Optional<ProductoResumen>> eliminarProductoById(@PathVariable int id){
        log.info("Eliminar producto por ID {}",id);
        this.pServiceImpl.deleteByIdProducto(id);
        log.info("Se elimino el producto con el ID {}",id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("admin/no-habilitados")
    public ResponseEntity<PginaDto<List<ProductoDTO>>> getProductosNoHabilitados(
            @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "1") int page) {
        log.info("Admin: obtener productos no habilitados page={} size={}", page, size);
        return ResponseEntity.ok(this.pServiceImpl.getProductosNoHabilitados(size, page));
    }

    @GetMapping("admin/sin-stock")
    public ResponseEntity<PginaDto<List<ProductoDTO>>> getProductosSinStock(
            @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "1") int page) {
        log.info("Admin: obtener productos sin stock page={} size={}", page, size);
        return ResponseEntity.ok(this.pServiceImpl.getProductosSinStock(size, page));
    }

    // Filtro combinado de admin: nombreOCodigo + conStock + conImagenes + habilitado +
    // codigoGenerado son todos opcionales e independientes entre si (AND). Cada uno es tri-estado
    // via Boolean nullable: null = cualquiera, true/false = con/sin. codigoGenerado=true trae los
    // productos cuyo codigo de barras sigue siendo el autogenerado de la carga rapida.
    @GetMapping("admin/filtrar")
    public ResponseEntity<PginaDto<List<ProductoDTO>>> filtrarProductosAdmin(
            @RequestParam(required = false) String nombreOCodigo,
            @RequestParam(required = false) Boolean conStock,
            @RequestParam(required = false) Boolean conImagenes,
            @RequestParam(required = false) Boolean habilitado,
            @RequestParam(required = false) Boolean codigoGenerado,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "1") int page) {
        log.info("Admin: filtrar productos nombreOCodigo={} conStock={} conImagenes={} habilitado={} codigoGenerado={} page={} size={}",
                nombreOCodigo, conStock, conImagenes, habilitado, codigoGenerado, page, size);
        return ResponseEntity.ok(this.pServiceImpl.filtrarProductosAdmin(
                nombreOCodigo, conStock, conImagenes, habilitado, codigoGenerado, size, page));
    }

    @PutMapping("{id}/habilitar")
    public ResponseEntity<Map<String, Object>> habilitarDeshabilitarProducto(
            @PathVariable Integer id,
            @RequestParam boolean habilitar) {
        log.info("Cambiar estado habilitado del producto id={} habilitar={}", id, habilitar);
        this.pServiceImpl.habilitarDeshabilitarProducto(id, habilitar);
        return ResponseEntity.ok(Map.of(
                "id", id,
                "habilitado", habilitar,
                "mensaje", habilitar ? "Producto habilitado correctamente" : "Producto deshabilitado correctamente"
        ));
    }

    @PutMapping("admin/habilitar-lote")
    public ResponseEntity<ResponseGeneric<String>> habilitarDeshabilitarProductosLote(
            @Validated @RequestBody HabilitarLoteRequest request) {
        log.info("Admin: habilitar/deshabilitar productos en lote ids={} habilitar={}", request.getIds(), request.isHabilitar());
        try {
            this.pServiceImpl.habilitarDeshabilitarProductosLote(request.getIds(), request.isHabilitar());
            String mensaje = request.isHabilitar()
                    ? "Productos habilitados correctamente"
                    : "Productos deshabilitados correctamente";
            return ResponseEntity.ok(new ResponseGeneric<>(mensaje));
        } catch (Exception e) {
            log.error("Error al habilitar/deshabilitar productos en lote: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("admin/diagnostico-imagenes/{productoId}")
    public ResponseEntity<DiagnosticoImagenProductoDto> diagnosticarImagenesProducto(@PathVariable Integer productoId) {
        log.info("Diagnóstico de imágenes para producto id={}", productoId);
        return ResponseEntity.ok(this.pServiceImpl.diagnosticarImagenesProducto(productoId));
    }

    @GetMapping("admin/sin-variantes/reporte")
    public ResponseEntity<byte[]> getReporteProductosSinVariantes() throws IOException {
        log.info("Admin: generar reporte de productos sin variantes");
        byte[] excel = this.pServiceImpl.generarReporteProductosSinVariantes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "productos_sin_variantes.xlsx");
        return ResponseEntity.ok().headers(headers).body(excel);
    }

    @PostMapping("/compartir-imagenes-variantes")
    public ResponseEntity<CompartirImagenesVarianteDto> diagnosticarImagenesProducto(@RequestBody CompartirImagenesVarianteDto compartirImagenesVarianteDto) {
        log.info("Compartir imagenes a variantes {}", compartirImagenesVarianteDto);
        return ResponseEntity.ok(this.pServiceImpl.compartirImagenesVarianteDto(compartirImagenesVarianteDto));
    }


}
