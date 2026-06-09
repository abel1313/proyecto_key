package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.config.RabbitMQConfig;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenPort;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenProductoPort;
import com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto;
import com.ventas.key.mis.productos.models.PageableDto;
import com.ventas.key.mis.productos.models.ProductoImagenDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.CacheService;
import com.ventas.key.mis.productos.service.api.IImagenService;
import com.ventas.key.mis.productos.service.api.IProductoImagenService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/imagen")
@Slf4j
public class ImageneController {


    private final IImagenService iImagenService;
    private final IProductoImagenService iProductoImagenService;
    private final ImagenProductoPort imagenProductoPort;
    private final ImagenPort imagenPort;
    private final CacheService cacheService;
    private final RabbitTemplate rabbitTemplate;


    /**
     * @deprecated Migrar a GET /imagen/v1/{productoId} que delega al microservicio de imágenes.
     */
    @Deprecated
    @GetMapping("/v3/{id}")
    @Cacheable(value = "imagenes", key = "#id")
    public ResponseEntity<byte[]> getImagen(@PathVariable Integer id) throws Exception {
        com.ventas.key.mis.productos.hexagonal.dominio.Imagen imagen = iImagenService.findByIdImg(id);
        MediaType mediaType = getMediaType(imagen.getContentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(imagen.getImagen());
    }

    @GetMapping("/v1/{productoId}")
    @Cacheable(value = "imagenes", key = "#productoId")
    public ResponseEntity<byte[]> getImagenV2(@PathVariable Integer productoId) throws Exception {
        com.ventas.key.mis.productos.hexagonal.dominio.Imagen imagen = imagenProductoPort.buscarImagenProducto(productoId);
        if (imagen == null || imagen.getImagen() == null) {
            log.warn("No se encontró imagen en disco para productoId={}", productoId);
            return ResponseEntity.noContent().build();
        }
        MediaType mediaType = getMediaType(imagen.getContentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(imagen.getImagen());
    }
    /**
     * @deprecated Migrar a GET /imagen/v1/{productoId}/detalle que obtiene bytes del micro de imágenes.
     */
    @Deprecated
    @GetMapping("/v3/{id}/detalle")
    public ResponseEntity<PageableDto> getDetalle(@PathVariable Integer id,
                                                  @RequestParam int size,
                                                  @RequestParam int page) {
        log.info("llegamos al detalle {}", id);
        PageableDto imagen = iImagenService.findImagenPrincipalPorProductoIds(id, page, size);
        log.info("despues de la imagen {}", imagen);
        return ResponseEntity.ok()
                .body(imagen);
    }

    // RabbitMQ: NO aplica — lectura síncrona paginada. Los bytes se obtienen del micro de imágenes vía HTTP.
    @GetMapping("/v1/{productoId}/detalle")
    public ResponseEntity<PageableDto> getDetalleV2(@PathVariable Integer productoId,
                                                    @RequestParam int size,
                                                    @RequestParam int page) {
        PageableDto resultado = iImagenService.findImagenPrincipalPorProductoIdsV2(productoId, page, size);
        return ResponseEntity.ok(resultado);
    }
    /**
     * @deprecated Migrar a GET /imagen/v1/file/{imagenId} que obtiene bytes del micro de imágenes.
     */
    @Deprecated
    @GetMapping("/v3/file/{imagenId}")
    public ResponseEntity<byte[]> getImagenByImagenId(@PathVariable Long imagenId) throws Exception {
        com.ventas.key.mis.productos.hexagonal.dominio.Imagen imagen = iImagenService.findByImagenId(imagenId);
        MediaType mediaType = getMediaType(imagen.getContentType());
        return ResponseEntity.ok().contentType(mediaType).body(imagen.getImagen());
    }

    // RabbitMQ: NO aplica — lectura síncrona por ID de imagen.
    @GetMapping("/v1/file/{imagenId}")
    public ResponseEntity<byte[]> getImagenByImagenIdV2(@PathVariable Long imagenId) {
        ImagenDto imagenDto = imagenPort.getOne(imagenId);
        if (imagenDto == null || imagenDto.getImagen() == null) {
            log.warn("No se encontró imagen en micro para imagenId={}", imagenId);
            return ResponseEntity.noContent().build();
        }
        MediaType mediaType = imagenDto.getContentType() != null
                ? getMediaType(imagenDto.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(mediaType).body(imagenDto.getImagen());
    }

    /**
     * @deprecated Migrar a GET /imagen/v1/{idProducto}/imagenes — URLs apuntan a /imagen/v1/file/
     */
    @Deprecated
    @GetMapping("/v3/{idProducto}/imagenes")
    public ResponseEntity<ProductoImagenDto> getImagenesPorProductoId(@PathVariable Integer idProducto){
        return ResponseEntity.ok(this.iProductoImagenService.findByImagenesPorIdProducto(idProducto));
    }

    // RabbitMQ: NO aplica — lectura síncrona.
    @GetMapping("/v1/{idProducto}/imagenes")
    public ResponseEntity<ProductoImagenDto> getImagenesPorProductoIdV2(@PathVariable Integer idProducto){
        return ResponseEntity.ok(this.iProductoImagenService.findByImagenesPorIdProductoV2(idProducto));
    }

    /** @deprecated Migrar a DELETE /imagen/v1/{idImagen} que también elimina del micro */
    @Deprecated
    @DeleteMapping("/v3/{idImagen}")
    public ResponseEntity<ResponseGeneric<String>> deleteById(@PathVariable Long idImagen) throws Exception {
        this.iImagenService.deleteById(idImagen);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ResponseGeneric<>("Se elimino correctamente"));
    }

    // TODO: RabbitMQ — candidato para publicar evento "imagen.eliminada" en vez de HTTP síncrono al micro
    @DeleteMapping("/v1/{idImagen}")
    public ResponseEntity<ResponseGeneric<String>> deleteByIdV2(@PathVariable Long idImagen) {
        this.iImagenService.deleteByIdV2(idImagen);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ResponseGeneric<>("Se elimino correctamente"));
    }

    /** @deprecated Migrar a DELETE /imagen/v1/{productoId}/imagenes */
    @Deprecated
    @DeleteMapping("/v3/{productoId}/imagenes")
    public ResponseEntity<ResponseGeneric<String>> eliminarImagenesEspecificas(
            @PathVariable Integer productoId,
            @RequestBody List<Long> imagenIds) {
        this.iProductoImagenService.eliminarImagenesEspecificas(productoId, imagenIds);
        return ResponseEntity.ok(new ResponseGeneric<>("Imágenes eliminadas correctamente"));
    }

    // RabbitMQ: NO aplica — misma lógica que v1, ya llama al micro internamente
    @DeleteMapping("/v1/{productoId}/imagenes")
    public ResponseEntity<ResponseGeneric<String>> eliminarImagenesEspecificasV2(
            @PathVariable Integer productoId,
            @RequestBody List<Long> imagenIds) {
        this.iProductoImagenService.eliminarImagenesEspecificas(productoId, imagenIds);
        return ResponseEntity.ok(new ResponseGeneric<>("Imágenes eliminadas correctamente"));
    }

    /** @deprecated Migrar a DELETE /imagen/v1/producto */
    @Deprecated
    @DeleteMapping("/v3/producto")
    public ResponseEntity<ResponseGeneric<String>> eliminarImagenesDeProductos(@RequestBody List<Integer> productoIds) {
        this.iProductoImagenService.eliminarImagenesDeProductos(productoIds);
        return ResponseEntity.ok(new ResponseGeneric<>("Imágenes de producto eliminadas correctamente"));
    }

    // RabbitMQ: NO aplica — misma lógica que v1, ya llama al micro internamente
    @DeleteMapping("/v1/producto")
    public ResponseEntity<ResponseGeneric<String>> eliminarImagenesDeProductosV2(@RequestBody List<Integer> productoIds) {
        this.iProductoImagenService.eliminarImagenesDeProductos(productoIds);
        return ResponseEntity.ok(new ResponseGeneric<>("Imágenes de producto eliminadas correctamente"));
    }
    private MediaType getMediaType(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
    /** @deprecated Migrar a GET /imagen/v1/cache/limpiar */
    @Deprecated
    @GetMapping("/v3/cache/imagen/limpiar")
    @CacheEvict(value = "imagenes", allEntries = true)
    public void limpiarTodaLaCacheDeImagenes() {
    }

    @GetMapping("/v1/cache/limpiar")
    public ResponseEntity<Void> limpiarCacheImagenesV2() {
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        return ResponseEntity.noContent().build();
    }

    @CacheEvict(value = "detalleImagen", allEntries = true)
    public void deleteByImg() {

    }

}
