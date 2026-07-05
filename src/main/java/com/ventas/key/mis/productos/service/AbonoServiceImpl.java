package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.*;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.models.NotificacionRequest;
import com.ventas.key.mis.productos.models.abonos.*;
import com.ventas.key.mis.productos.repository.*;
import com.ventas.key.mis.productos.service.api.IAbonoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbonoServiceImpl implements IAbonoService {

    private static final Set<String> TIPOS_CREDITO = Set.of("APARTADO", "FIADO");

    // IDs del catálogo pagos_y_meses — efectivo=1, transferencia=62, tarjeta débito=2
    private static final int PAGOS_EFECTIVO      = 1;
    private static final int PAGOS_TRANSFERENCIA = 62;
    private static final int PAGOS_TARJETA       = 2;

    private final IAbonoRepository abonoRepository;
    private final IPedidoRepository pedidoRepository;
    private final IVentaRepository ventaRepository;
    private final IPagosYMesesRepository pagosYMesesRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IVarianteRepository varianteRepository;
    private final IProductosRepository productosRepository;
    private final EmailService emailService;
    private final WhatsappService whatsappService;

    @Override
    @Transactional
    public AbonoResponse registrarAbono(int pedidoId, AbonoRequest request) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + pedidoId));

        if (!TIPOS_CREDITO.contains(pedido.getTipoPedido())) {
            throw new RuntimeException("El pedido " + pedidoId + " es de tipo " + pedido.getTipoPedido() + " y no admite abonos");
        }
        if ("PAGADO".equals(pedido.getEstadoPedido())) {
            throw new RuntimeException("El pedido " + pedidoId + " ya está completamente pagado");
        }
        if ("cancelado".equals(pedido.getEstadoPedido())) {
            throw new RuntimeException("El pedido " + pedidoId + " está cancelado");
        }

        double totalPagado = pedido.getTotalPagado() != null ? pedido.getTotalPagado() : 0.0;
        double saldoPendiente = pedido.getTotalPedido() - totalPagado;
        if (request.getMonto() <= 0) {
            throw new RuntimeException("El monto del abono debe ser mayor a cero");
        }
        if (request.getMonto() > saldoPendiente) {
            throw new RuntimeException(
                String.format("El monto $%.2f excede el saldo pendiente de $%.2f", request.getMonto(), saldoPendiente));
        }

        String metodoPago = request.getMetodoPago() != null ? request.getMetodoPago() : "EFECTIVO";

        // DN-1: crédito solo acepta EFECTIVO o TRANSFERENCIA
        if ("TARJETA".equals(metodoPago)) {
            throw new RuntimeException("Los pedidos de crédito (APARTADO/FIADO) solo aceptan EFECTIVO o TRANSFERENCIA");
        }

        // NF-3: validar monto_dado cuando se recibe efectivo
        if ("EFECTIVO".equals(metodoPago) && request.getMontoDado() != null
                && request.getMontoDado() < request.getMonto()) {
            throw new RuntimeException(
                String.format("El monto entregado $%.2f es menor al monto del abono $%.2f",
                        request.getMontoDado(), request.getMonto()));
        }

        AbonoPedido abono = new AbonoPedido();
        abono.setPedido(pedido);
        abono.setMonto(request.getMonto());
        abono.setFechaPago(request.getFechaPago() != null ? request.getFechaPago() : LocalDate.now());
        abono.setMetodoPago(metodoPago);
        abono.setNota(request.getNota());
        abono.setMontoDado("EFECTIVO".equals(metodoPago) ? request.getMontoDado() : null);
        abonoRepository.save(abono);

        double nuevoTotalPagado = totalPagado + request.getMonto();
        pedido.setTotalPagado(nuevoTotalPagado);

        if (nuevoTotalPagado >= pedido.getTotalPedido()) {
            pedido.setEstadoPedido("PAGADO");
            if ("APARTADO".equals(pedido.getTipoPedido())) {
                pedido.setFechaRecogida(LocalDate.now());
            }
            log.info("Pedido {} liquidado — tipo: {}, total: {}", pedidoId, pedido.getTipoPedido(), pedido.getTotalPedido());
            crearVentaDesdePedido(pedido, metodoPago, request.getUsuarioId());
        }

        pedidoRepository.save(pedido);

        double saldo = Math.max(0.0, pedido.getTotalPedido() - nuevoTotalPagado);
        AbonoResponse resp = toAbonoResponse(abono);
        resp.setEstadoPedido(pedido.getEstadoPedido());
        resp.setSaldoRestante(saldo);

        if (request.getNotificacion() != null) {
            String correo  = correoCliente(pedido);
            String telefono = telefonoCliente(pedido);
            String asunto  = "PAGADO".equals(pedido.getEstadoPedido())
                    ? "✅ Apartado liquidado — Novedades Jade"
                    : "Comprobante de abono — Novedades Jade";
            enviarNotificaciones(request.getNotificacion(), pedido.getCliente(), correo, telefono, asunto, resp);
        }

        return resp;
    }

    @Override
    public List<AbonoResponse> obtenerAbonos(int pedidoId) {
        if (!pedidoRepository.existsById(pedidoId)) {
            throw new RuntimeException("Pedido no encontrado: " + pedidoId);
        }
        return abonoRepository.findByPedidoIdOrderByFechaPagoAsc(pedidoId)
                .stream().map(this::toAbonoResponse).toList();
    }

    @Override
    public List<EstadoCuentaDto> reporteEstadoCuenta() {
        return abonoRepository.findPedidosConSaldo().stream().map(p -> {
            List<AbonoResponse> abonos = abonoRepository
                    .findByPedidoIdOrderByFechaPagoAsc(p.getId())
                    .stream().map(this::toAbonoResponse).toList();

            double totalPagado = p.getTotalPagado() != null ? p.getTotalPagado() : 0.0;
            double totalPedido = p.getTotalPedido() != null ? p.getTotalPedido() : 0.0;

            EstadoCuentaDto dto = new EstadoCuentaDto();
            dto.setPedidoId(p.getId());
            dto.setTipoPedido(p.getTipoPedido());
            dto.setEstadoPedido(p.getEstadoPedido());
            dto.setCliente(nombreCliente(p));
            dto.setTelefono(telefonoCliente(p));
            dto.setTotalPedido(totalPedido);
            dto.setTotalPagado(totalPagado);
            dto.setSaldo(totalPedido - totalPagado);
            dto.setFechaPedido(p.getFechaPedido());
            dto.setAbonos(abonos);
            return dto;
        }).toList();
    }

    @Override
    public List<ReportePagadosDto> reportePagados() {
        return abonoRepository.findPedidosPagados().stream().map(p -> {
            List<AbonoResponse> abonos = abonoRepository
                    .findByPedidoIdOrderByFechaPagoAsc(p.getId())
                    .stream().map(this::toAbonoResponse).toList();

            LocalDate ultimoPago = abonos.stream()
                    .map(AbonoResponse::getFechaPago)
                    .filter(f -> f != null)
                    .max(Comparator.naturalOrder())
                    .orElse(null);

            ReportePagadosDto dto = new ReportePagadosDto();
            dto.setPedidoId(p.getId());
            dto.setTipoPedido(p.getTipoPedido());
            dto.setCliente(nombreCliente(p));
            dto.setTelefono(telefonoCliente(p));
            dto.setTotalPedido(p.getTotalPedido());
            dto.setFechaPedido(p.getFechaPedido());
            dto.setFechaUltimoPago(ultimoPago);
            dto.setAbonos(abonos);
            return dto;
        }).toList();
    }

    @Override
    public List<ReporteCanceladosDto> reporteCancelados() {
        return abonoRepository.findPedidosCancelados().stream().map(p -> {
            List<AbonoResponse> abonos = abonoRepository
                    .findByPedidoIdOrderByFechaPagoAsc(p.getId())
                    .stream().map(this::toAbonoResponse).toList();

            double totalPagado  = p.getTotalPagado()  != null ? p.getTotalPagado()  : 0.0;
            double totalPedido  = p.getTotalPedido()  != null ? p.getTotalPedido()  : 0.0;
            boolean esFiado     = "FIADO".equals(p.getTipoPedido());

            ReporteCanceladosDto dto = new ReporteCanceladosDto();
            dto.setPedidoId(p.getId());
            dto.setTipoPedido(p.getTipoPedido());
            dto.setCliente(nombreCliente(p));
            dto.setTelefono(telefonoCliente(p));
            dto.setTotalPedido(totalPedido);
            dto.setTotalPagado(totalPagado);
            dto.setSaldoAFavor(esFiado ? 0.0 : totalPagado);
            dto.setDeudaPendiente(esFiado ? totalPedido - totalPagado : 0.0);
            dto.setMotivo(p.getMotivoCancelacion());
            dto.setFechaPedido(p.getFechaPedido());
            dto.setFechaCancelacion(p.getFechaCancelacion());
            dto.setPuedeTransferir(!esFiado && totalPagado > 0);
            dto.setAbonos(abonos);
            return dto;
        }).toList();
    }

    @Override
    @Transactional
    public CancelarAbonoResponse cancelarPedido(int pedidoId, CancelarAbonoRequest request) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + pedidoId));

        if (!TIPOS_CREDITO.contains(pedido.getTipoPedido())) {
            throw new RuntimeException("El pedido " + pedidoId + " no es de tipo crédito");
        }
        if ("PAGADO".equals(pedido.getEstadoPedido())) {
            throw new RuntimeException("El pedido ya está pagado — no se puede cancelar");
        }
        if ("cancelado".equals(pedido.getEstadoPedido())) {
            throw new RuntimeException("El pedido ya está cancelado");
        }

        boolean esFiado = "FIADO".equals(pedido.getTipoPedido());
        boolean stockDevuelto = false;

        if (!esFiado) {
            // APARTADO: producto no fue entregado → devolver stock
            for (DetallePedido dp : pedido.getDetalles()) {
                Variantes v = dp.getVariante();
                v.setStock(v.getStock() + dp.getCantidad());
                varianteRepository.save(v);
                Producto prod = dp.getProducto();
                prod.setStock(prod.getStock() + dp.getCantidad());
                productosRepository.save(prod);
            }
            stockDevuelto = true;
        }

        double totalPagado   = pedido.getTotalPagado()  != null ? pedido.getTotalPagado()  : 0.0;
        double totalPendiente = pedido.getTotalPedido() != null ? pedido.getTotalPedido() - totalPagado : 0.0;

        pedido.setEstadoPedido("cancelado");
        String motivo = request.getMotivo() != null ? request.getMotivo() : "CANCELADO";
        pedido.setMotivoCancelacion(motivo.length() > 30 ? motivo.substring(0, 30) : motivo);
        pedido.setFechaCancelacion(LocalDate.now());
        pedidoRepository.save(pedido);

        String msg = esFiado
                ? String.format("FIADO cancelado. Stock NO devuelto (producto entregado). Deuda incobrable: $%.2f", totalPendiente)
                : String.format("APARTADO cancelado. Stock devuelto. Saldo a favor del cliente: $%.2f", totalPagado);

        log.info("Pedido {} cancelado — tipo: {}, stock devuelto: {}", pedidoId, pedido.getTipoPedido(), stockDevuelto);
        CancelarAbonoResponse resp = new CancelarAbonoResponse(
                pedidoId, pedido.getTipoPedido(), "cancelado", totalPagado, totalPendiente, stockDevuelto, msg,
                null, null, null);

        if (request.getNotificacion() != null) {
            String correo   = correoCliente(pedido);
            String telefono = telefonoCliente(pedido);
            List<String> errores = new ArrayList<>();
            if (request.getNotificacion().isEnviarCorreo()) {
                String notifCorreo = request.getNotificacion().getCorreo();
                boolean usaCorreoManual = notifCorreo != null && !notifCorreo.isBlank();
                String destinoCorreo = usaCorreoManual ? notifCorreo : correo;
                if (!usaCorreoManual && pedido.getCliente() != null
                        && !Boolean.TRUE.equals(pedido.getCliente().getCorreoVerificado())) {
                    resp.setCorreoEnviado(false);
                    errores.add("El correo del cliente no esta verificado, no se envio el ticket");
                } else {
                    boolean ok = emailService.enviarTicket(destinoCorreo, "Cancelación de pedido — Novedades Jade",
                            request.getNotificacion().getTicketHtml());
                    resp.setCorreoEnviado(ok);
                    if (!ok) errores.add("No se pudo enviar el correo");
                }
            }
            if (request.getNotificacion().isEnviarWhatsapp()) {
                boolean ok = whatsappService.enviarMensaje(telefono, request.getNotificacion().getTicketTexto());
                resp.setWhatsappEnviado(ok);
                if (!ok) errores.add("No se pudo enviar el WhatsApp");
            }
            if (!errores.isEmpty()) resp.setErroresEnvio(errores);
        }
        return resp;
    }

    @Override
    @Transactional
    public TransferirAbonoResponse transferirAbono(int pedidoIdOrigen, TransferirAbonoRequest request) {
        Pedido origen = pedidoRepository.findById(pedidoIdOrigen)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + pedidoIdOrigen));

        if (!"cancelado".equals(origen.getEstadoPedido())) {
            throw new RuntimeException("Solo se puede transferir desde un pedido cancelado");
        }
        if (!"APARTADO".equals(origen.getTipoPedido())) {
            throw new RuntimeException("Solo se puede transferir el saldo de un APARTADO (no de FIADO)");
        }

        double montoTransferido = origen.getTotalPagado() != null ? origen.getTotalPagado() : 0.0;
        if (montoTransferido <= 0) {
            throw new RuntimeException("No hay saldo a favor para transferir en el pedido " + pedidoIdOrigen);
        }

        Variantes variante = varianteRepository.findByIdWithLock(request.getNuevaVarianteId())
                .orElseThrow(() -> new RuntimeException("Variante no encontrada: " + request.getNuevaVarianteId()));

        if (variante.getStock() < request.getCantidad()) {
            throw new RuntimeException("Stock insuficiente. Disponible: " + variante.getStock());
        }

        double totalNuevo = request.getPrecioUnitario() * request.getCantidad();

        // Descontar stock del nuevo producto
        variante.setStock(variante.getStock() - request.getCantidad());
        varianteRepository.save(variante);
        Producto prod = variante.getProducto();
        prod.setStock(prod.getStock() - request.getCantidad());
        productosRepository.save(prod);

        // Crear nuevo pedido APARTADO
        DetallePedido detalle = new DetallePedido();
        detalle.setCantidad(request.getCantidad());
        detalle.setPrecioUnitario(request.getPrecioUnitario());
        detalle.setSubTotal(totalNuevo);
        detalle.setProducto(prod);
        detalle.setVariante(variante);

        Pedido nuevoPedido = new Pedido();
        nuevoPedido.setTipoPedido("APARTADO");
        nuevoPedido.setEstadoPedido("APARTADO");
        nuevoPedido.setTotalPedido(totalNuevo);
        nuevoPedido.setTotalPagado(0.0);
        nuevoPedido.setCliente(origen.getCliente());
        nuevoPedido.setClienteSinRegistro(origen.getClienteSinRegistro());
        nuevoPedido.setFechaPedido(LocalDate.now());
        nuevoPedido.setObservaciones("Transferido desde pedido #" + pedidoIdOrigen);
        detalle.setPedido(nuevoPedido);
        List<DetallePedido> detalles = new ArrayList<>();
        detalles.add(detalle);
        nuevoPedido.setDetalles(detalles);

        Pedido savedPedido = pedidoRepository.save(nuevoPedido);

        // Registrar el monto transferido como primer abono
        AbonoPedido abonoTransferido = new AbonoPedido();
        abonoTransferido.setPedido(savedPedido);
        abonoTransferido.setMonto(montoTransferido);
        abonoTransferido.setFechaPago(LocalDate.now());
        abonoTransferido.setMetodoPago("EFECTIVO");
        abonoTransferido.setNota("Transferido desde pedido #" + pedidoIdOrigen);
        abonoRepository.save(abonoTransferido);

        savedPedido.setTotalPagado(montoTransferido);
        double saldoPendiente = Math.max(0.0, totalNuevo - montoTransferido);
        String estadoFinal;

        if (montoTransferido >= totalNuevo) {
            savedPedido.setEstadoPedido("PAGADO");
            savedPedido.setFechaRecogida(LocalDate.now());
            estadoFinal = "PAGADO";
            crearVentaDesdePedido(savedPedido, "EFECTIVO", request.getUsuarioId());
        } else {
            estadoFinal = "APARTADO";
        }

        pedidoRepository.save(savedPedido);

        String msg = estadoFinal.equals("PAGADO")
                ? String.format("Transferencia completa. Nuevo pedido #%d liquidado con $%.2f transferidos.", savedPedido.getId(), montoTransferido)
                : String.format("Nuevo pedido #%d creado. Saldo pendiente: $%.2f", savedPedido.getId(), saldoPendiente);

        log.info("Transferencia pedido {} → nuevo pedido {}, monto: {}", pedidoIdOrigen, savedPedido.getId(), montoTransferido);
        return new TransferirAbonoResponse(savedPedido.getId(), totalNuevo, montoTransferido, saldoPendiente, estadoFinal, msg);
    }

    private void crearVentaDesdePedido(Pedido pedido, String metodoPago, Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado al crear venta: " + usuarioId));

        int pagosYMesesId = switch (metodoPago) {
            case "TRANSFERENCIA" -> PAGOS_TRANSFERENCIA;
            case "TARJETA"       -> PAGOS_TARJETA;
            default              -> PAGOS_EFECTIVO;
        };

        PagosYMeses pagosYMeses = pagosYMesesRepository.findById(pagosYMesesId)
                .orElseThrow(() -> new RuntimeException("Catálogo de pago no encontrado: " + pagosYMesesId));

        List<DetalleVentaVariante> detallesVenta = new ArrayList<>();
        for (DetallePedido dp : pedido.getDetalles()) {
            double precioCosto = dp.getVariante().getProducto().getPrecioCosto();
            double subTotal    = dp.getSubTotal();
            double ganancia    = subTotal - (precioCosto * dp.getCantidad());

            DetalleVentaVariante dvv = new DetalleVentaVariante();
            dvv.setCantidad(dp.getCantidad());
            dvv.setPrecioUnitario(dp.getPrecioUnitario());
            dvv.setSubTotal(subTotal);
            dvv.setPrecioCosto(precioCosto);
            dvv.setGanancia(ganancia);
            dvv.setFechaVenta(LocalDate.now());
            dvv.setVariante(dp.getVariante());
            detallesVenta.add(dvv);
        }

        double totalVenta    = detallesVenta.stream().mapToDouble(DetalleVentaVariante::getSubTotal).sum();
        double gananciaTotal = detallesVenta.stream().mapToDouble(DetalleVentaVariante::getGanancia).sum();

        Venta venta = new Venta();
        venta.setEstadoVenta("Entregado");
        venta.setFechaVenta(LocalDateTime.now());
        venta.setUsuario(usuario);
        venta.setCliente(pedido.getCliente());
        venta.setClienteSinRegistro(pedido.getClienteSinRegistro());
        venta.setPagosYMeses(pagosYMeses);
        venta.setPedido(pedido);
        venta.setTotalVenta(totalVenta);
        venta.setGananciaTotal(gananciaTotal);
        detallesVenta.forEach(dvv -> dvv.setVenta(venta));
        venta.setDetalles(detallesVenta);

        ventaRepository.save(venta);
        log.info("Venta creada para pedido {} — total: {}, método: {}", pedido.getId(), totalVenta, metodoPago);
    }

    private AbonoResponse toAbonoResponse(AbonoPedido a) {
        Double cambio = null;
        if ("EFECTIVO".equals(a.getMetodoPago()) && a.getMontoDado() != null && a.getMontoDado() > a.getMonto()) {
            cambio = Math.round((a.getMontoDado() - a.getMonto()) * 100.0) / 100.0;
        }
        AbonoResponse r = new AbonoResponse();
        r.setId(a.getId());
        r.setMonto(a.getMonto());
        r.setFechaPago(a.getFechaPago());
        r.setMetodoPago(a.getMetodoPago());
        r.setNota(a.getNota());
        r.setMontoDado(a.getMontoDado());
        r.setCambio(cambio);
        return r;
    }

    private String nombreCliente(Pedido p) {
        if (p.getCliente() != null) return p.getCliente().getNombrePersona();
        if (p.getClienteSinRegistro() != null) return p.getClienteSinRegistro().getNombrePersona();
        return "Sin nombre";
    }

    private String telefonoCliente(Pedido p) {
        if (p.getCliente() != null) return p.getCliente().getNumeroTelefonico();
        if (p.getClienteSinRegistro() != null) return p.getClienteSinRegistro().getNumeroTelefonico();
        return "";
    }

    private String correoCliente(Pedido p) {
        if (p.getCliente() != null) return p.getCliente().getCorreoElectronico();
        if (p.getClienteSinRegistro() != null) return p.getClienteSinRegistro().getCorreoElectronico();
        return "";
    }

    private void enviarNotificaciones(NotificacionRequest notif, Cliente cliente, String correo, String telefono,
                                      String asunto, AbonoResponse resp) {
        List<String> errores = new ArrayList<>();
        if (notif.isEnviarCorreo()) {
            boolean usaCorreoManual = notif.getCorreo() != null && !notif.getCorreo().isBlank();
            String destinoCorreo = usaCorreoManual ? notif.getCorreo() : correo;
            if (!usaCorreoManual && cliente != null && !Boolean.TRUE.equals(cliente.getCorreoVerificado())) {
                resp.setCorreoEnviado(false);
                errores.add("El correo del cliente no esta verificado, no se envio el ticket");
            } else {
                boolean ok = emailService.enviarTicket(destinoCorreo, asunto, notif.getTicketHtml());
                resp.setCorreoEnviado(ok);
                if (!ok) errores.add("No se pudo enviar el correo");
            }
        }
        if (notif.isEnviarWhatsapp()) {
            boolean ok = whatsappService.enviarMensaje(telefono, notif.getTicketTexto());
            resp.setWhatsappEnviado(ok);
            if (!ok) errores.add("No se pudo enviar el WhatsApp");
        }
        if (!errores.isEmpty()) resp.setErroresEnvio(errores);
    }
}
