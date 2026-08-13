package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleResponseDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleValidarFraseRequestDto;
import com.ventas.key.mis.productos.service.RamoPedidoDetalleServiceImpl;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Adjunta el "ticket de produccion" de un ramo a un Pedido ya creado por el flujo normal
// (POST /v1/pedidos/savePedido). Ver RamoPedidoDetalleServiceImpl para el porque.
@Slf4j
@RestController
@RequestMapping("v1/flores/pedidos")
public class RamoPedidoDetalleController {

    private final RamoPedidoDetalleServiceImpl ramoPedidoDetalleService;

    public RamoPedidoDetalleController(RamoPedidoDetalleServiceImpl ramoPedidoDetalleService) {
        this.ramoPedidoDetalleService = ramoPedidoDetalleService;
    }

    @PostMapping("/{pedidoId}/detalle")
    public ResponseEntity<ResponseGeneric<RamoPedidoDetalleResponseDto>> adjuntar(
            @PathVariable Integer pedidoId, @RequestBody RamoPedidoDetalleRequestDto request) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ramoPedidoDetalleService.adjuntar(pedidoId, request)));
        } catch (Exception e) {
            log.error("Error al adjuntar detalle de ramo al pedido {}: {}", pedidoId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/{pedidoId}/detalle")
    public ResponseEntity<ResponseGeneric<RamoPedidoDetalleResponseDto>> listarPorPedido(
            @PathVariable Integer pedidoId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ramoPedidoDetalleService.listarPorPedido(pedidoId)));
        } catch (Exception e) {
            log.error("Error al listar detalle de ramo del pedido {}: {}", pedidoId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseGeneric<>((List<RamoPedidoDetalleResponseDto>) null, "Error al listar"));
        }
    }

    @PutMapping("/detalle/{id}/validar-frase")
    public ResponseEntity<ResponseGeneric<RamoPedidoDetalleResponseDto>> validarFrase(
            @PathVariable Integer id, @RequestBody RamoPedidoDetalleValidarFraseRequestDto request) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ramoPedidoDetalleService.validarFrase(id, request)));
        } catch (Exception e) {
            log.error("Error al validar frase del detalle {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }
}
