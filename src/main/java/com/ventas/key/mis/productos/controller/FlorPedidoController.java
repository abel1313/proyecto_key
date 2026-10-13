package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.floreseternas.CalcularPrecioRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.CalcularPrecioResponseDto;
import com.ventas.key.mis.productos.models.floreseternas.FechasDisponiblesRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.FechasDisponiblesResponseDto;
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
        return ResponseEntity.ok(new ResponseGeneric<>(florPedidoService.validarCantidad(request)));
    }

    @PostMapping("/calcular-precio")
    public ResponseEntity<ResponseGeneric<CalcularPrecioResponseDto>> calcularPrecio(
            @RequestBody CalcularPrecioRequestDto request) {
        return ResponseEntity.ok(new ResponseGeneric<>(florPedidoService.calcularPrecio(request)));
    }

    @PostMapping("/fechas-disponibles")
    public ResponseEntity<ResponseGeneric<FechasDisponiblesResponseDto>> fechasDisponibles(
            @RequestBody FechasDisponiblesRequestDto request) {
        return ResponseEntity.ok(new ResponseGeneric<>(florPedidoService.fechasDisponibles(request)));
    }
}
