package com.ventas.key.mis.productos.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ventas.key.mis.productos.dto.ClienteSinRegistroDto;
import com.ventas.key.mis.productos.entity.*;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.models.NotificacionRequest;
import com.ventas.key.mis.productos.models.VentaDirectaResponse;
import com.ventas.key.mis.productos.repository.*;
import com.ventas.key.mis.productos.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.TotalDetalle;
import com.ventas.key.mis.productos.models.VentaDirectaRequest;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class VentaServiceImpl extends CrudAbstractServiceImpl<Venta, List<Venta>, Optional<Venta>, Integer, PginaDto<List<Venta>>> {

    @PersistenceContext
    private EntityManager entityManager;

    private final IVentaRepository iVentaRepository;
    private final IProductosRepository iRepository;
    private final IUsuarioRepository iUsuarioRepository;
    private final IClienteRepository iClienteRepository;
    private final IPagosYMesesRepository iPagosYMesesRepository;
    private final IDetallePagoRepository iDetallePagoRepository;
    private final IVarianteRepository iVarianteRepository;
    private final IClienteSinRegistroRepository iClienteSinRegistroRepository;
    private final IPedidoRepository iPedidoRepository;
    private final ErrorGenerico errorGenerico;

    @Autowired private CacheService cacheService;
    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private EmailService emailService;
    @Autowired private WhatsappService whatsappService;

    public VentaServiceImpl(
            final IVentaRepository iVentaRepository,
            final IProductosRepository iRepository,
            final IUsuarioRepository iUsuarioRepository,
            final IClienteRepository iClienteRepository,
            final IPagosYMesesRepository iPagosYMesesRepository,
            final IDetallePagoRepository iDetallePagoRepository,
            final IVarianteRepository iVarianteRepository,
            final IClienteSinRegistroRepository iClienteSinRegistroRepository,
            final IPedidoRepository iPedidoRepository,
            final ErrorGenerico errorGenerico) {
        super(iVentaRepository, errorGenerico);
        this.iVentaRepository = iVentaRepository;
        this.iRepository = iRepository;
        this.iUsuarioRepository = iUsuarioRepository;
        this.iClienteRepository = iClienteRepository;
        this.iPagosYMesesRepository = iPagosYMesesRepository;
        this.iDetallePagoRepository = iDetallePagoRepository;
        this.iVarianteRepository = iVarianteRepository;
        this.errorGenerico = errorGenerico;
        this.iClienteSinRegistroRepository = iClienteSinRegistroRepository;
        this.iPedidoRepository = iPedidoRepository;
    }

    private ClienteSinRegistro mapperClienteSinRegistroDto(VentaDirectaRequest request) {
        ClienteSinRegistro clienteSinRegistro = new ClienteSinRegistro();
        ClienteSinRegistroDto dto = request.getClienteSinRegistroDto();
        clienteSinRegistro.setNombrePersona(dto.getNombre_persona());
        clienteSinRegistro.setSegundoNombre(dto.getSegundo_nombre());
        clienteSinRegistro.setApeidoMaterno(dto.getApeido_Materno());
        clienteSinRegistro.setApeidoPaterno(dto.getApeido_Paterno());
        clienteSinRegistro.setSexo(dto.getSexo());
        clienteSinRegistro.setCorreoElectronico(dto.getCorreo_Electronico());
        LocalDate fechaValida = dto.getFecha_Nacimiento().isBlank() ? null : LocalDate.parse(dto.getFecha_Nacimiento());
        clienteSinRegistro.setFechaNacimiento(fechaValida);
        clienteSinRegistro.setNumeroTelefonico(dto.getNumero_Telefonico());
        return clienteSinRegistro;
    }

    @Transactional
    public VentaDirectaResponse saveVentaDetalle(VentaDirectaRequest request) throws ExceptionErrorInesperado, ExceptionDataNotFound {

        Usuario usuario = iUsuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new ExceptionDataNotFound("Usuario no encontrado"));

        String tipoPedido = request.getTipoPedido();
        boolean esCredito = "APARTADO".equals(tipoPedido) || "FIADO".equals(tipoPedido);

        PagosYMeses pagosYMeses = null;
        MesesIntereses mesesIntereses = null;
        double tasaTarifa = 0.0;
        double tasaIva = 0.0;
        DetallePago detallePago = null;

        if (!esCredito) {
            pagosYMeses = iPagosYMesesRepository.findById(request.getPagosYMesesId())
                    .orElseThrow(() -> new ExceptionErrorInesperado("Opción de pago no válida"));
            mesesIntereses = pagosYMeses.getMesesIntereses();
            tasaTarifa = mesesIntereses.getTarifaTerminal() != null
                    ? mesesIntereses.getTarifaTerminal().getTarifa() / 100.0 : 0.0;
            tasaIva = mesesIntereses.getIvaTerminal() != null
                    ? mesesIntereses.getIvaTerminal().getIva() / 100.0 : 0.0;
            if (mesesIntereses.getTarifaTerminal() != null && mesesIntereses.getIvaTerminal() != null) {
                DetallePagoId detallePagoId = new DetallePagoId(
                        pagosYMeses.getTipoPago().getId(),
                        mesesIntereses.getTarifaTerminal().getId(),
                        mesesIntereses.getIvaTerminal().getId()
                );
                detallePago = iDetallePagoRepository.findById(detallePagoId).orElse(null);
            }
        }

        // Determinar si es cliente registrado o sin registro
        Cliente cliente = null;
        ClienteSinRegistro clienteSinRegistro = null;

        boolean esSinRegistro = request.getClienteSinRegistroDto() != null
                && !request.getClienteSinRegistroDto().getNombre_persona().isBlank();

        if (esSinRegistro) {
            clienteSinRegistro = iClienteSinRegistroRepository.save(mapperClienteSinRegistroDto(request));
        } else {
            cliente = iClienteRepository.findById(request.getClienteId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Cliente no encontrado"));
        }

        // Procesar items y construir las dos listas de detalle
        List<DetallePedido> detallesPedido = new ArrayList<>();
        List<DetalleVentaVariante> detallesVenta = new ArrayList<>();

        for (var item : request.getDetalles()) {
            Variantes variante = iVarianteRepository.findByIdWithLock(item.getVarianteId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Variante no encontrada: " + item.getVarianteId()));

            Producto prod = variante.getProducto();

            if (variante.getStock() < item.getCantidad()) {
                throw new RuntimeException("Stock insuficiente en variante id " + item.getVarianteId()
                        + ". Disponible: " + variante.getStock() + ", solicitado: " + item.getCantidad());
            }
            variante.setStock(variante.getStock() - item.getCantidad());
            iVarianteRepository.save(variante);

            if (prod.getStock() < item.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para: " + prod.getNombre()
                        + ". Disponible: " + prod.getStock() + ", solicitado: " + item.getCantidad());
            }
            prod.setStock(prod.getStock() - item.getCantidad());
            iRepository.save(prod);

            double precioCosto = prod.getPrecioCosto();
            double subTotal    = item.getSubTotal();
            double costoTotal  = precioCosto * item.getCantidad();
            double comision    = subTotal * (tasaTarifa + tasaIva);
            double ganancia    = subTotal - costoTotal - comision;

            DetallePedido dp = new DetallePedido();
            dp.setProducto(prod);
            dp.setVariante(variante);
            dp.setCantidad(item.getCantidad());
            dp.setPrecioUnitario(item.getPrecioVenta());
            dp.setSubTotal(subTotal);
            detallesPedido.add(dp);

            DetalleVentaVariante dvv = new DetalleVentaVariante();
            dvv.setVariante(variante);
            dvv.setCantidad(item.getCantidad());
            dvv.setPrecioUnitario(item.getPrecioVenta());
            dvv.setSubTotal(subTotal);
            dvv.setPrecioCosto(precioCosto);
            dvv.setGanancia(ganancia);
            dvv.setFechaVenta(LocalDate.now());
            detallesVenta.add(dvv);
        }

        double totalPedidoCalc = detallesPedido.stream().mapToDouble(DetallePedido::getSubTotal).sum();

        if (esCredito) {
            Pedido pedido = new Pedido();
            pedido.setTipoPedido(tipoPedido);
            pedido.setEstadoPedido(tipoPedido);
            pedido.setCliente(cliente);
            pedido.setClienteSinRegistro(clienteSinRegistro);
            pedido.setObservaciones(request.getObservaciones() != null ? request.getObservaciones() : "");
            pedido.setFechaPedido(LocalDate.now());
            pedido.setTotalPedido(totalPedidoCalc);
            pedido.setTotalPagado(0.0);
            detallesPedido.forEach(dp -> dp.setPedido(pedido));
            pedido.setDetalles(detallesPedido);
            Pedido savedPedido = iPedidoRepository.save(pedido);
            cacheService.evictAll();
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
            VentaDirectaResponse respCredito = new VentaDirectaResponse(
                    null, null, false, totalPedidoCalc, null, null, null, savedPedido.getId(),
                    null, null, null);
            enviarNotificacionesVenta(request.getNotificacion(), cliente, clienteSinRegistro,
                    "Pedido registrado — Novedades Jade", respCredito);
            return respCredito;
        }

        double totalVenta    = detallesVenta.stream().mapToDouble(DetalleVentaVariante::getSubTotal).sum();
        double gananciaTotal = detallesVenta.stream().mapToDouble(DetalleVentaVariante::getGanancia).sum();

        // Crear y guardar Pedido (siempre, para todos los escenarios)
        Pedido pedido = new Pedido();
        pedido.setEstadoPedido("Entregado");
        pedido.setCliente(cliente);
        pedido.setClienteSinRegistro(clienteSinRegistro);
        pedido.setObservaciones("");
        pedido.setFechaPedido(LocalDate.now());
        pedido.setFechaRecogida(LocalDate.now());
        detallesPedido.forEach(dp -> dp.setPedido(pedido));
        pedido.setDetalles(detallesPedido);
        iPedidoRepository.save(pedido);

        // Crear Venta vinculada al Pedido
        Venta venta = new Venta();
        venta.setEstadoVenta("Entregado");
        venta.setFechaVenta(LocalDateTime.now());
        venta.setUsuario(usuario);
        venta.setCliente(cliente);
        venta.setClienteSinRegistro(clienteSinRegistro);
        venta.setPagosYMeses(pagosYMeses);
        venta.setDetallePago(detallePago);
        venta.setPedido(pedido);
        venta.setTotalVenta(totalVenta);
        venta.setGananciaTotal(gananciaTotal);
        detallesVenta.forEach(dvv -> dvv.setVenta(venta));
        venta.setDetalles(detallesVenta);

        boolean requiereTerminal = mesesIntereses.getTarifaTerminal() != null && mesesIntereses.getTarifaTerminal().getId() != 3;
        Venta saved = iVentaRepository.save(venta);
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        VentaDirectaResponse respVenta = new VentaDirectaResponse(
                saved.getId(),
                pagosYMeses.getTipoPago().getFormaPago(),
                requiereTerminal,
                totalVenta,
                requiereTerminal ? mesesIntereses.getMeses() : null,
                mesesIntereses.getDescripcion(),
                null,
                null,
                null, null, null
        );
        enviarNotificacionesVenta(request.getNotificacion(), cliente, clienteSinRegistro,
                "Comprobante de compra — Novedades Jade", respVenta);
        return respVenta;
    }

    private void enviarNotificacionesVenta(NotificacionRequest notif, Cliente cliente,
                                            ClienteSinRegistro sinRegistro,
                                            String asunto, VentaDirectaResponse resp) {
        if (notif == null) return;
        List<String> errores = new ArrayList<>();
        String correo   = cliente != null ? cliente.getCorreoElectronico()
                        : sinRegistro != null ? sinRegistro.getCorreoElectronico() : "";
        String telefono = cliente != null ? cliente.getNumeroTelefonico()
                        : sinRegistro != null ? sinRegistro.getNumeroTelefonico() : "";
        if (notif.isEnviarCorreo()) {
            String destinoCorreo = notif.getCorreo() != null && !notif.getCorreo().isBlank()
                    ? notif.getCorreo() : correo;
            boolean ok = emailService.enviarTicket(destinoCorreo, asunto, notif.getTicketHtml());
            resp.setCorreoEnviado(ok);
            if (!ok) errores.add("No se pudo enviar el correo");
        }
        if (notif.isEnviarWhatsapp()) {
            boolean ok = whatsappService.enviarMensaje(telefono, notif.getTicketTexto());
            resp.setWhatsappEnviado(ok);
            if (!ok) errores.add("No se pudo enviar el WhatsApp");
        }
        if (!errores.isEmpty()) resp.setErroresEnvio(errores);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public List<TotalDetalle> getTotalDetalle() {
        return entityManager.createNativeQuery("CALL inventario_key.TOTAL_DETALLE()", TotalDetalle.class)
                .getResultList();
    }
}