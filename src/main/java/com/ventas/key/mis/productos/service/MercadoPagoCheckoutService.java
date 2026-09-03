package com.ventas.key.mis.productos.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.entity.PagoOnline;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.repository.IPagoOnlineRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Checkout Pro de Mercado Pago (pago online por redireccion) -- NO confundir con
 * MercadoPagoService, que es Point (terminal fisica). Comparten access-token/cuenta pero son dos
 * flujos de API completamente distintos del mismo SDK, cada uno con su propio seguimiento en BD:
 * Point usa mp_payment_intent (MpPaymentIntent), Checkout Pro usa pago_online (PagoOnline).
 *
 * Estado actual (2026-09-03): crea la Preference, guarda el seguimiento en PagoOnline, y
 * actualiza su estado cuando llega el webhook o se consulta manualmente. Todavia NO marca el
 * Pedido como pagado ni genera la Venta -- ver el comentario en confirmarPago() para el porque.
 */
@Slf4j
@Service
public class MercadoPagoCheckoutService {

    public static final String PROVEEDOR = "MP_CHECKOUT";

    private final IPedidoRepository iPedidoRepository;
    private final IPagoOnlineRepository iPagoOnlineRepository;
    private final PedidoServiceImpl pedidoService;

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.sandbox:false}")
    private boolean sandbox;

    // Backend -- para notification_url (MP tiene que poder llamarnos desde afuera).
    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    // Frontend -- para back_urls (adonde vuelve el navegador del cliente). Ver el bug real
    // 2026-09-03 en EmailService: NO usar publicBaseUrl para links que abre el navegador.
    @Value("${api.cors_angular:}")
    private String frontendBaseUrl;

    public MercadoPagoCheckoutService(IPedidoRepository iPedidoRepository,
                                       IPagoOnlineRepository iPagoOnlineRepository,
                                       PedidoServiceImpl pedidoService) {
        this.iPedidoRepository = iPedidoRepository;
        this.iPagoOnlineRepository = iPagoOnlineRepository;
        this.pedidoService = pedidoService;
    }

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    private MPRequestOptions requestOptions() {
        if (sandbox) {
            return MPRequestOptions.builder().customHeaders(Map.of("X-Test-Scope", "sandbox")).build();
        }
        return MPRequestOptions.createDefault();
    }

    /**
     * Crea la Preference en Mercado Pago para el pedido y devuelve la URL a la que el front debe
     * redirigir (window.location.href) -- init_point en produccion, sandbox_init_point si
     * mercadopago.sandbox=true (los links de produccion no dejan pagar con tarjetas de prueba).
     */
    @Transactional
    public String crearPreference(Integer pedidoId, Integer clienteIdSolicitante) {
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

        List<PreferenceItemRequest> items = detalles.stream().map(d -> PreferenceItemRequest.builder()
                .title(d.getProducto() != null ? d.getProducto().getNombre() : "Producto")
                .quantity(d.getCantidad())
                .unitPrice(BigDecimal.valueOf(d.getPrecioUnitario()))
                .currencyId("MXN")
                .build()).toList();

        String base = normalizar(frontendBaseUrl);
        String backendBase = normalizar(publicBaseUrl);
        if (base == null || backendBase == null) {
            throw new RuntimeException("Faltan api.cors_angular o app.public-base-url en este ambiente -- "
                    + "no se puede armar back_urls/notification_url para Checkout Pro");
        }

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(base + "/pago/resultado?estado=success&pedidoId=" + pedidoId)
                .pending(base + "/pago/resultado?estado=pending&pedidoId=" + pedidoId)
                .failure(base + "/pago/resultado?estado=failure&pedidoId=" + pedidoId)
                .build();

        PreferenceRequest request = PreferenceRequest.builder()
                .items(items)
                .backUrls(backUrls)
                .externalReference(String.valueOf(pedidoId))
                .notificationUrl(backendBase + "/v1/mp/checkout/webhook")
                .build();

        try {
            Preference preference = new PreferenceClient().create(request, requestOptions());

            PagoOnline pago = new PagoOnline();
            pago.setProveedor(PROVEEDOR);
            pago.setPedidoId(pedidoId);
            pago.setClienteId(pedido.getCliente().getId());
            pago.setReferenciaExterna(preference.getId());
            pago.setMonto(pedido.getTotalPedido());
            pago.setEstado("CREATED");
            pago.setFechaCreacion(LocalDateTime.now());
            iPagoOnlineRepository.save(pago);

            log.info("Preference MP creada: {} para pedido {}", preference.getId(), pedidoId);
            return sandbox ? preference.getSandboxInitPoint() : preference.getInitPoint();
        } catch (MPApiException | MPException e) {
            log.error("Error creando preference MP para pedido {}: {}", pedidoId, e.getMessage(), e);
            throw new RuntimeException("No se pudo iniciar el pago: " + e.getMessage());
        }
    }

    /**
     * Confirma un pago consultando DIRECTO a la API de MP con el payment.id (nunca confiar en lo
     * que dice el webhook o la URL de vuelta a ciegas), actualiza el PagoOnline correspondiente
     * y, si quedo aprobado, dispara PedidoServiceImpl.confirmarPagoOnline (marca el pedido como
     * PAGADO y genera la Venta bajo el catalogo "TARJETA", decidido 2026-09-03).
     */
    @Transactional
    public void confirmarPago(Long paymentId) {
        try {
            Payment payment = new PaymentClient().get(paymentId, requestOptions());
            String externalReference = payment.getExternalReference();
            if (externalReference == null) {
                log.warn("Payment MP {} sin external_reference, no se puede ligar a un pedido", paymentId);
                return;
            }

            PagoOnline pago = iPagoOnlineRepository
                    .findFirstByProveedorAndPedidoIdOrderByFechaCreacionDesc(PROVEEDOR, Integer.valueOf(externalReference))
                    .orElse(null);
            if (pago == null) {
                log.warn("No se encontro PagoOnline para pedido {} (payment MP {})", externalReference, paymentId);
                return;
            }

            pago.setPagoIdExterno(String.valueOf(payment.getId()));
            String estadoNuevo = normalizarEstado(payment.getStatus());
            pago.setEstado(estadoNuevo);
            pago.setFechaUpdate(LocalDateTime.now());
            iPagoOnlineRepository.save(pago);

            log.info("PagoOnline {} (pedido {}) actualizado a {} por payment MP {}",
                    pago.getId(), pago.getPedidoId(), pago.getEstado(), paymentId);

            if ("APPROVED".equals(estadoNuevo)) {
                pedidoService.confirmarPagoOnline(pago.getPedidoId());
            }
        } catch (MPApiException | MPException e) {
            log.error("Error consultando payment MP {}: {}", paymentId, e.getMessage(), e);
        }
    }

    /**
     * Reembolso TOTAL de un pago ya aprobado (2026-09-03) -- lo dispara PagoOnlineService, que ya
     * valida que la devolución del pedido se haya registrado primero (ver el comentario largo
     * ahí sobre por qué el reembolso es un paso aparte, deliberado, y no algo automático al
     * cancelar). Sin `amount` en el request = reembolso total.
     */
    public void reembolsar(String paymentIdStr) {
        try {
            new PaymentClient().refund(Long.valueOf(paymentIdStr), requestOptions());
            log.info("Reembolso MP disparado para payment {}", paymentIdStr);
        } catch (MPApiException | MPException e) {
            log.error("Error reembolsando payment MP {}: {}", paymentIdStr, e.getMessage(), e);
            throw new RuntimeException("No se pudo reembolsar en Mercado Pago: " + e.getMessage());
        }
    }

    private String normalizarEstado(String estadoMp) {
        if (estadoMp == null) return "PENDING";
        return switch (estadoMp) {
            case "approved" -> "APPROVED";
            case "rejected" -> "REJECTED";
            case "cancelled" -> "CANCELLED";
            case "refunded", "charged_back" -> "REFUNDED";
            default -> "PENDING"; // pending, in_process, in_mediation, authorized
        };
    }

    private String normalizar(String url) {
        if (url == null || url.isBlank()) return null;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
