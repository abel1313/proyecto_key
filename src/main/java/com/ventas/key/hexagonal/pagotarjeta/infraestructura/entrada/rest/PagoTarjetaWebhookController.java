package com.ventas.key.hexagonal.pagotarjeta.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.ConfirmarCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.pasarela.FirmaWebhookMercadoPago;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.pasarela.MercadoPagoCheckoutApi;
import com.ventas.key.mis.productos.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 *
 * <p>Aviso (webhook) de Mercado Pago cuando cambia un pago del cobro en linea. Es PUBLICO (Mercado Pago no manda
 * sesion), asi que no se confia en nada de lo que trae: se valida la firma {@code x-signature}, y el estado NO sale
 * del aviso sino de volver a preguntarle a Mercado Pago por el pago y aplicar el resultado con el mismo caso de uso
 * que usa el polling (idempotente: si llegan los dos, no se repite nada).
 *
 * <p>Siempre responde 200 a un aviso con firma valida (aunque no sea de un pago nuestro) para que Mercado Pago no
 * reintente sin fin; sin firma valida, 401 y no se hace nada.
 */
@RestController
@RequestMapping("/v1/pagos-tarjeta")
@ConditionalOnProperty(name = "pagos.tarjeta.pasarela-en-linea", havingValue = "checkout")
@Slf4j
public class PagoTarjetaWebhookController {

    private final ConfirmarCobroCasoUso confirmar;
    private final MercadoPagoCheckoutApi api;
    private final CacheService cacheService;
    private final FirmaWebhookMercadoPago firma;

    public PagoTarjetaWebhookController(ConfirmarCobroCasoUso confirmar, MercadoPagoCheckoutApi api,
                                        CacheService cacheService,
                                        @Value("${pagos.tarjeta.checkout.webhook-secret:}") String secreto) {
        this.confirmar = confirmar;
        this.api = api;
        this.cacheService = cacheService;
        this.firma = new FirmaWebhookMercadoPago(secreto);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestParam(value = "data.id", required = false) String dataIdUrl,
            @RequestParam(value = "type", required = false) String tipoUrl,
            @RequestBody(required = false) Map<String, Object> cuerpo) {

        // data.id sale de la URL (es lo que firma Mercado Pago); el cuerpo solo se usa si la URL no lo trae.
        String dataId = dataIdUrl != null ? dataIdUrl : dataIdDelCuerpo(cuerpo);
        if (!firma.esValida(xSignature, xRequestId, dataIdUrl != null ? dataIdUrl : dataId)) {
            log.warn("Webhook de pago en línea rechazado: firma inválida o sin secreto configurado");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String tipo = tipoUrl != null ? tipoUrl : (cuerpo != null && cuerpo.get("type") != null ? String.valueOf(cuerpo.get("type")) : "");
        if (!"payment".equalsIgnoreCase(tipo) || dataId == null) {
            return ResponseEntity.ok().build();                    // otro tipo de aviso: no nos toca
        }
        try {
            Optional<MercadoPagoCheckoutApi.Pago> pago = api.pago(Long.parseLong(dataId.trim()));
            Long intentoId = pago.map(MercadoPagoCheckoutApi.Pago::referencia).map(PagoTarjetaWebhookController::comoId).orElse(null);
            if (intentoId == null) {
                log.info("Webhook de pago {} sin referencia nuestra: se ignora", dataId);
                return ResponseEntity.ok().build();
            }
            var antes = confirmar.confirmar(intentoId);
            log.info("Webhook de pago {}: cobro {} queda {}", dataId, intentoId, antes.estado());
            cacheService.evictAll();
        } catch (NumberFormatException e) {
            log.warn("Webhook con data.id no numérico: se ignora");
        } catch (RuntimeException e) {
            // 500 para que Mercado Pago reintente: el cobro se confirma tambien por polling, no se pierde nada.
            log.error("No se pudo procesar el aviso del pago {}", dataId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok().build();
    }

    private static String dataIdDelCuerpo(Map<String, Object> cuerpo) {
        if (cuerpo == null || !(cuerpo.get("data") instanceof Map<?, ?> data) || data.get("id") == null) return null;
        return String.valueOf(data.get("id"));
    }

    private static Long comoId(String referencia) {
        try {
            return Long.valueOf(referencia.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
