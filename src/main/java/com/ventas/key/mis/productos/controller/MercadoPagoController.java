package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.MpPaymentIntent;
import com.ventas.key.mis.productos.models.PagoMPRequest;
import com.ventas.key.mis.productos.service.MercadoPagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mp")
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoController {

    private final MercadoPagoService mercadoPagoService;

    @PostMapping("/iniciar")
    public ResponseEntity<Map<String, String>> iniciarPago(@RequestBody PagoMPRequest request) {
        try {
            String intentId = mercadoPagoService.iniciarPago(request);
            return ResponseEntity.ok(Map.of(
                    "intentId", intentId,
                    "estado", "OPEN",
                    "mensaje", "Esperando pago en terminal"
            ));
        } catch (Exception e) {
            log.error("Error al crear payment intent: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "estado", "ERROR",
                    "mensaje", e.getMessage()
            ));
        }
    }

    @GetMapping("/estado/{intentId}")
    public ResponseEntity<Map<String, Object>> consultarEstado(@PathVariable String intentId) {
        try {
            String estado = mercadoPagoService.consultarEstado(intentId);
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("intentId", intentId);
            respuesta.put("estado", estado);
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            log.error("Error consultando estado de intent {}: {}", intentId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "estado", "ERROR",
                    "mensaje", e.getMessage()
            ));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody Map<String, Object> payload) {
        log.info("Webhook MP recibido: {}", payload);
        try {
            String type = (String) payload.get("type");
            if ("point_integration_ipn".equals(type)) {
                Map<?, ?> data = (Map<?, ?>) payload.get("data");
                if (data != null) {
                    String intentId = (String) data.get("id");
                    mercadoPagoService.procesarWebhook(intentId);
                }
            }
        } catch (Exception e) {
            log.error("Error procesando webhook: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/cancelar/{intentId}")
    public ResponseEntity<Map<String, String>> cancelar(@PathVariable String intentId) {
        try {
            mercadoPagoService.cancelar(intentId);
            return ResponseEntity.ok(Map.of("mensaje", "Pago cancelado"));
        } catch (Exception e) {
            log.error("Error cancelando intent {}: {}", intentId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("mensaje", e.getMessage()));
        }
    }

    // ── Consultas de historial ──────────────────────────────────────────────

    @GetMapping("/historial")
    public ResponseEntity<?> historial(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(mercadoPagoService.listarIntentsDB(pagina, size));
    }

    @GetMapping("/historial/pedido/{pedidoId}")
    public ResponseEntity<?> historialPorPedido(
            @PathVariable Integer pedidoId,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(mercadoPagoService.listarIntentsPorPedido(pedidoId, pagina, size));
    }

    @GetMapping("/historial/estado/{estado}")
    public ResponseEntity<?> historialPorEstado(
            @PathVariable String estado,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(mercadoPagoService.listarIntentsPorEstado(estado, pagina, size));
    }

    @GetMapping("/historial/mp")
    public ResponseEntity<?> historialMP(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            return ResponseEntity.ok(mercadoPagoService.consultarIntentsMP(desde, hasta));
        } catch (Exception e) {
            log.error("Error consultando intents en MP: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("mensaje", e.getMessage()));
        }
    }
}