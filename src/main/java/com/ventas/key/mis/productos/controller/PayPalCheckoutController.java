package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.service.PayPalCheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/paypal")
@RequiredArgsConstructor
@Slf4j
public class PayPalCheckoutController {

    private final PayPalCheckoutService checkoutService;

    @PostMapping("/orden/{pedidoId}")
    public ResponseEntity<Map<String, String>> crearOrden(@PathVariable Integer pedidoId) {
        try {
            Integer clienteId = clienteIdActual();
            if (clienteId == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("mensaje", "No tienes datos de cliente asociados"));
            }
            String approveUrl = checkoutService.crearOrden(pedidoId, clienteId);
            return ResponseEntity.ok(Map.of("approveUrl", approveUrl));
        } catch (Exception e) {
            log.error("Error creando orden PayPal para pedido {}: {}", pedidoId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", e.getMessage()));
        }
    }

    // El front lo llama cuando el cliente vuelve a la pantalla de resultado con estado=success
    // (ver PayPalCheckoutService: PayPal no confirma solo por webhook en este flujo simple).
    @PostMapping("/orden/{orderId}/capturar")
    public ResponseEntity<Map<String, String>> capturarOrden(@PathVariable String orderId) {
        try {
            Integer clienteId = clienteIdActual();
            if (clienteId == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("mensaje", "No tienes datos de cliente asociados"));
            }
            checkoutService.capturarOrden(orderId, clienteId);
            return ResponseEntity.ok(Map.of("mensaje", "Pago confirmado"));
        } catch (Exception e) {
            log.error("Error capturando orden PayPal {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", e.getMessage()));
        }
    }

    private Integer clienteIdActual() {
        Usuario actual = AuthenticationUtils.currentUsuario();
        return actual.getCliente() != null ? actual.getCliente().getId() : null;
    }
}
