package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.service.PagoOnlineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Reembolso de pagos online (Checkout Pro MP / PayPal) -- paso deliberadamente separado de
// cancelar/devolver el pedido, ver el comentario largo en PagoOnlineService.
@RestController
@RequestMapping("/v1/pagos-online")
@RequiredArgsConstructor
@Slf4j
public class PagoOnlineController {

    private final PagoOnlineService pagoOnlineService;

    // Solo ADMIN (ver SecurityConfig) -- el admin ya debio verificar que el producto regreso
    // antes de disparar esto (el service exige que el pedido ya este cancelado/devuelto).
    @PostMapping("/{pedidoId}/reembolsar")
    public ResponseEntity<Map<String, String>> reembolsar(@PathVariable Integer pedidoId) {
        try {
            pagoOnlineService.reembolsarPorPedido(pedidoId);
            return ResponseEntity.ok(Map.of("mensaje", "Reembolso iniciado correctamente"));
        } catch (Exception e) {
            log.error("Error reembolsando pedido {}: {}", pedidoId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", e.getMessage()));
        }
    }
}
