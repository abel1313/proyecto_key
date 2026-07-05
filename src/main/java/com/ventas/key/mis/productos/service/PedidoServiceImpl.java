package com.ventas.key.mis.productos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.mis.productos.entity.*;
import com.ventas.key.mis.productos.entity.DetalleVentaVariante;
import com.ventas.key.mis.productos.entity.MesesIntereses;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.entity.PagosYMeses;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.handleExeption.GenericException;
import com.ventas.key.mis.productos.models.PageableDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.UsuarioDto;
import com.ventas.key.mis.productos.models.pedidos.AbonoDetalleItem;
import com.ventas.key.mis.productos.models.pedidos.DetalleItemResponse;
import com.ventas.key.mis.productos.models.pedidos.NotificarPedidoRequest;
import com.ventas.key.mis.productos.models.pedidos.PedidoDetalleResponse;
import com.ventas.key.mis.productos.models.pedidos.PedidoGenerico;
import com.ventas.key.mis.productos.models.pedidos.PedidosDTOPedido;
import com.ventas.key.mis.productos.repository.IAbonoRepository;
import com.ventas.key.mis.productos.repository.IClienteRepository;
import com.ventas.key.mis.productos.repository.IDetallePagoRepository;
import com.ventas.key.mis.productos.repository.IDetallePedidoRepository;
import com.ventas.key.mis.productos.repository.IPagosYMesesRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IPromocionRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.repository.IVentaRepository;
import com.ventas.key.mis.productos.config.RabbitMQConfig;
import com.ventas.key.mis.productos.service.api.IPedidoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class PedidoServiceImpl extends CrudAbstractServiceImpl<
        Pedido,
        List<Pedido>,
        Optional<Pedido>,
        Integer,
        PginaDto<List<Pedido>>> implements IPedidoService {

    private final ObjectMapper objectMapper;
    private final IClienteRepository iClienteRepository;
    private final IProductosRepository iProductoRepository;
    private final IPedidoRepository iPedidoRepository;
    private final VentaServiceImpl vImpl;
    private final IUsuarioRepository iUsuarioRepository;
    private final IDetallePagoRepository iDetallePagoRepository;
    private final IDetallePedidoRepository iDetallePedidoRepository;
    private final IPagosYMesesRepository iPagosYMesesRepository;
    private final IVarianteRepository iVarianteRepository;
    private final IPromocionRepository iPromocionRepository;
    private final PromocionServiceImpl promocionService;

    @Autowired private CacheService cacheService;
    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private IVentaRepository iVentaRepository;
    @Autowired private IAbonoRepository iAbonoRepository;
    @Autowired private EmailService emailService;

    public PedidoServiceImpl(final IPedidoRepository iPedidoRepository, ErrorGenerico error,
                             final IClienteRepository iClienteRepository,
                             final IProductosRepository iProductoRepository,
                             final VentaServiceImpl vImpl,
                             final IUsuarioRepository iUsuarioRepository,
                             final ObjectMapper objectMapper,
                             final IDetallePagoRepository iDetallePagoRepository,
                             final IDetallePedidoRepository iDetallePedidoRepository,
                             final IPagosYMesesRepository iPagosYMesesRepository,
                             final IVarianteRepository iVarianteRepository,
                             final IPromocionRepository iPromocionRepository,
                             final PromocionServiceImpl promocionService) {
        super(iPedidoRepository, error);
        this.iProductoRepository = iProductoRepository;
        this.iClienteRepository = iClienteRepository;
        this.iPedidoRepository = iPedidoRepository;
        this.iUsuarioRepository = iUsuarioRepository;
        this.vImpl = vImpl;
        this.objectMapper = objectMapper;
        this.iDetallePagoRepository = iDetallePagoRepository;
        this.iDetallePedidoRepository = iDetallePedidoRepository;
        this.iPagosYMesesRepository = iPagosYMesesRepository;
        this.iVarianteRepository = iVarianteRepository;
        this.iPromocionRepository = iPromocionRepository;
        this.promocionService = promocionService;
    }

    @Transactional
    public Pedido savePedido(@RequestBody PedidosDTOPedido requestG, BindingResult result) throws Exception {

        Cliente cliente = this.iClienteRepository.findById(requestG.getCliente().getId())
                .orElseThrow(() -> new Exception("Ocurrio un erro al buscar al cliente"));
        if (!Boolean.TRUE.equals(cliente.getCorreoVerificado())) {
            throw new RuntimeException("Debes verificar tu correo antes de generar un pedido");
        }
        if (!Boolean.TRUE.equals(cliente.getDatosCompletos())) {
            throw new RuntimeException("Debes completar tus datos (nombre, apellido paterno, telefono) antes de generar un pedido");
        }
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setEstadoPedido(requestG.getEstadoPedido());
        pedido.setFechaPedido(requestG.getFechaPedido());
        pedido.setFechaRecogida(requestG.getFechaRecogida());
        pedido.setObservaciones(requestG.getObservaciones());
        String tipoPedido = requestG.getTipoPedido() != null ? requestG.getTipoPedido() : "NORMAL";
        pedido.setTipoPedido(tipoPedido);
        pedido.setTotalPagado(0.0);

        List<DetallePedido> detallePedido = new ArrayList<>();
        for (var mpa : requestG.getDetalles()) {
            Variantes variante = null;
            Producto prod;

            if (mpa.getVarianteId() != null) {
                variante = iVarianteRepository.findByIdWithLock(mpa.getVarianteId())
                        .orElseThrow(() -> new RuntimeException("Variante no encontrada: " + mpa.getVarianteId()));

                if (variante.getStock() < mpa.getCantidad()) {
                    throw new RuntimeException("Stock insuficiente en variante id " + mpa.getVarianteId()
                            + ". Disponible: " + variante.getStock() + ", solicitado: " + mpa.getCantidad());
                }
                variante.setStock(variante.getStock() - mpa.getCantidad());
                iVarianteRepository.save(variante);

                prod = this.iProductoRepository.findByIdWithLock(variante.getProducto().getId())
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado para variante: " + mpa.getVarianteId()));
            } else {
                prod = this.iProductoRepository.findByIdWithLock(mpa.getProducto().getId())
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + mpa.getProducto().getId()));
            }

            if (prod.getStock() < mpa.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para: " + prod.getNombre()
                        + ". Disponible: " + prod.getStock() + ", solicitado: " + mpa.getCantidad());
            }

            // Lineas sin promocionId deben pagar el precio de catalogo — el precio con descuento
            // solo es valido dentro de una promocion (validada aparte en validarLineasDePromocion).
            // Sin este chequeo, el front (o cualquiera con el token) podia mandar cualquier precio.
            if (mpa.getPromocionId() == null) {
                validarPrecioCatalogo(prod, mpa.getPrecioUnitario(), mpa.getCantidad(), mpa.getSubTotal());
            }

            prod.setStock(prod.getStock() - mpa.getCantidad());
            this.iProductoRepository.save(prod);

            DetallePedido dta = new DetallePedido();
            dta.setCantidad(mpa.getCantidad());
            dta.setPrecioUnitario(mpa.getPrecioUnitario());
            dta.setSubTotal(mpa.getSubTotal());
            dta.setPedido(pedido);
            dta.setProducto(prod);
            dta.setVariante(variante);
            if (mpa.getPromocionId() != null) {
                Promocion promocion = this.iPromocionRepository.findById(mpa.getPromocionId())
                        .orElseThrow(() -> new RuntimeException("La promocion ya no esta disponible"));
                dta.setPromocion(promocion);
            }
            detallePedido.add(dta);
        }

        validarLineasDePromocion(detallePedido, tipoPedido);

        pedido.setDetalles(detallePedido);
        double totalPedido = detallePedido.stream().mapToDouble(DetallePedido::getSubTotal).sum();
        pedido.setTotalPedido(totalPedido);
        Pedido saved = this.iPedidoRepository.save(pedido);
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        return saved;
    }

    // El precio/subtotal que manda el cliente en una linea normal (sin promocionId) debe
    // coincidir con el precio real del producto — de lo contrario cualquiera con sesion podria
    // editar el request y pagar lo que quiera. Tolerancia de 1 centavo por redondeo de Double.
    private void validarPrecioCatalogo(Producto prod, Double precioUnitario, Integer cantidad, Double subTotal) {
        double precioCatalogo = prod.getPrecioVenta() != null ? prod.getPrecioVenta() : 0.0;
        if (precioUnitario == null || Math.abs(precioUnitario - precioCatalogo) > 0.01) {
            throw new RuntimeException("El precio de " + prod.getNombre() + " no es valido");
        }
        double subTotalEsperado = precioCatalogo * cantidad;
        if (subTotal == null || Math.abs(subTotal - subTotalEsperado) > 0.01) {
            throw new RuntimeException("El subtotal de " + prod.getNombre() + " no es valido");
        }
    }

    // Agrupa las lineas del pedido que traen promocionId y valida cada combo contra
    // PromocionServiceImpl (vigencia, precios, y que el pedido sea de contado).
    private void validarLineasDePromocion(List<DetallePedido> detallePedido, String tipoPedido) {
        Map<Integer, List<PromocionServiceImpl.LineaPromocionCheck>> lineasPorPromocion = new LinkedHashMap<>();
        for (DetallePedido d : detallePedido) {
            if (d.getPromocion() != null) {
                lineasPorPromocion.computeIfAbsent(d.getPromocion().getId(), k -> new ArrayList<>())
                        .add(new PromocionServiceImpl.LineaPromocionCheck(
                                d.getVariante().getId(), d.getCantidad(), d.getPrecioUnitario()));
            }
        }
        for (var entry : lineasPorPromocion.entrySet()) {
            promocionService.validarLineasPromocion(entry.getKey(), entry.getValue(), tipoPedido);
        }
    }

    @Transactional
    @Override
    public PedidoGenerico updatePedido(int id, PedidoGenerico requestG) throws Exception {
        Pedido pedido = this.iPedidoRepository.findById(id)
                .orElseThrow(() -> new GenericException(500, "El pedido no existe"));

        if ("Entregado".equals(pedido.getEstadoPedido())) {
            throw new RuntimeException("El pedido ya fue confirmado");
        }
        if ("cancelado".equals(pedido.getEstadoPedido())) {
            throw new RuntimeException("El pedido está cancelado y no se puede confirmar");
        }
        if ("APARTADO".equals(pedido.getTipoPedido()) || "FIADO".equals(pedido.getTipoPedido())) {
            throw new RuntimeException("Los pedidos de tipo " + pedido.getTipoPedido() + " se liquidan mediante abonos, no por esta vía");
        }

        PagosYMeses pagosYMeses = iPagosYMesesRepository.findById(requestG.getPagosYMesesId())
                .orElseThrow(() -> new RuntimeException("Opción de pago no válida"));

        MesesIntereses mesesIntereses = pagosYMeses.getMesesIntereses();

        // tarifa e IVA son null cuando es efectivo/transferencia → se tratan como 0
        double tasaTarifa = mesesIntereses.getTarifaTerminal() != null
                ? mesesIntereses.getTarifaTerminal().getTarifa() / 100.0 : 0.0;
        double tasaIva = mesesIntereses.getIvaTerminal() != null
                ? mesesIntereses.getIvaTerminal().getIva() / 100.0 : 0.0;

        // detallePago solo aplica cuando hay tarifa/IVA (para tarjetas)
        DetallePago detallePago = null;
        if (mesesIntereses.getTarifaTerminal() != null && mesesIntereses.getIvaTerminal() != null) {
            DetallePagoId detallePagoId = new DetallePagoId(
                    pagosYMeses.getTipoPago().getId(),
                    mesesIntereses.getTarifaTerminal().getId(),
                    mesesIntereses.getIvaTerminal().getId()
            );
            detallePago = iDetallePagoRepository.findById(detallePagoId).orElse(null);
        }

        UsuarioDto usr = iUsuarioRepository.findUserByIdCliente(requestG.getCliente().getId())
                .orElseThrow(() -> new Exception("Ocurrio un error al buscar el usuario"));
        Usuario u = this.iUsuarioRepository.findById((int) usr.getIdUsuario())
                .orElseThrow(() -> new Exception("Ocurrio un error al buscar el usuario"));

        Venta venta = new Venta();
        venta.setEstadoVenta("Entregado");
        venta.setFechaVenta(LocalDateTime.now());
        venta.setPedido(pedido);
        venta.setUsuario(u);
        venta.setCliente(pedido.getCliente());
        venta.setDetallePago(detallePago);
        venta.setPagosYMeses(pagosYMeses);

        List<DetalleVentaVariante> det = new ArrayList<>();
        for (DetallePedido dp : pedido.getDetalles()) {
            double precioCosto  = dp.getVariante().getProducto().getPrecioCosto();
            double subTotal     = dp.getSubTotal();
            double costoTotal   = precioCosto * dp.getCantidad();
            double comision     = subTotal * (tasaTarifa + tasaIva);
            double ganancia     = subTotal - costoTotal - comision;

            DetalleVentaVariante dvv = new DetalleVentaVariante();
            dvv.setCantidad(dp.getCantidad());
            dvv.setPrecioUnitario(dp.getPrecioUnitario());
            dvv.setSubTotal(subTotal);
            dvv.setPrecioCosto(precioCosto);
            dvv.setGanancia(ganancia);
            dvv.setFechaVenta(LocalDate.now());
            dvv.setVariante(dp.getVariante());
            dvv.setVenta(venta);
            det.add(dvv);
        }

        double totalVenta    = det.stream().mapToDouble(DetalleVentaVariante::getSubTotal).sum();
        double gananciaTotal = det.stream().mapToDouble(DetalleVentaVariante::getGanancia).sum();

        venta.setTotalVenta(totalVenta);
        venta.setGananciaTotal(gananciaTotal);
        venta.setDetalles(det);

        pedido.setEstadoPedido("Entregado");
        this.iPedidoRepository.save(pedido);
        this.vImpl.save(venta);
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        return new PedidoGenerico();
    }

    @Override
    public PageableDto<List<PedidoGenerico>> obtenerPedido(int id, int size, int pageSize) {
        Pageable pageable = PageRequest.of(pageSize, size);
        Page<String> jsonList = iPedidoRepository.findPedidoPorId2(id, pageable);
        return getListPageableDto(jsonList);
    }

    @Override
    public PageableDto<List<PedidoGenerico>> obtenerPedidoPorId(int idPedido, int idCliente,int size, int pageSize) {
        Pageable pageable = PageRequest.of(pageSize, size);
        Page<String> jsonList = iPedidoRepository.pediodPorId(idPedido, idCliente,pageable);
        return getListPageableDto(jsonList);
    }

    @Override
    public PageableDto<List<PedidoGenerico>> buscarClientePorPedido(String buscar, int size, int pageSize) {
        Pageable pageable = PageRequest.of(pageSize, size);
        Page<String> jsonList;
        if(buscar.isEmpty()){
            jsonList = iPedidoRepository.buscarTodosLosPedidos(pageable);
        }else{
            jsonList = iPedidoRepository.buscarPedidosPorCliente(buscar, pageable);
        }
        return getListPageableDto(jsonList);
    }


    @Transactional
    @Override
    public void deletePedidoById(int id, String motivo) {
        Pedido pedido = iPedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if ("cancelado".equals(pedido.getEstadoPedido()) || "Entregado".equals(pedido.getEstadoPedido())
                || "PAGADO".equals(pedido.getEstadoPedido())) {
            throw new RuntimeException("No se puede cancelar un pedido en estado: " + pedido.getEstadoPedido());
        }

        pedido.getDetalles().forEach(detalle -> {
            Producto prod = iProductoRepository.findByIdWithLock(detalle.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado al devolver stock"));
            prod.setStock(prod.getStock() + detalle.getCantidad());
            iProductoRepository.save(prod);

            if (detalle.getVariante() != null) {
                Variantes variante = iVarianteRepository.findById(detalle.getVariante().getId())
                        .orElseThrow(() -> new RuntimeException("Variante no encontrada al devolver stock"));
                variante.setStock(variante.getStock() + detalle.getCantidad());
                iVarianteRepository.save(variante);
            }
        });

        pedido.setEstadoPedido("cancelado");
        pedido.setMotivoCancelacion(motivo);
        pedido.setFechaCancelacion(LocalDate.now());
        iPedidoRepository.save(pedido);
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
    }

    @Transactional
    @Override
    public void eliminarDetallePedido(int pedidoId, int productoId, int cantidad) {
        Pedido pedido = iPedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if ("Entregado".equals(pedido.getEstadoPedido())) {
            throw new RuntimeException("No se puede modificar un pedido ya entregado");
        }
        if ("cancelado".equals(pedido.getEstadoPedido())) {
            throw new RuntimeException("No se puede modificar un pedido cancelado");
        }

        DetallePedido detalle = pedido.getDetalles().stream()
                .filter(d -> d.getProducto().getId().equals(productoId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("El producto no existe en este pedido"));

        Producto prod = iProductoRepository.findByIdWithLock(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (cantidad >= detalle.getCantidad()) {
            prod.setStock(prod.getStock() + detalle.getCantidad());
            iProductoRepository.save(prod);
            if (detalle.getVariante() != null) {
                Variantes variante = iVarianteRepository.findById(detalle.getVariante().getId())
                        .orElseThrow(() -> new RuntimeException("Variante no encontrada al devolver stock"));
                variante.setStock(variante.getStock() + detalle.getCantidad());
                iVarianteRepository.save(variante);
            }
            iDetallePedidoRepository.delete(detalle);
        } else {
            prod.setStock(prod.getStock() + cantidad);
            iProductoRepository.save(prod);
            if (detalle.getVariante() != null) {
                Variantes variante = iVarianteRepository.findById(detalle.getVariante().getId())
                        .orElseThrow(() -> new RuntimeException("Variante no encontrada al devolver stock"));
                variante.setStock(variante.getStock() + cantidad);
                iVarianteRepository.save(variante);
            }
            detalle.setCantidad(detalle.getCantidad() - cantidad);
            detalle.setSubTotal(detalle.getPrecioUnitario() * detalle.getCantidad());
            iDetallePedidoRepository.save(detalle);
        }
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
    }

    @Override
    public PedidoDetalleResponse getDetallePedido(int id) {
        Pedido pedido = iPedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + id));

        double totalPagado = pedido.getTotalPagado() != null ? pedido.getTotalPagado() : 0.0;
        double totalPedido = pedido.getTotalPedido() != null ? pedido.getTotalPedido() : 0.0;

        PedidoDetalleResponse resp = new PedidoDetalleResponse();
        resp.setPedidoId(pedido.getId());
        resp.setTipoPedido(pedido.getTipoPedido());
        resp.setEstadoPedido(pedido.getEstadoPedido());
        resp.setTotalPedido(totalPedido);
        resp.setTotalPagado(totalPagado);
        resp.setSaldoPendiente(Math.max(0.0, totalPedido - totalPagado));
        resp.setFechaPedido(pedido.getFechaPedido());
        resp.setFechaRecogida(pedido.getFechaRecogida());
        resp.setObservaciones(pedido.getObservaciones());
        resp.setMotivoCancelacion(pedido.getMotivoCancelacion());
        resp.setFechaCancelacion(pedido.getFechaCancelacion());

        if (pedido.getCliente() != null) {
            resp.setClienteNombre(pedido.getCliente().getNombrePersona());
            resp.setClienteTelefono(pedido.getCliente().getNumeroTelefonico());
            resp.setClienteCorreo(pedido.getCliente().getCorreoElectronico());
        } else if (pedido.getClienteSinRegistro() != null) {
            resp.setClienteNombre(pedido.getClienteSinRegistro().getNombrePersona());
            resp.setClienteTelefono(pedido.getClienteSinRegistro().getNumeroTelefonico());
            resp.setClienteCorreo(pedido.getClienteSinRegistro().getCorreoElectronico());
        }

        // metodoPago/montoDado solo existen para ventas NORMAL al contado (vienen de la Venta
        // ligada al pedido); en créditos (APARTADO/FIADO) cada abono tiene los suyos, ver abonos[].
        iVentaRepository.findByPedidoId(pedido.getId()).ifPresent(venta -> {
            if (venta.getPagosYMeses() != null && venta.getPagosYMeses().getTipoPago() != null) {
                resp.setMetodoPago(venta.getPagosYMeses().getTipoPago().getFormaPago());
            }
            resp.setMontoDado(venta.getMontoDado());
        });

        resp.setAbonos(iAbonoRepository.findByPedidoIdOrderByFechaPagoAsc(pedido.getId()).stream()
                .map(a -> new AbonoDetalleItem(
                        a.getId(), a.getMonto(), a.getFechaPago(), a.getMetodoPago(), a.getNota(), a.getMontoDado()))
                .toList());

        List<DetalleItemResponse> detalles = pedido.getDetalles().stream().map(dp -> {
            DetalleItemResponse item = new DetalleItemResponse();
            item.setId(dp.getId());
            item.setCantidad(dp.getCantidad());
            item.setPrecioUnitario(dp.getPrecioUnitario());
            item.setSubTotal(dp.getSubTotal());
            if (dp.getProducto() != null) {
                item.setProductoNombre(dp.getProducto().getNombre());
            }
            if (dp.getVariante() != null) {
                item.setVarianteId(dp.getVariante().getId());
                item.setTalla(dp.getVariante().getTalla());
                item.setColor(dp.getVariante().getColor());
                item.setDescripcion(dp.getVariante().getDescripcion());
            }
            if (dp.getPromocion() != null) {
                item.setPromocionId(dp.getPromocion().getId());
                item.setPromocionDescripcion(dp.getPromocion().getDescripcion());
            }
            return item;
        }).toList();

        resp.setDetalles(detalles);
        return resp;
    }

    @Override
    public boolean notificarPedido(int id, NotificarPedidoRequest requestG) {
        if (iPedidoRepository.findById(id).isEmpty()) {
            throw new RuntimeException("Pedido no encontrado: " + id);
        }
        String asunto = "Comprobante de tu pedido #" + id + " — Novedades Jade";
        return emailService.enviarTicket(requestG.getCorreo(), asunto, requestG.getTicketHtml());
    }

    private PageableDto<List<PedidoGenerico>> getListPageableDto(Page<String> jsonList) {
        PageableDto<List<PedidoGenerico>> pedidoPage = new PageableDto<>();
        pedidoPage.setTotalPaginas(jsonList.getTotalPages());
        List<PedidoGenerico> pedidos = new ArrayList<>();
        try {
            for (String json : jsonList) {
                PedidoGenerico pedido = objectMapper.readValue(json, PedidoGenerico.class);
                pedidos.add(pedido);
            }
            pedidoPage.setList(pedidos);
            return pedidoPage;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al convertir JSON a objeto", e);
        }
    }
}
