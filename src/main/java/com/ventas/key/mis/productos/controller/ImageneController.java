package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.service.api.IImagenService;
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
@RequestMapping("/imagen")
public class ImageneController {


    @Autowired
    private IImagenService iImagenService;


//    @GetMapping("/{id}")
//    @Cacheable(value = "imagenes", key = "#id")
//    public ResponseEntity<byte[]> getImagen(@PathVariable Integer id) throws Exception {
//        Imagen imagen = iImagenService.findById(id)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen no encontrada"));
//        MediaType mediaType = getMediaType(imagen.getExtension());
//        return ResponseEntity.ok()
//                .contentType(mediaType)
//                .body(imagen.getBase64());
//    }
//    @GetMapping("/{id}/detalle")
//    @Cacheable(value = "detalle", key = "'id:' + #id + ':page:' + #page + ':size:' + #size")
//    public ResponseEntity<List<byte[]>> getDetalle(@PathVariable Integer id,
//                                                   @RequestParam int size,
//                                                   @RequestParam int page) {
//        List<byte[]> imagen = iImagenService.findImagenPrincipalPorProductoIds(id, page, size);
//        return ResponseEntity.ok()
//                .body(imagen);
//    }

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

}
