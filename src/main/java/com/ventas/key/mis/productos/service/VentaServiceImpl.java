package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.exeption.ExceptionSinPermiso;
import com.ventas.key.mis.productos.exeption.ExceptionOperacionNoPermitida;
import com.ventas.key.hexagonal.articulo.dominio.modelo.ArticuloAVender;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.LineaPorCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.PrecioDeGrupo;
import com.ventas.key.hexagonal.preciocantidad.dominio.puerto.entrada.CalcularPreciosPorCantidadCasoUso;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.dto.ClienteSinRegistroDto;
import com.ventas.key.mis.productos.entity.*;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.models.NotificacionRequest;
import com.ventas.key.mis.productos.models.VentaDirectaResponse;
import com.ventas.key.mis.productos.repository.*;
import com.ventas.key.mis.productos.config.RabbitMQConfig;
import java.util.LinkedHashMap;
import java.util.Map;
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

@Slf4j
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
    private final IPromocionRepository iPromocionRepository;
    private final PromocionServiceImpl promocionService;
    private final ErrorGenerico errorGenerico;

    @Autowired private CacheService cacheService;
    @Autowired private BloqueoStock bloqueoStock;
    /** Precio por cantidad (grupos). Opcional: sin el, todo se cobra como antes. */
    @Autowired(required = false) private CalcularPreciosPorCantidadCasoUso preciosPorCantidad;
    /** El precio de antes de las lineas cobradas con precio de grupo (para regresarlas si el pedido se edita). */
    @Autowired(required = false) private com.ventas.key.hexagonal.preciocantidad.dominio.puerto.salida.PreciosBaseDeLineaPort preciosBaseDeLinea;
    @Autowired(required = false) private com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ParticiparEnCampanaCasoUso campanas;
    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private EmailService emailService;
    @Autowired private WhatsappService whatsappService;
    @Autowired private ILugarEntregaRepository iLugarEntregaRepository;
    // Con el modulo nuevo de tarjeta encendido, la venta con tarjeta NO se da por pagada al crearla: queda un pedido
    // Pendiente y es el cobro aprobado el que lo marca Pagado y crea la Venta. Apagado = comportamiento de siempre.
    @org.springframework.beans.factory.annotation.Value("${pagos.tarjeta.habilitado:false}")
    private boolean pagoTarjetaHabilitado;

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
            final IPromocionRepository iPromocionRepository,
            final PromocionServiceImpl promocionService,
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
        this.iPromocionRepository = iPromocionRepository;
        this.promocionService = promocionService;
    }

    // lugarEntregaId es opcional (null = no se captura el lugar de entrega en esta venta)
    private LugarEntrega resolveLugarEntrega(Integer lugarEntregaId) {
        if (lugarEntregaId == null) return null;
        return iLugarEntregaRepository.findById(lugarEntregaId)
                .orElseThrow(() -> new RuntimeException("Lugar de entrega no encontrado: " + lugarEntregaId));
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

        // QA 2026-10-08: se guardaban nombres como "a". Opcional, pero si viene necesita 3 letras.
        String receptor = request.getNombreReceptor();
        if (receptor != null && !receptor.isBlank() && receptor.codePoints().filter(Character::isLetter).count() < 3) {
            throw new ExceptionErrorInesperado("El nombre de quien recibe debe tener al menos 3 letras (o déjalo vacío)");
        }

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

        boolean tieneClienteSinRegistroId = request.getClienteSinRegistroId() != null;
        boolean esSinRegistro = tieneClienteSinRegistroId
                || (request.getClienteSinRegistroDto() != null
                    && !request.getClienteSinRegistroDto().getNombre_persona().isBlank());

        if (tieneClienteSinRegistroId) {
            // Flujo nuevo: el registro ya se creo (y opcionalmente se verifico) antes de esta
            // llamada, via POST /v1/clientes-sin-registro — aqui solo se enlaza a la venta.
            clienteSinRegistro = iClienteSinRegistroRepository.findById(request.getClienteSinRegistroId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Cliente sin registro no encontrado"));
        } else if (esSinRegistro) {
            try {
                ClienteSinRegistroImpl.validar(request.getClienteSinRegistroDto());
            } catch (RuntimeException e) {
                throw new ExceptionErrorInesperado(e.getMessage());
            }
            clienteSinRegistro = iClienteSinRegistroRepository.save(mapperClienteSinRegistroDto(request));
        } else {
            cliente = iClienteRepository.findById(request.getClienteId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Cliente no encontrado"));
        }

        // Procesar items y construir las dos listas de detalle
        List<DetallePedido> detallesPedido = new ArrayList<>();
        List<DetalleVentaVariante> detallesVenta = new ArrayList<>();

        // Precio por cantidad: con TODO lo que lleva la venta (las piezas solo se suman dentro del mismo grupo).
        Map<Integer, PrecioDeGrupo> preciosDeGrupo = preciosDeGrupoDeLaVenta(request.getDetalles());
        // Campaña vigente: el back decide quién es recurrente y qué juguetes bajan; Ir pagando nunca lleva precio de campaña.
        var decisionCampana = decidirCampana(request, cliente, clienteSinRegistro, tipoPedido);

        for (var item : request.getDetalles()) {
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new RuntimeException("La cantidad es obligatoria y debe ser mayor a 0 para el artículo id "
                        + item.getVarianteId());
            }

            Variantes variante = iVarianteRepository.findByIdWithLock(item.getVarianteId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Artículo no encontrado: " + item.getVarianteId()));

            // El producto vino por EAGER sin bloqueo (y en MySQL, de la foto de la transaccion): se bloquea y se relee.
            Producto prod = bloqueoStock.bloquear(variante.getProducto());

            ArticuloAVender.deArticulo(
                    ArticuloAVender.nombreVisible(prod.getNombre(), variante.getTalla(), variante.getColor()),
                    prod.getHabilitado() == '1', prod.getStock() != null ? prod.getStock() : 0,
                    variante.getHabilitado() == '1', variante.getStock())
                    .exigirQueSePuedaVender(item.getCantidad());

            variante.setStock(variante.getStock() - item.getCantidad());
            iVarianteRepository.save(variante);

            // Lineas sin promocionId deben cobrarse al precio de catalogo — un descuento en
            // mostrador ya tiene su via oficial (Promociones); sin este chequeo, el request
            // podia traer cualquier precio.
            double precioLinea = item.getPrecioVenta() == null ? 0.0 : item.getPrecioVenta();
            com.ventas.key.hexagonal.preciocantidad.dominio.modelo.OrigenDePrecio origenDescuento = null;
            if (item.getPromocionId() == null) {
                PrecioDeGrupo grupo = preciosDeGrupo.get(prod.getId());
                java.math.BigDecimal precioCampana = decisionCampana.precioDe(prod.getId());
                validarPrecioCatalogo(prod, variante, item.getPrecioVenta(), grupo, precioCampana);
                // Si la venta junta el minimo del grupo, se cobra su precio de grupo aunque el front mande el normal (gana el mas bajo).
                precioLinea = precioAplicado(precioLinea, grupo);
                // Precio de campana (solo recurrente y solo Contado/Apartado): tambien gana el mas bajo. Si el front ya mando el precio bajado
                // (el de grupo o el de campana), igual se registra el "antes" de catalogo.
                double catalogo = precioDeCatalogo(prod, variante);
                if (precioCampana != null && precioCampana.doubleValue() <= precioLinea + 0.005) {
                    precioLinea = precioCampana.doubleValue();
                    if (precioLinea < catalogo - 0.005) {
                        origenDescuento = com.ventas.key.hexagonal.preciocantidad.dominio.modelo.OrigenDePrecio.CAMPANA;
                    }
                } else if (grupo != null && precioLinea <= grupo.precio().doubleValue() + 0.005 && precioLinea < catalogo - 0.005) {
                    origenDescuento = com.ventas.key.hexagonal.preciocantidad.dominio.modelo.OrigenDePrecio.GRUPO;
                }
            }

            prod.setStock(prod.getStock() - item.getCantidad());
            iRepository.save(prod);

            double precioCosto = prod.getPrecioCosto();
            // El subtotal se CALCULA, no se lee del request. El precio unitario si viene del
            // front, pero pasa por validarPrecioCatalogo (linea sin promocion) o por
            // validarLineasDePromocion (linea con promocion) antes de que la transaccion
            // confirme. El subtotal, en cambio, no lo validaba nadie: con promocionId el
            // request podia traer el precio unitario correcto y un subTotal de 1, y como el
            // total de la venta se arma sumando subtotales, la venta entera quedaba en 1.
            double subTotal    = precioLinea * item.getCantidad();
            double costoTotal  = precioCosto * item.getCantidad();
            double comision    = subTotal * (tasaTarifa + tasaIva);
            double ganancia    = subTotal - costoTotal - comision;

            Promocion promocion = null;
            if (item.getPromocionId() != null) {
                promocion = iPromocionRepository.findById(item.getPromocionId())
                        .orElseThrow(() -> new RuntimeException("La promocion ya no esta disponible"));
            }

            DetallePedido dp = new DetallePedido();
            dp.setProducto(prod);
            dp.setVariante(variante);
            dp.setCantidad(item.getCantidad());
            dp.setPrecioUnitario(precioLinea);
            // Si la linea se cobra con precio de grupo o de campana, se recuerda el que tenia antes (se registra al guardar el pedido).
            // El "antes" es el de catalogo aunque el front ya mande el precio bajado.
            if (origenDescuento != null) {
                dp.setPrecioAntesDeGrupo(Math.max(item.getPrecioVenta(), precioDeCatalogo(prod, variante)));
                dp.setOrigenDelDescuento(origenDescuento);
            }
            dp.setSubTotal(subTotal);
            dp.setPromocion(promocion);
            detallesPedido.add(dp);

            DetalleVentaVariante dvv = new DetalleVentaVariante();
            dvv.setVariante(variante);
            dvv.setCantidad(item.getCantidad());
            dvv.setPrecioUnitario(precioLinea);
            dvv.setSubTotal(subTotal);
            dvv.setPrecioCosto(precioCosto);
            dvv.setGanancia(ganancia);
            dvv.setFechaVenta(LocalDate.now());
            dvv.setPromocion(promocion);
            detallesVenta.add(dvv);
        }

        validarLineasDePromocion(detallesPedido);

        double totalPedidoCalc = detallesPedido.stream().mapToDouble(DetallePedido::getSubTotal).sum();

        if (esCredito) {
            Pedido pedido = new Pedido();
            pedido.setTipoPedido(tipoPedido);
            pedido.setEstadoPedido(tipoPedido);
            pedido.setCliente(cliente);
            pedido.setClienteSinRegistro(clienteSinRegistro);
            pedido.setObservaciones(request.getObservaciones() != null ? request.getObservaciones() : "");
            pedido.setNombreReceptor(request.getNombreReceptor());
            pedido.setDireccionEntrega(request.getDireccionEntrega());
            pedido.setLatitud(request.getLatitud());
            pedido.setLongitud(request.getLongitud());
            pedido.setReferencias(request.getReferencias());
            pedido.setLugarEntrega(resolveLugarEntrega(request.getLugarEntregaId()));
            pedido.setUrlFacebook(request.getUrlFacebook());
            pedido.setFechaPedido(LocalDate.now());
            pedido.setFechaHoraRegistro(LocalDateTime.now());
            pedido.setFechaRecogida(request.getFechaEntrega());
            pedido.setTotalPedido(totalPedidoCalc);
            pedido.setTotalPagado(0.0);
            // Ir pagando se lo lleva al crearse salvo que digan que no; un Apartado nunca (E3, E8).
            pedido.marcarEntregado("FIADO".equals(tipoPedido) && !Boolean.FALSE.equals(request.getEntregado()));
            detallesPedido.forEach(dp -> dp.setPedido(pedido));
            pedido.setDetalles(detallesPedido);
            Pedido savedPedido = iPedidoRepository.save(pedido);
            recordarPreciosAntesDeGrupo(detallesPedido);
            registrarCompraDeCampana(decisionCampana, savedPedido.getId(), "FIADO".equals(tipoPedido)
                    ? com.ventas.key.hexagonal.campana.dominio.modelo.FormaDeCobro.IR_PAGANDO : com.ventas.key.hexagonal.campana.dominio.modelo.FormaDeCobro.APARTADO);
            cacheService.evictAll();
            try {
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
            } catch (Exception e) {
                log.warn("No se pudo avisar a Rabbit para invalidar cache (no bloquea la operacion): {}", e.getMessage());
            }
            VentaDirectaResponse respCredito = new VentaDirectaResponse(
                    null, null, false, totalPedidoCalc, null, null, null, savedPedido.getId(),
                    null, null, null);
            enviarNotificacionesVenta(request.getNotificacion(), cliente, clienteSinRegistro,
                    "Pedido registrado — Novedades Jade", respCredito);
            respCredito.setRenglones(renglonesDelTicket(detallesPedido));
            return respCredito;
        }

        if (pagoTarjetaHabilitado && requiereTerminal(pagosYMeses, mesesIntereses)) {
            return crearPedidoPendienteParaCobro(request, cliente, clienteSinRegistro, detallesPedido,
                    totalPedidoCalc, pagosYMeses, mesesIntereses, decisionCampana);
        }

        double totalVenta    = detallesVenta.stream().mapToDouble(DetalleVentaVariante::getSubTotal).sum();
        double gananciaTotal = detallesVenta.stream().mapToDouble(DetalleVentaVariante::getGanancia).sum();

        // Crear y guardar Pedido (siempre, para todos los escenarios)
        Pedido pedido = new Pedido();
        pedido.setEstadoPedido("Entregado");
        // Venta de contado en el local: se lo lleva en ese momento salvo que digan que no (E4).
        pedido.marcarEntregado(!Boolean.FALSE.equals(request.getEntregado()));
        pedido.setCliente(cliente);
        pedido.setClienteSinRegistro(clienteSinRegistro);
        pedido.setObservaciones(request.getObservaciones() != null ? request.getObservaciones() : "");
        pedido.setNombreReceptor(request.getNombreReceptor());
        pedido.setDireccionEntrega(request.getDireccionEntrega());
        pedido.setLatitud(request.getLatitud());
        pedido.setLongitud(request.getLongitud());
        pedido.setReferencias(request.getReferencias());
        pedido.setLugarEntrega(resolveLugarEntrega(request.getLugarEntregaId()));
        pedido.setUrlFacebook(request.getUrlFacebook());
        pedido.setFechaPedido(LocalDate.now());
        pedido.setFechaHoraRegistro(LocalDateTime.now());
        pedido.setFechaRecogida(request.getFechaEntrega() != null ? request.getFechaEntrega() : LocalDate.now());
        pedido.setTotalPedido(totalVenta);
        pedido.setTotalPagado(totalVenta);
        detallesPedido.forEach(dp -> dp.setPedido(pedido));
        pedido.setDetalles(detallesPedido);
        iPedidoRepository.save(pedido);
        recordarPreciosAntesDeGrupo(detallesPedido);
        registrarCompraDeCampana(decisionCampana, pedido.getId(), com.ventas.key.hexagonal.campana.dominio.modelo.FormaDeCobro.CONTADO);

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
        venta.setMontoDado(request.getMontoDado());
        detallesVenta.forEach(dvv -> dvv.setVenta(venta));
        venta.setDetalles(detallesVenta);

        // Venta hecha con ClienteSinRegistro (no se capturó al cliente real en el momento):
        // se genera un código para que el cliente la reclame después desde su cuenta y así
        // aparezca en /concursante/clientesPorMes al armar la rifa.
        String codigoReclamo = null;
        if (esSinRegistro && clienteSinRegistro.getCorreoElectronico() != null
                && !clienteSinRegistro.getCorreoElectronico().isBlank()) {
            codigoReclamo = UUID.randomUUID().toString();
            venta.setCodigoReclamo(codigoReclamo);
            venta.setCorreoReclamo(clienteSinRegistro.getCorreoElectronico());
        }

        boolean requiereTerminal = requiereTerminal(pagosYMeses, mesesIntereses);
        Venta saved = iVentaRepository.save(venta);
        if (codigoReclamo != null) {
            emailService.enviarCodigoReclamoVenta(clienteSinRegistro.getCorreoElectronico(), codigoReclamo);
        }
        cacheService.evictAll();
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        } catch (Exception e) {
            log.warn("No se pudo avisar a Rabbit para invalidar cache (no bloquea la operacion): {}", e.getMessage());
        }
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
        respVenta.setRenglones(renglonesDelTicket(detallesPedido));
        return respVenta;
    }

    /**
     * Efectivo nunca requiere terminal, sin importar el tarifaTerminal que tenga enlazado su mesesIntereses -- antes se
     * decidia SOLO con "tarifaTerminal.getId() != 3", un id fijo que asumia que la fila "sin terminal" de
     * tarifa_terminal siempre cae en el id 3. Eso depende del orden en que se sembro esa tabla en cada ambiente (no hay
     * migracion que la siembre igual en dev/qa/prod) -- si en QA la fila "N/A" no quedo en el id 3, "Efectivo"
     * terminaba pidiendo terminal de Mercado Pago igual (reportado en QA 2026-09-02: "elegi efectivo pero marca que por
     * mercado"). formaPago ya es la etiqueta real que el admin le puso a esta forma de pago, no depende de ningun id.
     */
    private static boolean requiereTerminal(PagosYMeses pagosYMeses, MesesIntereses mesesIntereses) {
        boolean esEfectivo = pagosYMeses.getTipoPago() != null
                && "efectivo".equalsIgnoreCase(
                        pagosYMeses.getTipoPago().getFormaPago() == null ? null : pagosYMeses.getTipoPago().getFormaPago().trim());
        return !esEfectivo
                && mesesIntereses.getTarifaTerminal() != null && mesesIntereses.getTarifaTerminal().getId() != 3;
    }

    /**
     * Venta con tarjeta con el modulo nuevo: solo se crea el pedido (Pendiente, sin dinero, sin entregar y SIN Venta).
     * Si la tarjeta se rechaza o se cancela no queda nada pagado; al aprobarse, PedidoCobroAdapter.marcarPagado lo
     * deja Pagado y Entregado y crea la Venta. El stock ya se descontó arriba (igual que un pedido del cliente).
     */
    private VentaDirectaResponse crearPedidoPendienteParaCobro(VentaDirectaRequest request, Cliente cliente,
            ClienteSinRegistro clienteSinRegistro, List<DetallePedido> detallesPedido, double total,
            PagosYMeses pagosYMeses, MesesIntereses mesesIntereses,
            com.ventas.key.hexagonal.campana.dominio.modelo.DecisionDeCampana decisionCampana) {
        Pedido pedido = new Pedido();
        pedido.setTipoPedido("NORMAL");
        pedido.setEstadoPedido("Pendiente");
        pedido.marcarEntregado(false);
        pedido.setCliente(cliente);
        pedido.setClienteSinRegistro(clienteSinRegistro);
        pedido.setObservaciones(request.getObservaciones() != null ? request.getObservaciones() : "");
        pedido.setNombreReceptor(request.getNombreReceptor());
        pedido.setDireccionEntrega(request.getDireccionEntrega());
        pedido.setLatitud(request.getLatitud());
        pedido.setLongitud(request.getLongitud());
        pedido.setReferencias(request.getReferencias());
        pedido.setLugarEntrega(resolveLugarEntrega(request.getLugarEntregaId()));
        pedido.setUrlFacebook(request.getUrlFacebook());
        pedido.setFechaPedido(LocalDate.now());
        pedido.setFechaHoraRegistro(LocalDateTime.now());
        pedido.setFechaRecogida(request.getFechaEntrega() != null ? request.getFechaEntrega() : LocalDate.now());
        pedido.setTotalPedido(total);
        pedido.setTotalPagado(0.0);
        detallesPedido.forEach(dp -> dp.setPedido(pedido));
        pedido.setDetalles(detallesPedido);
        Pedido guardado = iPedidoRepository.save(pedido);
        recordarPreciosAntesDeGrupo(detallesPedido);
        registrarCompraDeCampana(decisionCampana, guardado.getId(), com.ventas.key.hexagonal.campana.dominio.modelo.FormaDeCobro.CONTADO);
        cacheService.evictAll();
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        } catch (Exception e) {
            log.warn("No se pudo avisar a Rabbit para invalidar cache (no bloquea la operacion): {}", e.getMessage());
        }
        VentaDirectaResponse resp = new VentaDirectaResponse(
                null, pagosYMeses.getTipoPago().getFormaPago(), true, total, null,
                mesesIntereses.getDescripcion(), null, guardado.getId(), null, null, null);
        resp.setCobroConModuloNuevo(true);
        resp.setRenglones(renglonesDelTicket(detallesPedido));
        return resp;
    }

    // El precio/subtotal que manda el request en una linea normal (sin promocionId) debe
    // coincidir con el precio real del producto — evita que la venta directa acepte un precio
    // arbitrario fuera de una promocion. Tolerancia de 1 centavo por redondeo de Double.
    /**
     * Una linea sin promocion se cobra a uno de los dos precios del catalogo: el normal o el de
     * rebaja. Nada mas.
     *
     * <p>Hasta el 2026-09-22 solo se aceptaba el normal, asi que para venderle mas barato a
     * alguien habia que armarle una promocion -- mas trabajo, y quedaba registrada una promocion
     * que nunca existio. El campo {@code precio_rebaja} ya existia: se capturaba y se mostraba en
     * el admin, pero no habia forma de cobrarlo.
     *
     * <p>Sigue sin poder mandarse un precio arbitrario: los dos valores salen del catalogo, asi
     * que el front elige entre ellos pero no inventa ninguno. El subtotal tampoco se valida
     * porque ya no se usa el del request -- se calcula (ver mas arriba).
     */
    private void validarPrecioCatalogo(Producto prod, Variantes variante, Double precioUnitario) {
        validarPrecioCatalogo(prod, variante, precioUnitario, null);
    }

    private void validarPrecioCatalogo(Producto prod, Variantes variante, Double precioUnitario, PrecioDeGrupo grupo) {
        validarPrecioCatalogo(prod, variante, precioUnitario, grupo, null);
    }

    private void validarPrecioCatalogo(Producto prod, Variantes variante, Double precioUnitario, PrecioDeGrupo grupo,
                                       java.math.BigDecimal precioCampana) {
        if (precioUnitario == null) {
            throw new RuntimeException("Falta el precio de " + prod.getNombre());
        }
        // El articulo puede tener precio propio (2026-09-29); si no, son los del producto.
        Double precioNormal = variante != null ? variante.precioNormal() : prod.getPrecioVenta();
        Double precioDescuento = variante != null ? variante.precioDescuento() : prod.getPrecioRebaja();
        double normal = precioNormal != null ? precioNormal : 0.0;
        double rebaja = precioDescuento != null ? precioDescuento : 0.0;

        boolean esNormal = Math.abs(precioUnitario - normal) <= 0.01;
        // Una rebaja en 0 significa "este producto no tiene rebaja", no "sale gratis".
        boolean esRebaja = rebaja > 0 && Math.abs(precioUnitario - rebaja) <= 0.01;
        // El precio de grupo solo es valido si la venta junto el minimo del grupo (la lista de grupos viene calculada del back).
        boolean esGrupo = grupo != null && Math.abs(precioUnitario - grupo.precio().doubleValue()) <= 0.01;

        // El precio de campana solo es valido si el back decidio que esta venta lo lleva (cliente recurrente, campana vigente, Contado o Apartado).
        boolean esCampana = precioCampana != null && Math.abs(precioUnitario - precioCampana.doubleValue()) <= 0.01;

        if (!esNormal && !esRebaja && !esGrupo && !esCampana) {
            String validos = rebaja > 0
                    ? String.format("%.2f (normal) o %.2f (rebaja)", normal, rebaja)
                    : String.format("%.2f", normal);
            if (grupo != null) {
                validos += String.format(" o %.2f (precio de grupo)", grupo.precio().doubleValue());
            }
            if (precioCampana != null) {
                validos += String.format(" o %.2f (precio de campana)", precioCampana.doubleValue());
            }
            throw new RuntimeException(String.format(
                    "El precio de %s no es valido: llego %.2f y los precios de catalogo son %s",
                    prod.getNombre(), precioUnitario, validos));
        }
    }

    /** Los precios de grupo que le tocan a esta venta, por producto. Las lineas de una promocion (combo) no cuentan. */
    private Map<Integer, PrecioDeGrupo> preciosDeGrupoDeLaVenta(List<com.ventas.key.mis.productos.models.DetalleVentaDto> detalles) {
        if (preciosPorCantidad == null || detalles == null) {
            return Map.of();
        }
        List<LineaPorCantidad> lineas = new ArrayList<>();
        for (var item : detalles) {
            if (item.getPromocionId() != null || item.getVarianteId() == null || item.getCantidad() == null || item.getCantidad() <= 0) {
                continue;
            }
            iVarianteRepository.findById(item.getVarianteId())
                    .ifPresent(v -> lineas.add(new LineaPorCantidad(v.getProducto().getId(), item.getCantidad())));
        }
        return preciosPorCantidad.calcular(lineas);
    }

    /** Los renglones tal como los cobro el back, para que el ticket muestre el precio normal y el de campana sin adivinar nada. */
    private static List<VentaDirectaResponse.Renglon> renglonesDelTicket(List<DetallePedido> detalles) {
        List<VentaDirectaResponse.Renglon> lista = new ArrayList<>();
        for (DetallePedido d : detalles) {
            VentaDirectaResponse.Renglon r = new VentaDirectaResponse.Renglon();
            String base = d.getProducto() != null ? d.getProducto().getNombre() : "Artículo";
            r.setNombre(d.getVariante() != null
                    ? ArticuloAVender.nombreVisible(base, d.getVariante().getTalla(), d.getVariante().getColor()) : base);
            r.setCantidad(d.getCantidad());
            r.setPrecioCobrado(d.getPrecioUnitario());
            r.setPrecioNormal(d.getPrecioAntesDeGrupo() != null ? d.getPrecioAntesDeGrupo() : d.getPrecioUnitario());
            boolean campana = d.getOrigenDelDescuento() == com.ventas.key.hexagonal.preciocantidad.dominio.modelo.OrigenDePrecio.CAMPANA;
            boolean grupo = d.getPrecioAntesDeGrupo() != null && !campana;
            r.setConDescuentoDeCampana(campana ? Boolean.TRUE : null);
            r.setConDescuentoDeGrupo(grupo ? Boolean.TRUE : null);
            lista.add(r);
        }
        return lista;
    }

    /** El precio mas bajo "de catalogo" del articulo: la rebaja si existe y es menor, si no el normal. */
    private static double precioDeCatalogo(Producto prod, Variantes variante) {
        Double normal = variante != null ? variante.precioNormal() : prod.getPrecioVenta();
        Double rebaja = variante != null ? variante.precioDescuento() : prod.getPrecioRebaja();
        double n = normal != null ? normal : 0.0;
        double r = rebaja != null ? rebaja : 0.0;
        return r > 0 && r < n ? r : n;
    }

    /**
     * Pregunta a la campana vigente que juguetes bajan en esta venta. Sin campana (o sin las tablas) devuelve NINGUNA y la venta sigue igual.
     * Una falla de la campana nunca tira la venta: se registra y se cobra normal.
     */
    private com.ventas.key.hexagonal.campana.dominio.modelo.DecisionDeCampana decidirCampana(
            VentaDirectaRequest request, Cliente cliente, ClienteSinRegistro sinRegistro, String tipoPedido) {
        var ninguna = com.ventas.key.hexagonal.campana.dominio.modelo.DecisionDeCampana.NINGUNA;
        if (campanas == null) {
            return ninguna;
        }
        try {
            var cobro = "FIADO".equals(tipoPedido) ? com.ventas.key.hexagonal.campana.dominio.modelo.FormaDeCobro.IR_PAGANDO
                    : "APARTADO".equals(tipoPedido) ? com.ventas.key.hexagonal.campana.dominio.modelo.FormaDeCobro.APARTADO
                    : com.ventas.key.hexagonal.campana.dominio.modelo.FormaDeCobro.CONTADO;
            var lineas = new ArrayList<com.ventas.key.hexagonal.campana.dominio.modelo.SolicitudDeCampana.Linea>();
            for (var item : request.getDetalles()) {
                if (item.getPromocionId() != null || item.getVarianteId() == null || item.getCantidad() == null || item.getCantidad() <= 0) {
                    continue;
                }
                var v = iVarianteRepository.findById(item.getVarianteId()).orElse(null);
                if (v != null) {
                    lineas.add(new com.ventas.key.hexagonal.campana.dominio.modelo.SolicitudDeCampana.Linea(v.getProducto().getId(),
                            item.getCantidad(), java.math.BigDecimal.valueOf(precioDeCatalogo(v.getProducto(), v))));
                }
            }
            var c = request.getCampana();
            String nombre = cliente != null ? String.join(" ", Optional.ofNullable(cliente.getNombrePersona()).orElse(""),
                    Optional.ofNullable(cliente.getApeidoPaterno()).orElse("")).trim()
                    : sinRegistro != null ? String.join(" ", Optional.ofNullable(sinRegistro.getNombrePersona()).orElse(""),
                    Optional.ofNullable(sinRegistro.getApeidoPaterno()).orElse("")).trim() : "";
            String correo = cliente != null ? cliente.getCorreoElectronico() : sinRegistro != null ? sinRegistro.getCorreoElectronico() : null;
            String telefono = cliente != null ? cliente.getNumeroTelefonico() : sinRegistro != null ? sinRegistro.getNumeroTelefonico() : null;
            return campanas.decidir(new com.ventas.key.hexagonal.campana.dominio.modelo.SolicitudDeCampana(
                    c == null ? null : c.getCodigo(), c != null && c.isRecurrenteManual(), c != null && c.isAceptaPrivacidad(), cobro,
                    new com.ventas.key.hexagonal.campana.dominio.modelo.ContactoCliente(nombre, correo, telefono,
                            cliente != null ? cliente.getId() : null, sinRegistro != null ? sinRegistro.getId() : null), lineas));
        } catch (RuntimeException e) {
            log.warn("La campana no pudo decidir precios; se cobra normal: {}", e.getMessage());
            return ninguna;
        }
    }

    /** Al cerrar la venta: liga el pedido a la persona y le manda su codigo. Nunca hace caer la venta. */
    private void registrarCompraDeCampana(com.ventas.key.hexagonal.campana.dominio.modelo.DecisionDeCampana decision, Integer pedidoId,
                                          com.ventas.key.hexagonal.campana.dominio.modelo.FormaDeCobro cobro) {
        if (campanas == null || decision == null || !decision.hayCampana() || pedidoId == null) {
            return;
        }
        try {
            campanas.registrarCompra(decision, pedidoId, cobro);
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar la compra en la campana del pedido {}: {}", pedidoId, e.getMessage());
        }
    }

    /** Registra el precio de antes de las lineas cobradas con precio de grupo. Va DESPUES de guardar el pedido: hasta entonces las lineas no tienen id. */
    private void recordarPreciosAntesDeGrupo(List<DetallePedido> detalles) {
        if (preciosBaseDeLinea == null || detalles == null) {
            return;
        }
        Map<Integer, java.math.BigDecimal> grupo = new LinkedHashMap<>();
        Map<Integer, java.math.BigDecimal> campana = new LinkedHashMap<>();
        for (DetallePedido d : detalles) {
            if (d.getId() != null && d.getPrecioAntesDeGrupo() != null) {
                var destino = d.getOrigenDelDescuento() == com.ventas.key.hexagonal.preciocantidad.dominio.modelo.OrigenDePrecio.CAMPANA ? campana : grupo;
                destino.put(d.getId(), java.math.BigDecimal.valueOf(d.getPrecioAntesDeGrupo()));
            }
        }
        preciosBaseDeLinea.guardar(grupo, com.ventas.key.hexagonal.preciocantidad.dominio.modelo.OrigenDePrecio.GRUPO);
        if (!campana.isEmpty()) {
            preciosBaseDeLinea.guardar(campana, com.ventas.key.hexagonal.preciocantidad.dominio.modelo.OrigenDePrecio.CAMPANA);
        }
    }

    /** Gana el precio mas bajo: el de grupo solo se usa si es menor al que llego. */
    private static double precioAplicado(double precio, PrecioDeGrupo grupo) {
        return grupo != null && grupo.precio().doubleValue() < precio - 0.005 ? grupo.precio().doubleValue() : precio;
    }

    // Agrupa las lineas de la venta que traen promocionId y valida cada combo contra
    // PromocionServiceImpl (vigencia, precios, y que la venta sea de contado).
    private void validarLineasDePromocion(List<DetallePedido> detallesPedido) {
        Map<Integer, List<PromocionServiceImpl.LineaPromocionCheck>> lineasPorPromocion = new LinkedHashMap<>();
        for (DetallePedido dp : detallesPedido) {
            if (dp.getPromocion() != null) {
                lineasPorPromocion.computeIfAbsent(dp.getPromocion().getId(), k -> new ArrayList<>())
                        .add(new PromocionServiceImpl.LineaPromocionCheck(
                                dp.getVariante().getId(), dp.getCantidad(), dp.getPrecioUnitario()));
            }
        }
        for (var entry : lineasPorPromocion.entrySet()) {
            promocionService.validarLineasPromocion(entry.getKey(), entry.getValue());
        }
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

    // El cliente reclama una venta hecha con ClienteSinRegistro capturando el código que se le
    // envió por correo. Se exige que coincida con el correo de su cuenta (defensa extra por si
    // el código se reenvía/filtra), que no se haya usado ya (reclamadoEn != null), y que no haya
    // expirado (solo vale dentro del mes calendario de la venta, no N días desde la compra).
    @Transactional
    public Venta reclamarVenta(String codigo) throws Exception {
        if (codigo == null || codigo.isBlank()) {
            throw new ExceptionErrorInesperado("Debes indicar el código");
        }
        Venta venta = iVentaRepository.findByCodigoReclamo(codigo.trim())
                .orElseThrow(() -> new ExceptionDataNotFound("Código inválido"));
        if (venta.getReclamadoEn() != null) {
            throw new ExceptionOperacionNoPermitida("Este código ya fue utilizado");
        }

        LocalDateTime finDeMes = YearMonth.from(venta.getFechaVenta()).atEndOfMonth().atTime(23, 59, 59);
        if (LocalDateTime.now().isAfter(finDeMes)) {
            throw new ExceptionOperacionNoPermitida("Este código ya expiró: solo es válido durante el mes de tu compra");
        }

        Usuario usuario = AuthenticationUtils.currentUsuario();
        Cliente cliente = usuario.getCliente();
        if (cliente == null) {
            throw new ExceptionOperacionNoPermitida("Tu cuenta todavía no tiene un perfil de cliente completo");
        }
        if (venta.getCorreoReclamo() == null
                || !venta.getCorreoReclamo().equalsIgnoreCase(cliente.getCorreoElectronico())) {
            throw new ExceptionSinPermiso("El correo de tu cuenta no coincide con el de esta compra");
        }

        return vincularClienteAVenta(venta, cliente);
    }

    // Fallback para el admin: si el cliente nunca reclama el UUID por correo (se fue a spam, no
    // quiso loguearse, etc.), el admin puede vincular la venta manualmente buscando al cliente.
    // No requiere el código porque el admin ya validó identidad al elegir al cliente.
    @Transactional
    public Venta asignarClienteManual(Integer ventaId, Integer clienteId) throws Exception {
        Venta venta = iVentaRepository.findById(ventaId)
                .orElseThrow(() -> new ExceptionDataNotFound("Venta no encontrada"));
        if (venta.getCliente() != null) {
            throw new ExceptionOperacionNoPermitida("Esta venta ya tiene un cliente asignado");
        }
        Cliente cliente = iClienteRepository.findById(clienteId)
                .orElseThrow(() -> new ExceptionDataNotFound("Cliente no encontrado"));

        return vincularClienteAVenta(venta, cliente);
    }

    // Se propaga el cliente también al Pedido que respalda la venta porque el sistema de
    // rifas/boletos lee de pedidos, no de ventas (ver IPedidoRepository.calcularScore).
    private Venta vincularClienteAVenta(Venta venta, Cliente cliente) {
        venta.setCliente(cliente);
        venta.setReclamadoEn(LocalDateTime.now());

        if (venta.getPedido() != null) {
            Pedido pedido = venta.getPedido();
            pedido.setCliente(cliente);
            iPedidoRepository.save(pedido);
        }

        return iVentaRepository.save(venta);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public List<TotalDetalle> getTotalDetalle() {
        return entityManager.createNativeQuery("CALL inventario_key.TOTAL_DETALLE()", TotalDetalle.class)
                .getResultList();
    }
}
