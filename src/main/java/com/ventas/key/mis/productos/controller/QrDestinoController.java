package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.dto.qr.QrDestinoCreateDto;
import com.ventas.key.mis.productos.dto.qr.QrDestinoPublicoDto;
import com.ventas.key.mis.productos.dto.qr.QrDestinoUpdateDto;
import com.ventas.key.mis.productos.entity.QrDestino;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.QrDestinoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Destinos configurables del generador de códigos QR (P3 del "para después").
 *
 * El QR se dibuja en el front con angularx-qrcode; aquí sólo vive la lista de a dónde puede
 * apuntar, para que dar de alta un destino nuevo no necesite tocar código ni desplegar.
 */
@RestController
@RequestMapping("/v1/qr-destinos")
@RequiredArgsConstructor
@Slf4j
public class QrDestinoController {

    private final QrDestinoService qrDestinoService;

    /** Lista sólo los destinos activos — es la que consume la pantalla que dibuja los QR. */
    @GetMapping("/publico")
    public ResponseEntity<ResponseGeneric<List<QrDestinoPublicoDto>>> listarPublico() {
        return ResponseEntity.ok(new ResponseGeneric<List<QrDestinoPublicoDto>>(qrDestinoService.listarPublico()));
    }

    /** Lista completa (activos e inactivos) — para la pantalla de administración. */
    @GetMapping
    public ResponseEntity<ResponseGeneric<List<QrDestino>>> listarTodos() {
        return ResponseEntity.ok(new ResponseGeneric<List<QrDestino>>(qrDestinoService.listarTodos()));
    }

    @PostMapping
    public ResponseEntity<ResponseGeneric<QrDestino>> crear(@RequestBody QrDestinoCreateDto dto) {
        log.info("Alta de destino de QR: {}", dto.getNombre());
        return ResponseEntity.ok(new ResponseGeneric<>(qrDestinoService.crear(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseGeneric<QrDestino>> actualizar(
            @PathVariable Integer id, @RequestBody QrDestinoUpdateDto dto) {
        return ResponseEntity.ok(new ResponseGeneric<>(qrDestinoService.actualizar(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseGeneric<String>> eliminar(@PathVariable Integer id) {
        qrDestinoService.eliminar(id);
        return ResponseEntity.ok(new ResponseGeneric<>("Destino de QR eliminado correctamente"));
    }
}
