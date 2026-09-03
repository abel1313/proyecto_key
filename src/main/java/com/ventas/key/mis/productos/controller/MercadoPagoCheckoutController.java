package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.service.MercadoPagoCheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Checkout Pro (pago online por redireccion) -- NO confundir con MercadoPagoController, que es
// Point (terminal fisica). Ver el comentario de MercadoPagoCheckoutService para el porque estan
// separados aunque compartan la misma cuenta/access-token de Mercado Pago.
@RestController
@RequestMapping("/v1/mp/checkout")
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoCheckoutController {

    private final MercadoPagoCheckoutService checkoutService;

    // El propio cliente dueno del pedido dispara esto desde el checkout -- ver SecurityConfig,
    // tiene su propio matcher ANTES del hasRole("ADMIN") generico de /v1/mp/**.
    @PostMapping("/preference/{pedidoId}")
    public ResponseEntity<Map<String, String>> crearPreference(@PathVariable Integer pedidoId) {
        try {
            Usuario actual = AuthenticationUtils.currentUsuario();
            if (actual.getCliente() == null || actual.getCliente().getId() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("mensaje", "No tienes datos de cliente asociados"));
            }
            String initPoint = checkoutService.crearPreference(pedidoId, actual.getCliente().getId());
            return ResponseEntity.ok(Map.of("initPoint", initPoint));
        } catch (Exception e) {
            log.error("Error creando preference para pedido {}: {}", pedidoId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", e.getMessage()));
        }
    }

    // Publico a proposito -- lo llama Mercado Pago desde afuera, sin ningun header de auth
    // nuestro (ver SecurityConfig, permitAll explicito antes del catch-all de /v1/mp/**).
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody Map<String, Object> payload) {
        log.info("Webhook Checkout Pro MP recibido: {}", payload);
        try {
            String type = (String) payload.get("type");
            if ("payment".equals(type)) {
                Map<?, ?> data = (Map<?, ?>) payload.get("data");
                if (data != null && data.get("id") != null) {
                    Long paymentId = Long.valueOf(String.valueOf(data.get("id")));
                    checkoutService.confirmarPago(paymentId);
                }
            }
        } catch (Exception e) {
            log.error("Error procesando webhook de Checkout Pro: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok().build();
    }
}
