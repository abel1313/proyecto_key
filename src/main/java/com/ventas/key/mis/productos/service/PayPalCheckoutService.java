package com.ventas.key.mis.productos.service;

import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.AmountWithBreakdown;
import com.paypal.orders.ApplicationContext;
import com.paypal.orders.LinkDescription;
import com.paypal.orders.Order;
import com.paypal.orders.OrderCaptureRequest;
import com.paypal.orders.OrderRequest;
import com.paypal.orders.OrdersCaptureRequest;
import com.paypal.orders.OrdersCreateRequest;
import com.paypal.orders.PurchaseUnitRequest;
import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.entity.PagoOnline;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.repository.IPagoOnlineRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * PayPal Orders API v2 (SDK oficial com.paypal.sdk:checkout-sdk, decidido 2026-09-03 sobre REST
 * directo) -- mismo patron y misma tabla PagoOnline que MercadoPagoCheckoutService, pero el flujo
 * de PayPal es distinto: no basta con crear la orden, hay que CAPTURARLA aparte una vez el
 * cliente aprueba en la pantalla de PayPal (no existe un webhook equivalente al de MP para esto
 * en el flujo simple sin verificar firma -- la captura la dispara el front al volver a
 * back_urls.success, ver PayPalCheckoutController).
 */
@Slf4j
@Service
public class PayPalCheckoutService {

    public static final String PROVEEDOR = "PAYPAL";

    private final IPedidoRepository iPedidoRepository;
    private final IPagoOnlineRepository iPagoOnlineRepository;
    private final PedidoServiceImpl pedidoService;

    @Value("${paypal.client-id:}")
    private String clientId;

    @Value("${paypal.client-secret:}")
    private String clientSecret;

    @Value("${paypal.sandbox:true}")
    private boolean sandbox;

    // Backend -- no se usa para notification_url (PayPal no tiene webhook obligatorio en este
    // flujo simple), se deja por si mas adelante se agrega verificacion de firma de webhook.
    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    // Frontend -- para return_url/cancel_url (adonde vuelve el navegador del cliente).
    @Value("${api.cors_angular:}")
    private String frontendBaseUrl;

    private PayPalHttpClient client;

    public PayPalCheckoutService(IPedidoRepository iPedidoRepository,
                                  IPagoOnlineRepository iPagoOnlineRepository,
                                  PedidoServiceImpl pedidoService) {
        this.iPedidoRepository = iPedidoRepository;
        this.iPagoOnlineRepository = iPagoOnlineRepository;
        this.pedidoService = pedidoService;
    }

    @PostConstruct
    public void init() {
        // clientId/clientSecret vacios (PayPal Business todavia no dado de alta, ver
        // PASARELAS_PAGO_MP_OPENPAY_PAYPAL.md seccion 7) -- se deja el cliente sin armar en vez de
        // tronar el arranque de toda la app; crearOrden() revisa esto y da un error claro.
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            log.warn("PayPal no esta configurado en este ambiente (paypal.client-id/client-secret vacios) -- Checkout de PayPal deshabilitado");
            return;
        }
        PayPalEnvironment environment = sandbox
                ? new PayPalEnvironment.Sandbox(clientId, clientSecret)
                : new PayPalEnvironment.Live(clientId, clientSecret);
        client = new PayPalHttpClient(environment);
    }

    /** Crea la orden en PayPal y devuelve la URL de aprobacion (link "approve") a la que el front debe redirigir. */
    @Transactional
    public String crearOrden(Integer pedidoId, Integer clienteIdSolicitante) {
        if (client == null) {
            throw new RuntimeException("PayPal no esta configurado en este ambiente");
        }

        Pedido pedido = iPedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + pedidoId));

        if (pedido.getCliente() == null || !pedido.getCliente().getId().equals(clienteIdSolicitante)) {
            throw new RuntimeException("Este pedido no te pertenece");
        }
        if ("Entregado".equalsIgnoreCase(pedido.getEstadoPedido())
                || "cancelado".equalsIgnoreCase(pedido.getEstadoPedido())
                || "PAGADO".equalsIgnoreCase(pedido.getEstadoPedido())) {
            throw new RuntimeException("El pedido ya esta " + pedido.getEstadoPedido() + ", no se puede volver a cobrar");
        }

        List<DetallePedido> detalles = pedido.getDetalles();
        if (detalles == null || detalles.isEmpty()) {
            throw new RuntimeException("El pedido no tiene detalles");
        }

        String base = normalizar(frontendBaseUrl);
        if (base == null) {
            throw new RuntimeException("Falta api.cors_angular en este ambiente -- no se puede armar return_url/cancel_url");
        }

        AmountWithBreakdown amount = new AmountWithBreakdown()
                .currencyCode("MXN")
                .value(String.format(Locale.US, "%.2f", pedido.getTotalPedido()));

        PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                .referenceId(String.valueOf(pedidoId))
                .customId(String.valueOf(pedidoId))
                .amountWithBreakdown(amount);

        ApplicationContext context = new ApplicationContext()
                .brandName("Novedades Jade")
                .returnUrl(base + "/pago/resultado?estado=success&pedidoId=" + pedidoId + "&proveedor=paypal")
                .cancelUrl(base + "/pago/resultado?estado=failure&pedidoId=" + pedidoId + "&proveedor=paypal")
                .userAction("PAY_NOW");

        OrderRequest orderRequest = new OrderRequest()
                .checkoutPaymentIntent("CAPTURE")
                .purchaseUnits(List.of(purchaseUnit))
                .applicationContext(context);

        try {
            OrdersCreateRequest request = new OrdersCreateRequest().requestBody(orderRequest);
            HttpResponse<Order> response = client.execute(request);
            Order order = response.result();

            PagoOnline pago = new PagoOnline();
            pago.setProveedor(PROVEEDOR);
            pago.setPedidoId(pedidoId);
            pago.setClienteId(pedido.getCliente().getId());
            pago.setReferenciaExterna(order.id());
            pago.setMonto(pedido.getTotalPedido());
            pago.setEstado("CREATED");
            pago.setFechaCreacion(LocalDateTime.now());
            iPagoOnlineRepository.save(pago);

            String approveUrl = order.links().stream()
                    .filter(l -> "approve".equals(l.rel()))
                    .findFirst()
                    .map(LinkDescription::href)
                    .orElseThrow(() -> new RuntimeException("PayPal no devolvio el link de aprobacion"));

            log.info("Orden PayPal creada: {} para pedido {}", order.id(), pedidoId);
            return approveUrl;
        } catch (IOException e) {
            log.error("Error creando orden PayPal para pedido {}: {}", pedidoId, e.getMessage(), e);
            throw new RuntimeException("No se pudo iniciar el pago con PayPal: " + e.getMessage());
        }
    }

    /**
     * Captura una orden ya aprobada por el cliente en PayPal -- lo dispara el front cuando el
     * cliente vuelve a return_url (a diferencia de MP, donde el webhook confirma solo). Idempotente
     * del lado de PedidoServiceImpl.confirmarPagoOnline si se llama mas de una vez.
     */
    @Transactional
    public void capturarOrden(String orderId, Integer clienteIdSolicitante) {
        if (client == null) {
            throw new RuntimeException("PayPal no esta configurado en este ambiente");
        }

        PagoOnline pago = iPagoOnlineRepository.findByProveedorAndReferenciaExterna(PROVEEDOR, orderId)
                .orElseThrow(() -> new RuntimeException("No se encontro un pago PayPal con orden " + orderId));
        if (!pago.getClienteId().equals(clienteIdSolicitante)) {
            throw new RuntimeException("Esta orden no te pertenece");
        }
        if ("APPROVED".equals(pago.getEstado())) {
            log.info("Orden PayPal {} ya estaba capturada, no se vuelve a capturar", orderId);
            return;
        }

        try {
            OrdersCaptureRequest request = new OrdersCaptureRequest(orderId).requestBody(new OrderCaptureRequest());
            HttpResponse<Order> response = client.execute(request);
            Order order = response.result();

            String captureId = order.purchaseUnits() != null && !order.purchaseUnits().isEmpty()
                    && order.purchaseUnits().get(0).payments() != null
                    && order.purchaseUnits().get(0).payments().captures() != null
                    && !order.purchaseUnits().get(0).payments().captures().isEmpty()
                    ? order.purchaseUnits().get(0).payments().captures().get(0).id()
                    : null;

            String estadoNuevo = normalizarEstado(order.status());
            pago.setPagoIdExterno(captureId);
            pago.setEstado(estadoNuevo);
            pago.setFechaUpdate(LocalDateTime.now());
            iPagoOnlineRepository.save(pago);

            log.info("PagoOnline {} (pedido {}) actualizado a {} por captura PayPal {}",
                    pago.getId(), pago.getPedidoId(), estadoNuevo, captureId);

            if ("APPROVED".equals(estadoNuevo)) {
                pedidoService.confirmarPagoOnline(pago.getPedidoId());
            }
        } catch (IOException e) {
            log.error("Error capturando orden PayPal {}: {}", orderId, e.getMessage(), e);
            pago.setEstado("REJECTED");
            pago.setFechaUpdate(LocalDateTime.now());
            iPagoOnlineRepository.save(pago);
            throw new RuntimeException("No se pudo capturar el pago con PayPal: " + e.getMessage());
        }
    }

    /**
     * Reembolso TOTAL de una captura ya completada (2026-09-03) -- lo dispara PagoOnlineService,
     * que ya valida que la devolución del pedido se haya registrado primero. Usa la API de
     * Payments (com.paypal.payments), no la de Orders -- PayPal expone el reembolso ahí, sobre el
     * capture id, no sobre el order id. RefundRequest vacío (sin amount) = reembolso total.
     */
    public void reembolsar(String captureId) {
        if (client == null) {
            throw new RuntimeException("PayPal no esta configurado en este ambiente");
        }
        try {
            com.paypal.payments.CapturesRefundRequest request =
                    new com.paypal.payments.CapturesRefundRequest(captureId)
                            .requestBody(new com.paypal.payments.RefundRequest());
            client.execute(request);
            log.info("Reembolso PayPal disparado para capture {}", captureId);
        } catch (IOException e) {
            log.error("Error reembolsando capture PayPal {}: {}", captureId, e.getMessage(), e);
            throw new RuntimeException("No se pudo reembolsar en PayPal: " + e.getMessage());
        }
    }

    // Vocabulario de estado de PayPal (Orders API): CREATED, SAVED, APPROVED (aprobada por el
    // cliente pero SIN capturar todavia), VOIDED, COMPLETED (capturada con exito),
    // PAYER_ACTION_REQUIRED. Distinto del vocabulario de MP (approved/rejected/...) -- NO
    // compartir este metodo con MercadoPagoCheckoutService.normalizarEstado.
    private String normalizarEstado(String estadoPaypal) {
        if (estadoPaypal == null) return "PENDING";
        return switch (estadoPaypal) {
            case "COMPLETED" -> "APPROVED";
            case "VOIDED" -> "CANCELLED";
            default -> "PENDING"; // CREATED, SAVED, APPROVED (sin capturar), PAYER_ACTION_REQUIRED
        };
    }

    private String normalizar(String url) {
        if (url == null || url.isBlank()) return null;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
