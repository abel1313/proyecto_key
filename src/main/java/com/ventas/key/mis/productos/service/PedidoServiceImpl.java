package com.ventas.key.mis.productos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.mis.productos.entity.*;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.handleExeption.GenericException;
import com.ventas.key.mis.productos.models.DetalleVentaDto;
import com.ventas.key.mis.productos.models.PageableDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.UsuarioDto;
import com.ventas.key.mis.productos.models.pedidos.PedidoGenerico;
import com.ventas.key.mis.productos.models.pedidos.PedidosDTOPedido;
import com.ventas.key.mis.productos.repository.*;
import com.ventas.key.mis.productos.service.api.IPedidoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;

import javax.swing.text.html.Option;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
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
    public PedidoServiceImpl(final IPedidoRepository iPedidoRepository, ErrorGenerico error,
                             final IClienteRepository iClienteRepository,
                             final IProductosRepository iProductoRepository,
                             final VentaServiceImpl vImpl,
                             final IUsuarioRepository iUsuarioRepository,
                             final ObjectMapper objectMapper) {
        super(iPedidoRepository, error);
        this.iProductoRepository = iProductoRepository;
        this.iClienteRepository = iClienteRepository;
        this.iPedidoRepository = iPedidoRepository;
        this.iUsuarioRepository = iUsuarioRepository;
        this.vImpl = vImpl;
        this.objectMapper = objectMapper;
    }

    public Pedido savePedido(@RequestBody PedidosDTOPedido requestG, BindingResult result) throws Exception {

        Cliente cliente = this.iClienteRepository.findById(requestG.getCliente().getId())
                .orElseThrow(()-> new Exception("Ocurrio un erro al buscar al cliente"));
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setEstadoPedido(requestG.getEstadoPedido());
        pedido.setFechaPedido(requestG.getFechaPedido());
        pedido.setObservaciones(requestG.getObservaciones());

        List<DetallePedido> detallePedido = requestG.getDetalles().stream().map(mpa->{
            DetallePedido dta = new DetallePedido();
            dta.setCantidad(mpa.getCantidad());
            dta.setPrecioUnitario(mpa.getPrecioUnitario());
            dta.setSubTotal(mpa.getSubTotal());
            dta.setPedido(pedido);
            Producto prod = this.iProductoRepository.findById(mpa.getProducto().getId()).orElseThrow(()-> new RuntimeException("Ocurrio un erro al buscar al producto") );
            dta.setProducto(prod);
            return dta;
        }).toList();
        pedido.setDetalles(detallePedido);
        
       // log.info("info {}",new ObjectMapper().writeValueAsString(pedido));

        return this.iPedidoRepository.save(pedido);
    }

    @Override
    public PedidoGenerico updatePedido(int id, PedidoGenerico requestG) throws Exception {
        Optional<Pedido> optPedido = this.iPedidoRepository.findById(id);

        if(optPedido.isPresent()) {
            this.iPedidoRepository.save(optPedido.get());
            Pedido pedido = optPedido.get();
            List<DetalleVenta> det = requestG.getPedido().getDetalles().stream().map(sa->{
                DetalleVenta deta = new DetalleVenta();
                deta.setCantidad(sa.getCantidad());
                deta.setPrecioUnitario(sa.getPrecio_unitario());
                deta.setSubTotal(sa.getSub_total());
                deta.getProducto().setId(sa.getProducto().intValue());
                return deta;
            }).toList();
            Venta venta = new Venta();
            venta.setEstadoVenta("Entregado");
            venta.setTotalVenta(det.stream().mapToDouble(DetalleVenta::getSubTotal).sum());
            venta.setFormaPago("efectivo");
            venta.setFechaVenta(LocalDateTime.now());
            venta.setPedido(pedido);
            venta.setDetalles(det);
//            UsuarioDto usr = iUsuarioRepository.findUserByIdCliente(requestG.getCliente().getId())
//                    .orElseThrow(()-> new Exception("Ocurrio un error al buscar el usuario"));
//
//            Usuario u = new Usuario();
//            u.setId(usr.getIdUsuario());
//            venta.setUsuario(u);
//            venta.setDetalles(det);
            this.vImpl.save(venta);

        }
        throw new GenericException(500,"El pedido no existe");
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
        Page<String> jsonList = null;
        if(buscar.isEmpty()){
            jsonList = iPedidoRepository.buscarPedidosPorCliente(buscar, pageable);
        }else{
            jsonList = iPedidoRepository.buscarTodosLosPedidos(pageable);
        }
        return getListPageableDto(jsonList);
    }


    @Override
    public void deletePedidoById(int id) {
        this.iPedidoRepository.deleteById(id);
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
