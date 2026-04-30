package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.dto.negocio.ImagenPresentacionUpdateDto;
import com.ventas.key.mis.productos.entity.ImagenPresentacion;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.ImagenPresentacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/presentacion")
@RequiredArgsConstructor
public class ImagenPresentacionController {

    private final ImagenPresentacionService service;

    /** Público — imágenes activas por tipo: LOGIN o REGISTRO */
    @GetMapping("/imagenes")
    public ResponseEntity<ResponseGeneric<List<ImagenPresentacion>>> getImagenes(
            @RequestParam String tipo) {
        return ResponseEntity.ok(new ResponseGeneric<List<ImagenPresentacion>>(service.getImagenesPorTipo(tipo)));
    }

    /** Público — bytes de la imagen desde disco */
    @GetMapping("/imagenes/{id}/imagen")
    public ResponseEntity<byte[]> getImagen(@PathVariable Integer id) throws IOException {
        byte[] bytes = service.getImagenBytes(id);
        return ResponseEntity.ok()
                .contentType(service.getMediaType(id))
                .body(bytes);
    }

    /** Solo ADMIN — ver todas (activas e inactivas) */
    @GetMapping("/imagenes/todas")
    public ResponseEntity<ResponseGeneric<List<ImagenPresentacion>>> getTodas() {
        return ResponseEntity.ok(new ResponseGeneric<List<ImagenPresentacion>>(service.getTodas()));
    }

    /** Solo ADMIN — actualizar imagen y metadatos por id */
    @PutMapping("/imagenes/{id}")
    public ResponseEntity<ResponseGeneric<ImagenPresentacion>> actualizar(
            @PathVariable Integer id,
            @RequestBody ImagenPresentacionUpdateDto dto) {
        return ResponseEntity.ok(new ResponseGeneric<>(service.actualizar(id, dto)));
    }
}