package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.floreseternas.CalcularPrecioRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.CalcularPrecioResponseDto;
import com.ventas.key.mis.productos.models.floreseternas.ValidarCantidadRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.ValidarCantidadResponseDto;
import com.ventas.key.mis.productos.service.FlorPedidoServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Motor de calculo del configurador de ramo a la medida (ver PROPUESTA_FLORES_ETERNAS.md).
// Publico: el cliente puede cotizar un ramo sin necesidad de estar logueado.
@Slf4j
@RestController
@RequestMapping("v1/flores")
public class FlorPedidoController {

    private final FlorPedidoServiceImpl florPedidoService;

    public FlorPedidoController(FlorPedidoServiceImpl florPedidoService) {
        this.florPedidoService = florPedidoService;
    }

    @PostMapping("/validar-cantidad")
    public ResponseEntity<ResponseGeneric<ValidarCantidadResponseDto>> validarCantidad(
            @RequestBody ValidarCantidadRequestDto request) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(florPedidoService.validarCantidad(request)));
        } catch (Exception e) {
            log.error("Error al validar cantidad de flores: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PostMapping("/calcular-precio")
    public ResponseEntity<ResponseGeneric<CalcularPrecioResponseDto>> calcularPrecio(
            @RequestBody CalcularPrecioRequestDto request) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(florPedidoService.calcularPrecio(request)));
        } catch (Exception e) {
            log.error("Error al calcular precio de ramo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }
}
