package com.ventas.key.mis.productos.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ventas.key.mis.productos.entity.LotesProductos;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ProductoDTO;
import com.ventas.key.mis.productos.models.TotalDetalle;
import com.ventas.key.mis.productos.repository.IDetalleVentaRepository;
import com.ventas.key.mis.productos.repository.ILostesProductosRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.service.api.IProductoService;

@Service
public class ProductosServiceImpl extends CrudAbstract<Producto, Producto, List<Producto>, Optional<Producto>, Integer>
        implements IProductoService {

    private final IProductosRepository iProductosRepository;
    private final ILostesProductosRepository iLoteProducto;
    private final ErrorGenerico error;

    public ProductosServiceImpl(final IProductosRepository iProductosRepository,
            final ErrorGenerico error,
            final ILostesProductosRepository iLoteProducto) {
        super(iProductosRepository, error);
        this.iProductosRepository = iProductosRepository;
        this.error = error;
        this.iLoteProducto = iLoteProducto;
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

    @Override
    public Producto saveProductoLote(Producto producto) throws Exception {

        try {
            Optional<Producto> prodExistente = this.iProductosRepository
                    .findByCodigoBarras_CodigoBarras(producto.getCodigoBarras().getCodigoBarras());
            
            if (prodExistente.isPresent()) {
                Producto prductoPtional = prodExistente.get();
                if (prductoPtional.getPrecioVenta() == producto.getPrecioVenta()) {
                    int totalStock = prductoPtional.getStock() + producto.getStock();
                    prductoPtional.setStock(totalStock);
                    return this.iProductosRepository.save(prductoPtional);
                } else {
                    Optional<LotesProductos> optionalLote = this.iLoteProducto
                            .findByProducto_CodigoBarras_CodigoBarras(producto.getCodigoBarras().getCodigoBarras());
                    LotesProductos saveLote = null;
                    if (optionalLote.isPresent()) {
                        LotesProductos productoExistente = optionalLote.get();
                        int totalStock = productoExistente.getStock() + producto.getStock();
                        productoExistente.setStock(totalStock);
                        saveLote = this.iLoteProducto.save(productoExistente);
                        if( saveLote != null ){
                            return producto;
                        }
                        
                    }
                     throw new Exception("No se guardo el producto ");
                }
            } else{
                return this.iProductosRepository.save(producto);
            }
        } catch (Exception e) {
            this.error.error(e);
        }
        throw new Exception("No se guardo el producto ");
    }


    // total 4337.0
}
