package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.dto.CompletarProductoDto;
import com.ventas.key.mis.productos.dto.EstadoCargaProductoDto;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.api.ICargaImagenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/v1/carga-imagenes")
@RequiredArgsConstructor
@Slf4j
public class CargaImagenesController {

    private final ICargaImagenService cargaImagenService;

    @PostMapping(value = "subir-imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseGeneric<EstadoCargaProductoDto>> subirImagen(
            @RequestPart("imagen") MultipartFile imagen) {
        log.info("Carga rapida: recibiendo imagen {}", imagen.getOriginalFilename());
        try {
            byte[] bytes = imagen.getBytes();
            EstadoCargaProductoDto creado = cargaImagenService.crearBorrador();
            cargaImagenService.subirImagenAsync(creado.getProductoId(), creado.getVarianteId(), bytes, imagen.getOriginalFilename());
            return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseGeneric<>(creado));
        } catch (IOException e) {
            log.error("Error leyendo la imagen recibida: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, "No se pudo leer la imagen"));
        } catch (Exception e) {
            log.error("Error en carga rapida de imagen: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PostMapping(value = "{productoId}/reintentar-imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseGeneric<EstadoCargaProductoDto>> reintentarImagen(
            @PathVariable Integer productoId,
            @RequestPart("imagen") MultipartFile imagen) {
        log.info("Carga rapida: reintentando imagen para productoId={}", productoId);
        try {
            byte[] bytes = imagen.getBytes();
            EstadoCargaProductoDto estado = cargaImagenService.marcarPendienteParaReintento(productoId);
            cargaImagenService.subirImagenAsync(estado.getProductoId(), estado.getVarianteId(), bytes, imagen.getOriginalFilename());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ResponseGeneric<>(estado));
        } catch (IOException e) {
            log.error("Error leyendo la imagen recibida: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, "No se pudo leer la imagen"));
        } catch (Exception e) {
            log.error("Error en reintento de carga rapida productoId={}: {}", productoId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("estado")
    public ResponseEntity<ResponseGeneric<List<EstadoCargaProductoDto>>> consultarEstado(
            @RequestParam List<Integer> productoIds) {
        return ResponseEntity.ok(new ResponseGeneric<List<EstadoCargaProductoDto>>(cargaImagenService.consultarEstado(productoIds)));
    }

    @GetMapping("fallidas")
    public ResponseEntity<ResponseGeneric<List<EstadoCargaProductoDto>>> listarFallidas() {
        return ResponseEntity.ok(new ResponseGeneric<List<EstadoCargaProductoDto>>(cargaImagenService.listarFallidas()));
    }

    @PutMapping("{productoId}/completar")
    public ResponseEntity<ResponseGeneric<Producto>> completarProducto(
            @PathVariable Integer productoId,
            @RequestBody CompletarProductoDto request) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(cargaImagenService.completarProducto(productoId, request)));
        } catch (Exception e) {
            log.error("Error al completar producto borrador id={}: {}", productoId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }
}
