package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.models.ImagenProductoDto;
import com.ventas.key.mis.productos.models.PageableDto;
import com.ventas.key.mis.productos.models.ProductoImagenDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.repository.IProductoImagenRepository;
import com.ventas.key.mis.productos.service.api.IImagenService;
import com.ventas.key.mis.productos.service.api.IProductoImagenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/imagen")
@Slf4j
public class ImageneController {


    private final IImagenService iImagenService;
    private final IProductoImagenService iProductoImagenService;


    @GetMapping("/{id}")
    @Cacheable(value = "imagenes", key = "#id")
    public ResponseEntity<byte[]> getImagen(@PathVariable Integer id) throws Exception {
        com.ventas.key.mis.productos.hexagonal.dominio.Imagen imagen = iImagenService.findByIdImg(id);
        MediaType mediaType = getMediaType(imagen.getContentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(imagen.getImagen());
    }
    @GetMapping("/{id}/detalle")
    @Cacheable(value = "detalle", key = "'id:' + #id + ':page:' + #page + ':size:' + #size")
    public ResponseEntity<PageableDto> getDetalle(@PathVariable Integer id,
                                                  @RequestParam int size,
                                                  @RequestParam int page) {
        log.info("llegamos al detalle {}", id);
        PageableDto imagen = iImagenService.findImagenPrincipalPorProductoIds(id, page, size);
        log.info("despues de la imagen {}", imagen);
        return ResponseEntity.ok()
                .body(imagen);
    }
    @GetMapping("/{idProducto}/imagenes")
    @Cacheable(value = "detalleImagen", key = "#idProducto")
    public ResponseEntity<ProductoImagenDto> getImagenesPorProductoId(@PathVariable Integer idProducto){
        return ResponseEntity.ok(this.iProductoImagenService.findByImagenesPorIdProducto(idProducto));
    }

    @DeleteMapping("/{idImagen}")
    @CacheEvict(value = "detalleImagen", allEntries = true)
    public ResponseEntity<ResponseGeneric<String>> deleteById(@PathVariable Long idImagen) throws Exception {
        ResponseGeneric<String> response = new ResponseGeneric<>("Se eleimino correctamente");
        this.iImagenService.deleteById(idImagen);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    private MediaType getMediaType(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
    @GetMapping("/cache/imagen/limpiar")
    @CacheEvict(value = "imagenes", allEntries = true)
    public void limpiarTodaLaCacheDeImagenes() {

    }

    @CacheEvict(value = "detalleImagen", allEntries = true)
    public void deleteByImg() {

    }

}
