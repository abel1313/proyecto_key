package com.ventas.key.mis.productos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.mis.productos.entity.*;
import com.ventas.key.mis.productos.entity.MesesIntereses;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.entity.PagosYMeses;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.handleExeption.GenericException;
import com.ventas.key.mis.productos.models.PageableDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.UsuarioDto;
import com.ventas.key.mis.productos.models.pedidos.PedidoGenerico;
import com.ventas.key.mis.productos.models.pedidos.PedidosDTOPedido;
import com.ventas.key.mis.productos.repository.IClienteRepository;
import com.ventas.key.mis.productos.repository.IDetallePagoRepository;
import com.ventas.key.mis.productos.repository.IDetallePedidoRepository;
import com.ventas.key.mis.productos.repository.IPagosYMesesRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.service.api.IPedidoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    public PedidoServiceImpl(final IPedidoRepository iPedidoRepository, ErrorGenerico error,
                             final IClienteRepository iClienteRepository,
                             final IProductosRepository iProductoRepository,
                             final VentaServiceImpl vImpl,
                             final IUsuarioRepository iUsuarioRepository,
                             final ObjectMapper objectMapper,
                             final IDetallePagoRepository iDetallePagoRepository,
                             final IDetallePedidoRepository iDetallePedidoRepository,
                             final IPagosYMesesRepository iPagosYMesesRepository,
                             final IVarianteRepository iVarianteRepository) {
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
    }

    @CacheEvict(value = {"obtenerProductosCache",
                        "buscarNombreOrCodigoBarrasCache",
                        "findByIdCache",
                        "variantesCodigoBarrasCache",
                        "variantesNombreCache",
                        "variantesProductoCache",
                        "variantesImagenesCache",
                        "variantesCodigoBarrasCache"}, allEntries = true)
    @Transactional
    public Pedido savePedido(@RequestBody PedidosDTOPedido requestG, BindingResult result) throws Exception {

        Cliente cliente = this.iClienteRepository.findById(requestG.getCliente().getId())
                .orElseThrow(() -> new Exception("Ocurrio un erro al buscar al cliente"));
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setEstadoPedido(requestG.getEstadoPedido());
        pedido.setFechaPedido(requestG.getFechaPedido());
        pedido.setFechaRecogida(requestG.getFechaRecogida());
        pedido.setObservaciones(requestG.getObservaciones());

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
            prod.setStock(prod.getStock() - mpa.getCantidad());
            this.iProductoRepository.save(prod);

            DetallePedido dta = new DetallePedido();
            dta.setCantidad(mpa.getCantidad());
            dta.setPrecioUnitario(mpa.getPrecioUnitario());
            dta.setSubTotal(mpa.getSubTotal());
            dta.setPedido(pedido);
            dta.setProducto(prod);
            dta.setVariante(variante);
            detallePedido.add(dta);
        }
        pedido.setDetalles(detallePedido);
        return this.iPedidoRepository.save(pedido);
    }

    @Transactional
    @Override
    @CacheEvict(value = {"obtenerProductosCache",
            "buscarNombreOrCodigoBarrasCache",
            "findByIdCache",
            "variantesCodigoBarrasCache",
            "variantesNombreCache",
            "variantesProductoCache",
            "variantesImagenesCache",
            "variantesCodigoBarrasCache"}, allEntries = true)
    public PedidoGenerico updatePedido(int id, PedidoGenerico requestG) throws Exception {
        Pedido pedido = this.iPedidoRepository.findById(id)
                .orElseThrow(() -> new GenericException(500, "El pedido no existe"));

        if ("Entregado".equals(pedido.getEstadoPedido())) {
            throw new RuntimeException("El pedido ya fue confirmado");
        }
        if ("cancelado".equals(pedido.getEstadoPedido())) {
            throw new RuntimeException("El pedido está cancelado y no se puede confirmar");
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

        List<DetalleVenta> det = new ArrayList<>();
        for (var sa : requestG.getPedido().getDetalles()) {
            Producto prod = this.iProductoRepository.findById(sa.getProducto().intValue())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + sa.getProducto()));

            double precioCosto  = prod.getPrecioCosto();
            double subTotal     = sa.getSub_total();
            double costoTotal   = precioCosto * sa.getCantidad();
            double comision     = subTotal * (tasaTarifa + tasaIva);
            double ganancia     = subTotal - costoTotal - comision;

            DetalleVenta deta = new DetalleVenta();
            deta.setCantidad(sa.getCantidad());
            deta.setPrecioUnitario(sa.getPrecio_unitario());
            deta.setSubTotal(subTotal);
            deta.setPrecioCosto(precioCosto);
            deta.setGanancia(ganancia);
            deta.setFechaVenta(LocalDate.now());
            deta.setProducto(prod);
            deta.setVenta(venta);
            det.add(deta);
        }

        double totalVenta    = det.stream().mapToDouble(DetalleVenta::getSubTotal).sum();
        double gananciaTotal = det.stream().mapToDouble(DetalleVenta::getGanancia).sum();

        venta.setTotalVenta(totalVenta);
        venta.setGananciaTotal(gananciaTotal);
        venta.setDetalles(det);

        pedido.setEstadoPedido("Entregado");
        this.iPedidoRepository.save(pedido);
        this.vImpl.save(venta);

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
            jsonList = iPedidoRepository.buscarPedidosPorCliente(buscar, pageable);
        }else{
            jsonList = iPedidoRepository.buscarTodosLosPedidos(pageable);
        }
        return getListPageableDto(jsonList);
    }


    @CacheEvict(value = {"obtenerProductosCache", "buscarNombreOrCodigoBarrasCache", "findByIdCache"}, allEntries = true)
    @Transactional
    @Override
    public void deletePedidoById(int id) {
        Pedido pedido = iPedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if ("cancelado".equals(pedido.getEstadoPedido()) || "Entregado".equals(pedido.getEstadoPedido())) {
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
        iPedidoRepository.save(pedido);
    }

    @CacheEvict(value = {"obtenerProductosCache", "buscarNombreOrCodigoBarrasCache", "findByIdCache"}, allEntries = true)
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
