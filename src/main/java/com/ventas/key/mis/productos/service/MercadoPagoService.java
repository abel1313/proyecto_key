package com.ventas.key.mis.productos.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.point.PointClient;
import com.mercadopago.client.point.PointPaymentIntentListRequest;
import com.mercadopago.client.point.PointPaymentIntentRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.point.PointPaymentIntent;
import com.mercadopago.resources.point.PointPaymentIntentList;
import com.mercadopago.resources.point.PointPaymentIntentListEvent;
import com.mercadopago.resources.point.PointStatusPaymentIntent;
import com.ventas.key.mis.productos.entity.MpPaymentIntent;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.models.PagoMPRequest;
import com.ventas.key.mis.productos.models.pedidos.ClienteQuery;
import com.ventas.key.mis.productos.models.pedidos.DetalleQuery;
import com.ventas.key.mis.productos.models.pedidos.PedidoGenerico;
import com.ventas.key.mis.productos.models.pedidos.PedidoQuery;
import com.ventas.key.mis.productos.repository.IMpPaymentIntentRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MercadoPagoService {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.device-id}")
    private String deviceId;

    @Value("${mercadopago.sandbox:false}")
    private boolean sandbox;

    private final PedidoServiceImpl pedidoService;
    private final IPedidoRepository iPedidoRepository;
    private final IMpPaymentIntentRepository intentRepository;

    public MercadoPagoService(PedidoServiceImpl pedidoService,
                               IPedidoRepository iPedidoRepository,
                               IMpPaymentIntentRepository intentRepository) {
        this.pedidoService = pedidoService;
        this.iPedidoRepository = iPedidoRepository;
        this.intentRepository = intentRepository;
    }

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    private MPRequestOptions requestOptions() {
        if (sandbox) {
            return MPRequestOptions.builder()
                    .customHeaders(Map.of("X-Test-Scope", "sandbox"))
                    .build();
        }
        return MPRequestOptions.createDefault();
    }

    public String iniciarPago(PagoMPRequest request) throws MPException, MPApiException {
        log.info("Iniciando pago - pedidoId: {} monto: {} cuotas: {}", request.getPedidoId(), request.getTotalMonto(), request.getCuotas());

        BigDecimal montoCentavos = BigDecimal.valueOf(request.getTotalMonto())
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, java.math.RoundingMode.HALF_UP);

        PointPaymentIntentRequest mpRequest = PointPaymentIntentRequest.builder()
                .amount(montoCentavos)
                .build();

        PointPaymentIntent intent = new PointClient()
                .createPaymentIntent(deviceId, mpRequest, requestOptions());

        String intentId = intent.getId();

        MpPaymentIntent registro = new MpPaymentIntent();
        registro.setIntentId(intentId);
        registro.setPedidoId(request.getPedidoId());
        registro.setClienteId(request.getClienteId());
        registro.setMonto(request.getTotalMonto());
        registro.setCuotas(request.getCuotas());
        registro.setEstado("OPEN");
        registro.setFechaCreacion(LocalDateTime.now());
        intentRepository.save(registro);

        log.info("Payment intent creado: {} pedido: {} monto: {}", intentId, request.getPedidoId(), request.getTotalMonto());
        return intentId;
    }

    public String consultarEstado(String intentId) throws MPException, MPApiException {
        MpPaymentIntent registro = intentRepository.findByIntentId(intentId).orElse(null);
        if (registro != null && "FINISHED".equals(registro.getEstado())) {
            return "FINISHED";
        }
        PointStatusPaymentIntent status = new PointClient()
                .getPaymentIntentStatus(intentId, requestOptions());
        String estado = status.getStatus() != null ? status.getStatus() : "UNKNOWN";

        if (registro != null && !estado.equals(registro.getEstado())) {
            actualizarEstado(registro, estado);
        }
        return estado;
    }

    public void procesarWebhook(String intentId) {
        MpPaymentIntent registro = intentRepository.findByIntentId(intentId).orElse(null);
        if (registro == null) {
            log.warn("Webhook recibido para intent desconocido: {}", intentId);
            return;
        }
        try {
            PointStatusPaymentIntent status = new PointClient()
                    .getPaymentIntentStatus(intentId, requestOptions());
            String estado = status.getStatus() != null ? status.getStatus() : "";

            actualizarEstado(registro, estado);

            if ("FINISHED".equalsIgnoreCase(estado)) {
                PagoMPRequest request = new PagoMPRequest(
                        registro.getPedidoId(), registro.getClienteId(),
                        null, registro.getCuotas(), registro.getMonto(), null);
                PedidoGenerico pg = buildPedidoGenerico(request);
                pedidoService.updatePedido(registro.getPedidoId(), pg);
                log.info("Pedido {} confirmado por webhook, intent: {}", registro.getPedidoId(), intentId);
            }
        } catch (Exception e) {
            actualizarEstado(registro, "ERROR");
            log.error("Error procesando webhook para intent {}: {}", intentId, e.getMessage());
        }
    }

    public void cancelar(String intentId) throws MPException, MPApiException {
        new PointClient().cancelPaymentIntent(deviceId, intentId, requestOptions());
        intentRepository.findByIntentId(intentId).ifPresent(r -> actualizarEstado(r, "CANCELED"));
        log.info("Payment intent cancelado: {}", intentId);
    }

    public Page<MpPaymentIntent> listarIntentsDB(int pagina, int size) {
        return intentRepository.findAll(PageRequest.of(pagina - 1, size));
    }

    public Page<MpPaymentIntent> listarIntentsPorPedido(Integer pedidoId, int pagina, int size) {
        return intentRepository.findByPedidoId(pedidoId, PageRequest.of(pagina - 1, size));
    }

    public Page<MpPaymentIntent> listarIntentsPorEstado(String estado, int pagina, int size) {
        return intentRepository.findByEstado(estado, PageRequest.of(pagina - 1, size));
    }

    public List<PointPaymentIntentListEvent> consultarIntentsMP(LocalDate desde, LocalDate hasta) throws MPException, MPApiException {
        PointPaymentIntentListRequest listRequest = PointPaymentIntentListRequest.builder()
                .startDate(desde)
                .endDate(hasta)
                .build();
        PointPaymentIntentList lista = new PointClient().getPaymentIntentList(listRequest, requestOptions());
        return lista.getEvents();
    }

    private void actualizarEstado(MpPaymentIntent registro, String nuevoEstado) {
        registro.setEstado(nuevoEstado);
        registro.setFechaUpdate(LocalDateTime.now());
        intentRepository.save(registro);
    }

    private PedidoGenerico buildPedidoGenerico(PagoMPRequest request) {
        Pedido pedido = iPedidoRepository.findById(request.getPedidoId())
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + request.getPedidoId()));

        ClienteQuery clienteQuery = new ClienteQuery();
        clienteQuery.setId(request.getClienteId());

        List<DetalleQuery> detalles = pedido.getDetalles().stream().map(d -> {
            DetalleQuery dq = new DetalleQuery();
            dq.setProducto(d.getProducto().getId().longValue());
            dq.setCantidad(d.getCantidad());
            dq.setPrecio_unitario(d.getPrecioUnitario());
            dq.setSub_total(d.getSubTotal());
            return dq;
        }).toList();

        PedidoQuery pedidoQuery = new PedidoQuery();
        pedidoQuery.setId(request.getPedidoId());
        pedidoQuery.setDetalles(detalles);

        PedidoGenerico pg = new PedidoGenerico();
        pg.setCliente(clienteQuery);
        pg.setPedido(pedidoQuery);
        pg.setPagosYMesesId(request.getPagosYMesesId());
        return pg;
    }
}