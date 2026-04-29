package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.dto.ResultadoCargaDto;
import com.ventas.key.mis.productos.service.api.ISubirDocumentosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/documentos")
@RequiredArgsConstructor
public class SubirDocumentosController {

    private final ISubirDocumentosService subirDocumentosService;

    @PostMapping(value = "/productos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResultadoCargaDto> subirProductos(@RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(subirDocumentosService.procesarExcelProductos(archivo));
    }
}