package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.dto.negocio.LogoUploadDto;
import com.ventas.key.mis.productos.models.LogoDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.LogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/logos")
@RequiredArgsConstructor
public class LogoController {

    private final LogoService service;

    /** Solo ADMIN -- ver todos los logos subidos. */
    @GetMapping
    public ResponseEntity<ResponseGeneric<List<LogoDto>>> listar() {
        return ResponseEntity.ok(new ResponseGeneric<List<LogoDto>>(service.listar()));
    }

    /** Público -- cuál está activo hoy (lo usa el front para mostrarlo, ej. en Personalización). */
    @GetMapping("/activo")
    public ResponseEntity<ResponseGeneric<Optional<LogoDto>>> obtenerActivo() {
        return ResponseEntity.ok(new ResponseGeneric<>(service.obtenerActivo()));
    }

    /** Público -- bytes del logo, para que cargue embebido en el correo sin necesitar sesión. */
    @GetMapping("/{id}/imagen")
    public ResponseEntity<byte[]> obtenerImagen(@PathVariable Integer id) {
        try {
            byte[] bytes = service.obtenerBytes(id);
            return ResponseEntity.ok().contentType(service.obtenerMediaType(id)).body(bytes);
        } catch (IOException e) {
            return ResponseEntity.noContent().build();
        }
    }

    /** Solo ADMIN -- sube un logo nuevo (no reemplaza los existentes, se suman al catálogo). */
    @PostMapping
    public ResponseEntity<ResponseGeneric<LogoDto>> subir(@RequestBody LogoUploadDto dto) {
        return ResponseEntity.ok(new ResponseGeneric<>(service.subir(dto)));
    }

    /** Solo ADMIN -- marca este logo como el usado en el encabezado de los correos. */
    @PutMapping("/{id}/activar")
    public ResponseEntity<ResponseGeneric<LogoDto>> activar(@PathVariable Integer id) {
        return ResponseEntity.ok(new ResponseGeneric<>(service.activar(id)));
    }

    /** Solo ADMIN -- elimina un logo del catálogo. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseGeneric<Void>> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.ok(new ResponseGeneric<>(null));
    }
}
