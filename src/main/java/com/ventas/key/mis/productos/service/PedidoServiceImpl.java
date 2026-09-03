package com.ventas.key.mis.productos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
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
import com.ventas.key.mis.productos.models.pedidos.EditarEntregaPedidoRequest;
import com.ventas.key.mis.productos.models.pedidos.NotificarPedidoRequest;
import com.ventas.key.mis.productos.models.pedidos.PedidoDetalleResponse;
import com.ventas.key.mis.productos.models.pedidos.PedidoGenerico;
import com.ventas.key.mis.productos.models.pedidos.PedidosDTOPedido;
import com.ventas.key.mis.productos.repository.IAbonoRepository;
import com.ventas.key.mis.productos.repository.IAccesorioRamoRepository;
import com.ventas.key.mis.productos.repository.IClienteRepository;
import com.ventas.key.mis.productos.repository.IColorFlorRepository;
import com.ventas.key.mis.productos.repository.IDetallePagoRepository;
import com.ventas.key.mis.productos.repository.IDetallePedidoRepository;
import com.ventas.key.mis.productos.repository.ILugarEntregaRepository;
import com.ventas.key.mis.productos.repository.IPagosYMesesRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IPromocionRepository;
import com.ventas.key.mis.productos.repository.IRamoPedidoDetalleRepository;
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
    private final ILugarEntregaRepository iLugarEntregaRepository;
    private final IRamoPedidoDetalleRepository iRamoPedidoDetalleRepository;
    private final IAccesorioRamoRepository iAccesorioRamoRepository;
    private final IColorFlorRepository iColorFlorRepository;

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
                             final PromocionServiceImpl promocionService,
                             final ILugarEntregaRepository iLugarEntregaRepository,
                             final IRamoPedidoDetalleRepository iRamoPedidoDetalleRepository,
                             final IAccesorioRamoRepository iAccesorioRamoRepository,
                             final IColorFlorRepository iColorFlorRepository) {
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
        this.iLugarEntregaRepository = iLugarEntregaRepository;
        this.iRamoPedidoDetalleRepository = iRamoPedidoDetalleRepository;
        this.iAccesorioRamoRepository = iAccesorioRamoRepository;
        this.iColorFlorRepository = iColorFlorRepository;
    }

    // Bug encontrado 2026-08-28: ColorFlor.stock (lo que ve el cliente en "Arma tu ramo" al
    // repartir flores entre colores) es una copia separada del stock real, que vive en la
    // variante "sombra" de ese color (ver ProductoSombraServiceImpl). Cada vez que un pedido
    // mueve el stock de esa variante (venta, cancelación, edición de detalle) hay que empujar el
    // mismo valor a ColorFlor.stock -- si no, con cada ramo vendido la variante baja pero
    // ColorFlor.stock se queda pegado en el número viejo, hasta que el configurador ofrece más
    // flores de las que en realidad quedan y el pedido explota al guardar con "Stock
    // insuficiente" (síntoma reportado: el configurador decía 100 disponibles, la variante ya
    // tenía 20). No hace falta clamp a 0: la variante nunca queda negativa (los checks de arriba
    // ya lo garantizan), así que tampoco ColorFlor.stock.
    private void sincronizarStockColorFlor(Variantes variante) {
        iColorFlorRepository.findByVarianteId(variante.getId()).ifPresent(color -> {
            color.setStock(variante.getStock());
            iColorFlorRepository.save(color);
        });
    }

    // lugarEntregaId es opcional (null = no se captura el lugar de entrega en ese pedido)
    private LugarEntrega resolveLugarEntrega(Integer lugarEntregaId) {
        if (lugarEntregaId == null) return null;
        return iLugarEntregaRepository.findById(lugarEntregaId)
                .orElseThrow(() -> new RuntimeException("Lugar de entrega no encontrado: " + lugarEntregaId));
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
        pedido.setFechaHoraRegistro(LocalDateTime.now());
        pedido.setFechaRecogida(requestG.getFechaRecogida());
        pedido.setObservaciones(requestG.getObservaciones());
        pedido.setNombreReceptor(requestG.getNombreReceptor());
        pedido.setDireccionEntrega(requestG.getDireccionEntrega());
        pedido.setLatitud(requestG.getLatitud());
        pedido.setLongitud(requestG.getLongitud());
        pedido.setReferencias(requestG.getReferencias());
        pedido.setLugarEntrega(resolveLugarEntrega(requestG.getLugarEntregaId()));
        pedido.setUrlFacebook(requestG.getUrlFacebook());
        String tipoPedido = requestG.getTipoPedido() != null ? requestG.getTipoPedido() : "NORMAL";
        pedido.setTipoPedido(tipoPedido);
        pedido.setTotalPagado(0.0);

        List<DetallePedido> detallePedido = new ArrayList<>();
        for (var mpa : requestG.getDetalles()) {
            if (mpa.getCantidad() == null || mpa.getCantidad() <= 0) {
                throw new RuntimeException("La cantidad es obligatoria y debe ser mayor a 0");
            }

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
                sincronizarStockColorFlor(variante);

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
        notificarSeguimientoPedido(pedido);
        this.vImpl.save(venta);
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        return new PedidoGenerico();
    }

    @Override
    public PageableDto<List<PedidoGenerico>> obtenerPedido(int id, int size, int pageSize) {
        // `id` es un cliente_id -- sin este chequeo cualquier usuario autenticado podia mandar el
        // id de OTRO cliente por la URL y ver su historial completo de pedidos (nombre, correo,
        // telefono y detalle de cada compra), pura IDOR (encontrado 2026-08-27). ADMIN si puede
        // consultar el de cualquiera; un cliente solo el suyo, sin importar que id venga en la URL.
        int idEfectivo = AuthenticationUtils.isAdminContext() ? id : idClientePropio();
        Pageable pageable = PageRequest.of(pageSize, size);
        Page<String> jsonList = iPedidoRepository.findPedidoPorId2(idEfectivo, pageable);
        return getListPageableDto(jsonList);
    }

    @Override
    public PageableDto<List<PedidoGenerico>> obtenerPedidoPorId(int idPedido, int idCliente,int size, int pageSize) {
        // Mismo chequeo que obtenerPedido() -- idCliente tambien es atacable por URL.
        int idClienteEfectivo = AuthenticationUtils.isAdminContext() ? idCliente : idClientePropio();
        Pageable pageable = PageRequest.of(pageSize, size);
        Page<String> jsonList = iPedidoRepository.pediodPorId(idPedido, idClienteEfectivo,pageable);
        return getListPageableDto(jsonList);
    }

    /** Cliente del usuario autenticado, o -1 (ningun cliente tiene ese id) si no tiene uno ligado. */
    private int idClientePropio() {
        Cliente cliente = AuthenticationUtils.currentUsuario().getCliente();
        return cliente != null ? cliente.getId() : -1;
    }

    @Override
    public PageableDto<List<PedidoGenerico>> buscarClientePorPedido(String buscar, Integer lugarEntregaId, List<String> tipoPedido, List<String> estadoPedido, int size, int pageSize) {
        Pageable pageable = PageRequest.of(pageSize, size);
        boolean sinFiltroTipo = (tipoPedido == null || tipoPedido.isEmpty());
        // IN (:tipoPedido) necesita una lista no vacia siempre -- si no hay filtro, se manda un
        // dummy con los 3 tipos validos, pero sinFiltroTipo=true hace que el OR ya no dependa de el.
        List<String> tiposParaQuery = sinFiltroTipo
                ? List.of("NORMAL", "APARTADO", "FIADO")
                : tipoPedido;

        boolean sinFiltroEstado = (estadoPedido == null || estadoPedido.isEmpty());
        // estado_pedido esta guardado con mayusculas inconsistentes segun quien lo escribio
        // ('PAGADO', 'cancelado', 'Entregado'), asi que se compara en mayusculas de los dos lados
        // para que el front pueda mandar el valor como lo tenga a la mano.
        List<String> estadosParaQuery = sinFiltroEstado
                ? List.of("PAGADO")
                : estadoPedido.stream().map(e -> e.toUpperCase()).toList();
        Page<String> jsonList;
        if(buscar.isEmpty()){
            jsonList = iPedidoRepository.buscarTodosLosPedidos(lugarEntregaId, sinFiltroTipo, tiposParaQuery, sinFiltroEstado, estadosParaQuery, pageable);
        }else{
            jsonList = iPedidoRepository.buscarPedidosPorCliente(buscar, lugarEntregaId, sinFiltroTipo, tiposParaQuery, sinFiltroEstado, estadosParaQuery, pageable);
        }
        return getListPageableDto(jsonList);
    }


    @Transactional
    @Override
    public void deletePedidoById(int id, String motivo) {
        Pedido pedido = iPedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if ("cancelado".equals(pedido.getEstadoPedido())) {
            throw new RuntimeException("No se puede cancelar un pedido en estado: " + pedido.getEstadoPedido());
        }

        // Entregado (venta al contado) y PAGADO (credito ya liquidado) significan que la mercancia
        // y el dinero ya cambiaron de manos y que ya existe una Venta ligada al pedido -- cancelar
        // aqui es en realidad una devolucion, solo la puede registrar un ADMIN, y no se puede usar
        // un motivo de "no se presento" (eso afectaria el score de rifa de un cliente que si cumplio).
        boolean esDevolucion = "Entregado".equals(pedido.getEstadoPedido()) || "PAGADO".equals(pedido.getEstadoPedido());
        if (esDevolucion) {
            if (!AuthenticationUtils.isAdminContext()) {
                throw new RuntimeException("Solo un administrador puede cancelar un pedido que ya fue entregado o pagado");
            }
            if ("TIMEOUT".equals(motivo) || "NO_SE_PRESENTO".equals(motivo)) {
                throw new RuntimeException("Ese motivo es para pedidos que no se recogieron, no aplica para un pedido ya entregado");
            }
        }

        // FIADO activo ya entrego la mercancia al cliente (igual que en AbonoServiceImpl.cancelarPedido) --
        // no se le devuelve el stock solo por dejar de pagar, queda como deuda incobrable. Si ya es una
        // devolucion (PAGADO/Entregado) si se devuelve, porque el cliente esta regresando algo que ya tenia.
        boolean esFiadoActivo = "FIADO".equals(pedido.getTipoPedido()) && !esDevolucion;

        if (!esFiadoActivo) {
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
                    sincronizarStockColorFlor(variante);
                }
            });
        }

        pedido.setEstadoPedido("cancelado");
        pedido.setMotivoCancelacion(motivo);
        pedido.setFechaCancelacion(LocalDate.now());
        iPedidoRepository.save(pedido);
        notificarSeguimientoPedido(pedido);

        if (esDevolucion) {
            iVentaRepository.findByPedidoId(pedido.getId()).ifPresent(venta -> {
                venta.setEstadoVenta("Devuelta");
                iVentaRepository.save(venta);
            });
        }

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
                sincronizarStockColorFlor(variante);
            }
            iDetallePedidoRepository.delete(detalle);
            pedido.getDetalles().remove(detalle);
        } else {
            prod.setStock(prod.getStock() + cantidad);
            iProductoRepository.save(prod);
            if (detalle.getVariante() != null) {
                Variantes variante = iVarianteRepository.findById(detalle.getVariante().getId())
                        .orElseThrow(() -> new RuntimeException("Variante no encontrada al devolver stock"));
                variante.setStock(variante.getStock() + cantidad);
                iVarianteRepository.save(variante);
                sincronizarStockColorFlor(variante);
            }
            detalle.setCantidad(detalle.getCantidad() - cantidad);
            detalle.setSubTotal(detalle.getPrecioUnitario() * detalle.getCantidad());
            iDetallePedidoRepository.save(detalle);
        }

        // El detalle que se quito/redujo ya no debe seguir contando en el total del pedido --
        // sin este recalculo, totalPedido se quedaba con el valor de antes de la edicion.
        double nuevoTotal = pedido.getDetalles().stream().mapToDouble(DetallePedido::getSubTotal).sum();
        pedido.setTotalPedido(nuevoTotal);
        iPedidoRepository.save(pedido);

        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
    }

    @Override
    public PedidoDetalleResponse getDetallePedido(int id) {
        Pedido pedido = iPedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + id));

        // El detalle de un pedido solo lo puede ver su dueno o ADMIN -- mismo patron que
        // editarDatosEntrega() (encontrado junto con la misma IDOR en obtenerPedido/obtenerPedidoPorId).
        if (!AuthenticationUtils.isAdminContext()) {
            Cliente clienteActual = AuthenticationUtils.currentUsuario().getCliente();
            if (clienteActual == null || pedido.getCliente() == null
                    || !pedido.getCliente().getId().equals(clienteActual.getId())) {
                throw new RuntimeException("No puedes ver el detalle de un pedido que no es tuyo");
            }
        }

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
        resp.setFechaHoraRegistro(pedido.getFechaHoraRegistro() != null
                ? pedido.getFechaHoraRegistro()
                : (pedido.getFechaPedido() != null ? pedido.getFechaPedido().atStartOfDay() : null));
        resp.setFechaRecogida(pedido.getFechaRecogida());
        resp.setObservaciones(pedido.getObservaciones());
        resp.setMotivoCancelacion(pedido.getMotivoCancelacion());
        resp.setFechaCancelacion(pedido.getFechaCancelacion());
        resp.setNombreReceptor(pedido.getNombreReceptor());
        resp.setDireccionEntrega(pedido.getDireccionEntrega());
        resp.setLatitud(pedido.getLatitud());
        resp.setLongitud(pedido.getLongitud());
        resp.setReferencias(pedido.getReferencias());
        resp.setUrlFacebook(pedido.getUrlFacebook());
        if (pedido.getLugarEntrega() != null) {
            resp.setLugarEntregaId(pedido.getLugarEntrega().getId());
            resp.setLugarEntregaNombre(pedido.getLugarEntrega().getNombre());
        }

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

        // esRamoFlores / esLineaInterna: para que el front pueda distinguir el pedido de flores y
        // esconder/agrupar la linea del papel sin depender del nombre del producto (fragil).
        boolean esRamoFlores = !iRamoPedidoDetalleRepository.findByPedidoId(pedido.getId()).isEmpty();
        resp.setEsRamoFlores(esRamoFlores);
        Integer varianteIdPapel = esRamoFlores
                ? iAccesorioRamoRepository.findFirstByEsPapelTrueAndActivoTrue()
                        .map(a -> a.getVariante() != null ? a.getVariante().getId() : null)
                        .orElse(null)
                : null;

        List<DetalleItemResponse> detalles = pedido.getDetalles().stream().map(dp -> {
            DetalleItemResponse item = new DetalleItemResponse();
            item.setId(dp.getId());
            item.setCantidad(dp.getCantidad());
            item.setPrecioUnitario(dp.getPrecioUnitario());
            item.setSubTotal(dp.getSubTotal());
            if (dp.getProducto() != null) {
                item.setProductoId(dp.getProducto().getId());
                item.setProductoNombre(dp.getProducto().getNombre());
            }
            if (dp.getVariante() != null) {
                item.setVarianteId(dp.getVariante().getId());
                item.setTalla(dp.getVariante().getTalla());
                item.setColor(dp.getVariante().getColor());
                item.setDescripcion(dp.getVariante().getDescripcion());
                item.setEsLineaInterna(varianteIdPapel != null && varianteIdPapel.equals(dp.getVariante().getId()));
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

    // Edicion de solo los datos de entrega (quien recibe, direccion, fecha de entrega,
    // observaciones) -- no toca lineas ni stock, se puede llamar en cualquier momento despues
    // de creado el pedido (venta directa los manda opcionales; si quedan vacios, se completan
    // aqui despues).
    @Transactional
    @Override
    public PedidoDetalleResponse editarDatosEntrega(int id, EditarEntregaPedidoRequest requestG) {
        Pedido pedido = iPedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        // El cliente solo puede editar la entrega de SU propio pedido; ADMIN puede cualquiera.
        // Mismo patron que ResenaServiceImpl (dueno vs ADMIN) -- ver SecurityConfig, esta ruta
        // ya no exige ROLE_ADMIN, asi que la validacion de propiedad vive aqui.
        if (!AuthenticationUtils.isAdminContext()) {
            Cliente clienteActual = AuthenticationUtils.currentUsuario().getCliente();
            if (clienteActual == null || pedido.getCliente() == null
                    || !pedido.getCliente().getId().equals(clienteActual.getId())) {
                throw new RuntimeException("No puedes editar la entrega de un pedido que no es tuyo");
            }
        }

        if ("cancelado".equals(pedido.getEstadoPedido())) {
            throw new RuntimeException("No se pueden editar los datos de entrega de un pedido cancelado");
        }

        // fechaEntrega/lugarEntregaId aqui no recalculan nada de flores (fechaLimitePago,
        // cargoUrgenteMonto, esUrgente) -- un pedido de ramo podia cotizarse sin urgencia, pagarse,
        // y despues moverse a una fecha urgente por este endpoint sin cobrar el cargo. Reportado
        // por el front 2026-08-17 (ya bloqueado en su UI); aqui se bloquea tambien del lado del
        // back para que no se pueda saltar llamando el endpoint directo. Usar
        // PUT /v1/flores/pedidos/{id}/editar-ramo para mover la fecha de un ramo, que si recotiza.
        boolean tocaFechaOLugar = requestG.getFechaEntrega() != null || requestG.getLugarEntregaId() != null;
        if (tocaFechaOLugar && !iRamoPedidoDetalleRepository.findByPedidoId(id).isEmpty()) {
            throw new RuntimeException("Este pedido es un ramo de flores -- la fecha de entrega y el lugar no se "
                    + "pueden cambiar desde aqui porque no recalculan el cargo de urgencia. Para mover la fecha usa "
                    + "PUT /v1/flores/pedidos/" + id + "/editar-ramo (el lugar todavia no se puede cambiar en un "
                    + "ramo por ningun endpoint).");
        }

        if (requestG.getNombreReceptor() != null) {
            pedido.setNombreReceptor(requestG.getNombreReceptor());
        }
        if (requestG.getDireccionEntrega() != null) {
            pedido.setDireccionEntrega(requestG.getDireccionEntrega());
        }
        if (requestG.getLatitud() != null) {
            pedido.setLatitud(requestG.getLatitud());
        }
        if (requestG.getLongitud() != null) {
            pedido.setLongitud(requestG.getLongitud());
        }
        if (requestG.getReferencias() != null) {
            pedido.setReferencias(requestG.getReferencias());
        }
        if (requestG.getLugarEntregaId() != null) {
            pedido.setLugarEntrega(resolveLugarEntrega(requestG.getLugarEntregaId()));
        }
        if (requestG.getUrlFacebook() != null) {
            pedido.setUrlFacebook(requestG.getUrlFacebook());
        }
        if (requestG.getFechaEntrega() != null) {
            pedido.setFechaRecogida(requestG.getFechaEntrega());
        }
        if (requestG.getObservaciones() != null) {
            pedido.setObservaciones(requestG.getObservaciones());
        }

        iPedidoRepository.save(pedido);
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        return getDetallePedido(pedido.getId());
    }

    @Override
    public boolean notificarPedido(int id, NotificarPedidoRequest requestG) {
        if (iPedidoRepository.findById(id).isEmpty()) {
            throw new RuntimeException("Pedido no encontrado: " + id);
        }
        String asunto = "Comprobante de tu pedido #" + id + " — Novedades Jade";
        return emailService.enviarTicket(requestG.getCorreo(), asunto, requestG.getTicketHtml());
    }

    // Catalogo interno de pagos (pagos_y_meses) -- decidido 2026-09-03: los pagos online por
    // Checkout Pro de Mercado Pago y PayPal se registran bajo el mismo renglon "TARJETA" que ya
    // usan los abonos con tarjeta en persona (ver AbonoServiceImpl.PAGOS_TARJETA), en vez de
    // crear renglones nuevos por pasarela -- mas simple para arrancar, aunque los reportes no
    // distinguen tarjeta fisica de online por ahora.
    private static final int PAGOS_TARJETA_ONLINE = 2;

    /**
     * Confirma un pago online (Checkout Pro MP o PayPal) ya aprobado por la pasarela: marca el
     * pedido como PAGADO y genera la Venta -- mismo patron que
     * AbonoServiceImpl.crearVentaDesdePedido (sin descontar comision/tarifa, ese calculo es
     * exclusivo del flujo de Point/updatePedido). Se usa el Usuario ligado al propio Cliente
     * (Cliente.usuario) como responsable de la venta, porque aqui no hay ningun admin operando --
     * el pago lo completo el cliente solo, sin intervencion humana de por medio.
     * Idempotente: si el pedido ya esta PAGADO no vuelve a generar otra Venta (un webhook de MP/
     * PayPal puede reintentar la misma notificacion mas de una vez).
     */
    @Transactional
    public void confirmarPagoOnline(Integer pedidoId) {
        Pedido pedido = iPedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + pedidoId));

        if ("PAGADO".equals(pedido.getEstadoPedido()) || "Entregado".equals(pedido.getEstadoPedido())) {
            log.info("confirmarPagoOnline: pedido {} ya esta {}, no se genera otra venta",
                    pedidoId, pedido.getEstadoPedido());
            return;
        }
        if ("cancelado".equals(pedido.getEstadoPedido())) {
            log.warn("confirmarPagoOnline: pedido {} esta cancelado, se ignora la confirmacion de pago", pedidoId);
            return;
        }

        Cliente cliente = pedido.getCliente();
        if (cliente == null || cliente.getUsuario() == null) {
            log.error("confirmarPagoOnline: pedido {} sin cliente/usuario, no se puede generar la venta", pedidoId);
            return;
        }

        PagosYMeses pagosYMeses = iPagosYMesesRepository.findById(PAGOS_TARJETA_ONLINE)
                .orElseThrow(() -> new RuntimeException("Catálogo de pago no encontrado: " + PAGOS_TARJETA_ONLINE));

        List<DetalleVentaVariante> detallesVenta = new ArrayList<>();
        for (DetallePedido dp : pedido.getDetalles()) {
            double precioCosto = dp.getVariante() != null ? dp.getVariante().getProducto().getPrecioCosto() : 0.0;
            double subTotal = dp.getSubTotal();
            double ganancia = subTotal - (precioCosto * dp.getCantidad());

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

        double totalVenta = detallesVenta.stream().mapToDouble(DetalleVentaVariante::getSubTotal).sum();
        double gananciaTotal = detallesVenta.stream().mapToDouble(DetalleVentaVariante::getGanancia).sum();

        Venta venta = new Venta();
        venta.setEstadoVenta("Entregado");
        venta.setFechaVenta(LocalDateTime.now());
        venta.setUsuario(cliente.getUsuario());
        venta.setCliente(cliente);
        venta.setPagosYMeses(pagosYMeses);
        venta.setPedido(pedido);
        venta.setTotalVenta(totalVenta);
        venta.setGananciaTotal(gananciaTotal);
        detallesVenta.forEach(dvv -> dvv.setVenta(venta));
        venta.setDetalles(detallesVenta);
        iVentaRepository.save(venta);

        pedido.setEstadoPedido("PAGADO");
        pedido.setTotalPagado(pedido.getTotalPedido());
        iPedidoRepository.save(pedido);
        notificarSeguimientoPedido(pedido);
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");

        log.info("Pedido {} confirmado como PAGADO por pago online — total: {}", pedidoId, totalVenta);
    }

    /**
     * Correo de seguimiento ante un cambio de estado del pedido (confirmado/entregado o
     * cancelado). NO transaccional -- respeta Cliente.recibirCorreos, a diferencia del ticket de
     * compra (notificarPedido) que el cliente dispara explícitamente con un checkbox propio.
     * Nunca debe tumbar la transacción que confirma/cancela el pedido si el envío falla.
     */
    private void notificarSeguimientoPedido(Pedido pedido) {
        try {
            Cliente cliente = pedido.getCliente();
            if (cliente == null || !Boolean.TRUE.equals(cliente.getRecibirCorreos())) return;
            String correo = cliente.getCorreoElectronico();
            if (correo == null || correo.isBlank()) return;
            emailService.enviarSeguimientoPedido(correo, cliente.getNombrePersona(), pedido.getId(), pedido.getEstadoPedido());
        } catch (Exception e) {
            log.warn("No se pudo notificar seguimiento del pedido id={}: {}", pedido.getId(), e.getMessage());
        }
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
