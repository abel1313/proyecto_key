package com.ventas.key.mis.productos.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.LotesProductos;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ProductoDTO;
import com.ventas.key.mis.productos.models.ProductoDetalle;
import com.ventas.key.mis.productos.repository.ILostesProductosRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.service.api.ICodigoBarrasService;
import com.ventas.key.mis.productos.service.api.IProductoService;


@Service
public class ProductosServiceImpl extends
        CrudAbstractServiceImpl<Producto, List<Producto>, Optional<Producto>, Integer, PginaDto<List<Producto>>>
        implements IProductoService {

    private final IProductosRepository iProductosRepository;
    private final ILostesProductosRepository iLoteProducto;
    private final ICodigoBarrasService iBarrasService;
    private final ErrorGenerico error;

    public ProductosServiceImpl(final IProductosRepository iProductosRepository,
            final ErrorGenerico error,
            final ILostesProductosRepository iLoteProducto,
            final ICodigoBarrasService iBarrasService) {
        super(iProductosRepository, error);
        this.iProductosRepository = iProductosRepository;
        this.error = error;
        this.iLoteProducto = iLoteProducto;
        this.iBarrasService = iBarrasService;
        // TODO Auto-generated constructor stub
    }

    @Override
    public Producto actualizarStock(Integer id, Integer nuevoStock) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PginaDto<List<ProductoDTO>> getAll(int size, int page) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Producto> productosPaginados = iProductosRepository.findAll(pageable);
        PginaDto<List<ProductoDTO>> pginaDto = new PginaDto<>();
        List<ProductoDTO> lista = productosPaginados.getContent()
                .stream()
                .filter(stock-> stock.getStock() > 0)
                .map(m -> {
                    final ProductoDTO pro = new ProductoDTO();
                    pro.setNombre(m.getNombre());
                    pro.setPrecioCosto(m.getPrecioCosto());
                    pro.setPiezas(m.getPiezas());
                    pro.setColor(m.getColor());
                    pro.setPrecioVenta(m.getPrecioVenta());
                    pro.setPrecioRebaja(m.getPrecioRebaja());
                    pro.setDescripcion(m.getDescripcion());
                    pro.setStock(m.getStock());
                    pro.setMarca(m.getMarca());
                    pro.setContenido(m.getContenido());
                    pro.setCodigoBarras(m.getCodigoBarras().getCodigoBarras());
                    return pro;
                })
                .collect(Collectors.toList());

        pginaDto.setPagina(page);
        pginaDto.setTotalPaginas(productosPaginados.getTotalPages());
        pginaDto.setTotalRegistros((int) productosPaginados.getTotalElements());
        pginaDto.setT(lista);
        return pginaDto;
    }

    @Override
    public PginaDto<List<ProductoDTO>> findNombreOrCodigoBarra(int size, int page, String nombre) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Producto> productosPaginados = iProductosRepository.findByNombreContaining(nombre, pageable);

        if (productosPaginados.isEmpty()) {
            productosPaginados = iProductosRepository.findByCodigoBarras_CodigoBarrasContaining(nombre, pageable);
        }
        PginaDto<List<ProductoDTO>> pginaDto = new PginaDto<>();

        pginaDto.setPagina(page);
        pginaDto.setTotalPaginas(productosPaginados.getTotalPages());
        pginaDto.setTotalRegistros((int) productosPaginados.getTotalElements());
        pginaDto.setT(listaProductos(productosPaginados.getContent()));
        return pginaDto;
    }

    private List<ProductoDTO> listaProductos(List<Producto> lista) {
        return lista
                .stream()
                .filter(stock-> stock.getStock() > 0)
                .map(m -> {
                    final ProductoDTO pro = new ProductoDTO();
                    pro.setNombre(m.getNombre());
                    pro.setPrecioCosto(m.getPrecioCosto());
                    pro.setPiezas(m.getPiezas());
                    pro.setColor(m.getColor());
                    pro.setPrecioVenta(m.getPrecioVenta());
                    pro.setPrecioRebaja(m.getPrecioRebaja());
                    pro.setDescripcion(m.getDescripcion());
                    pro.setStock(m.getStock());
                    pro.setMarca(m.getMarca());
                    pro.setContenido(m.getContenido());
                    pro.setCodigoBarras(m.getCodigoBarras().getCodigoBarras());
                    return pro;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public Producto saveProductoLote(ProductoDetalle productoDetalle) throws Exception {

        try {
            Producto producto = new Producto();
            producto.setId(productoDetalle.getId());
            producto.setNombre(productoDetalle.getNombre());
            producto.setPrecioCosto(productoDetalle.getPrecioCosto());
            producto.setPiezas(productoDetalle.getPiezas());
            producto.setColor(productoDetalle.getColor());
            producto.setPrecioVenta(productoDetalle.getPrecioVenta());
            producto.setPrecioRebaja(productoDetalle.getPrecioRebaja());
            producto.setDescripcion(productoDetalle.getDescripcion());
            producto.setStock(productoDetalle.getStock());
            producto.setMarca(productoDetalle.getMarca());
            producto.setContenido(productoDetalle.getContenido());
            CodigoBarra codigoBarras = new CodigoBarra();
            codigoBarras.setId(productoDetalle.getCodigoBarras().getId());
            codigoBarras.setCodigoBarras(productoDetalle.getCodigoBarras().getCodigoBarras());
            producto.setCodigoBarras(codigoBarras);

            Optional<Producto> prodExistente = this.iProductosRepository
                    .findByCodigoBarras_CodigoBarras(producto.getCodigoBarras().getCodigoBarras());

            if (prodExistente.isPresent()) {
                Producto prductoPtional = prodExistente.get();
                Producto prductoEdicion = prductoPtional;
                BigDecimal precioBase = new BigDecimal(prductoPtional.getPrecioVenta());
                BigDecimal precioReq = new BigDecimal(productoDetalle.getPrecioVenta());


                

                if( productoDetalle.getStock() == 0   ){
                    throw new Exception("El stock no debe de ser 0");
                }
                if (precioBase.compareTo(precioReq) == 0) {

                    prductoPtional = producto;
                    producto.setId(prductoEdicion.getId());
                    prductoPtional.setCodigoBarras(prductoEdicion.getCodigoBarras());
                    prductoPtional.setStock(productoDetalle.getStock());

                    Producto prd = this.iProductosRepository.save(prductoPtional);
                    System.out.println(prd + "-----------------------------------------------");
                    return prd;
                } else {
                    Optional<LotesProductos> existeProducto = this.iLoteProducto
                                                                  .findByProducto_CodigoBarras_CodigoBarras(productoDetalle.getCodigoBarras().getCodigoBarras());

                    LotesProductos saveLote = new LotesProductos();
                    if( existeProducto.isPresent()){
                        LotesProductos optionalLot = existeProducto.get();
                        saveLote.setId(optionalLot.getId());
                        saveLote.setPrecioUnitario(productoDetalle.getPrecioVenta());
                        saveLote.setStock(productoDetalle.getStock());
                        saveLote.setProducto(prductoEdicion);
                    }else{
                    saveLote.setProducto(prductoEdicion);
                    int totalStock = productoDetalle.getStock();
                    saveLote.setStock(totalStock);
                    saveLote.setPrecioUnitario(productoDetalle.getPrecioVenta());
                    }

                    saveLote = this.iLoteProducto.save(saveLote);
                    if (saveLote != null) {
                        return producto;
                    }
                }
            } else {
                CodigoBarra codigoBarra = new CodigoBarra();
                Optional<CodigoBarra> optBarra = iBarrasService.findByCodigoBarra(producto.getCodigoBarras().getCodigoBarras());
                if( optBarra.isPresent()){
                    codigoBarra = optBarra.get();
                }else{
                    codigoBarra.setCodigoBarras(producto.getCodigoBarras().getCodigoBarras());
                    codigoBarra = iBarrasService.save(codigoBarra);
                }
                producto.setCodigoBarras(codigoBarra);
                return this.iProductosRepository.save(producto);
            }
        } catch (Exception e) {
            this.error.error(e);
        }
        throw new Exception("No se guardo el producto ");
    }

    // total 4337.0
}
