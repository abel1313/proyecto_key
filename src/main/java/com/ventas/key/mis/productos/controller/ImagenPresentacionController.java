package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.dto.negocio.ImagenPresentacionUpdateDto;
import com.ventas.key.mis.productos.entity.ImagenPresentacion;
import com.ventas.key.mis.productos.models.ImagenPresentacionDto;
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

    /**
     * @deprecated Migrar a GET /presentacion/v2/imagenes — retorna DTO con urlImagen apuntando al micro.
     * Público — imágenes activas por tipo: LOGIN o REGISTRO
     */
    @Deprecated
    @GetMapping("/imagenes")
    public ResponseEntity<ResponseGeneric<List<ImagenPresentacion>>> getImagenes(
            @RequestParam String tipo) {
        return ResponseEntity.ok(new ResponseGeneric<List<ImagenPresentacion>>(service.getImagenesPorTipo(tipo)));
    }

    // RabbitMQ: NO aplica — lectura síncrona.
    // Cache "presentacion-imagenes" se invalida vía Rabbit cuando se implemente PUT /presentacion/v2/imagenes/{id}.
    @GetMapping("/v2/imagenes")
    public ResponseEntity<ResponseGeneric<List<ImagenPresentacionDto>>> getImagenesV2(
            @RequestParam String tipo) {
        return ResponseEntity.ok(new ResponseGeneric<List<ImagenPresentacionDto>>(service.getImagenesPorTipoV2(tipo)));
    }

    /**
     * @deprecated Usar GET /presentacion/v2/imagenes/{id}/imagen
     * Público — bytes de la imagen desde disco local
     */
    @Deprecated
    @GetMapping("/imagenes/{id}/imagen")
    public ResponseEntity<byte[]> getImagen(@PathVariable Integer id) throws IOException {
        byte[] bytes = service.getImagenBytes(id);
        return ResponseEntity.ok()
                .contentType(service.getMediaType(id))
                .body(bytes);
    }

    /**
     * Público — bytes de la imagen de presentación.
     * Por ahora lee del disco local (mismo que v1). Cuando se migre PUT /presentacion/v2/imagenes/{id}
     * al micro, este endpoint pasará a obtener los bytes de micro_imagenes.
     */
    @GetMapping("/v2/imagenes/{id}/imagen")
    public ResponseEntity<byte[]> getImagenV2(@PathVariable Integer id) {
        try {
            byte[] bytes = service.getImagenBytes(id);
            return ResponseEntity.ok()
                    .contentType(service.getMediaType(id))
                    .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.noContent().build();
        }
    }

    /**
     * @deprecated Usar GET /presentacion/v2/imagenes/todas — expone nombreArchivo (ruta interna de disco)
     * Solo ADMIN — ver todas (activas e inactivas)
     */
    @Deprecated
    @GetMapping("/imagenes/todas")
    public ResponseEntity<ResponseGeneric<List<ImagenPresentacion>>> getTodas() {
        return ResponseEntity.ok(new ResponseGeneric<List<ImagenPresentacion>>(service.getTodas()));
    }

    /** Solo ADMIN — ver todas (activas e inactivas) con urlImagen calculada */
    @GetMapping("/v2/imagenes/todas")
    public ResponseEntity<ResponseGeneric<List<ImagenPresentacionDto>>> getTodasV2() {
        return ResponseEntity.ok(new ResponseGeneric<List<ImagenPresentacionDto>>(service.getTodasV2()));
    }

    /**
     * @deprecated Usar PUT /presentacion/v2/imagenes/{id} — no invalida caché y devuelve entidad con nombreArchivo interno
     * Solo ADMIN — actualizar imagen y metadatos por id
     */
    @Deprecated
    @PutMapping("/imagenes/{id}")
    public ResponseEntity<ResponseGeneric<ImagenPresentacion>> actualizar(
            @PathVariable Integer id,
            @RequestBody ImagenPresentacionUpdateDto dto) {
        return ResponseEntity.ok(new ResponseGeneric<>(service.actualizar(id, dto)));
    }

    /** Solo ADMIN — actualizar imagen y metadatos; invalida caché presentacion-imagenes */
    @PutMapping("/v2/imagenes/{id}")
    public ResponseEntity<ResponseGeneric<ImagenPresentacionDto>> actualizarV2(
            @PathVariable Integer id,
            @RequestBody ImagenPresentacionUpdateDto dto) {
        return ResponseEntity.ok(new ResponseGeneric<>(service.actualizarV2(id, dto)));
    }
}