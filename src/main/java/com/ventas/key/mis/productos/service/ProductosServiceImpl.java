package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.*;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.mapper.ProductoAdmin;
import com.ventas.key.mis.productos.mapper.ProductoUser;
import com.ventas.key.mis.productos.models.*;
import com.ventas.key.mis.productos.repository.ILostesProductosRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.service.api.ICodigoBarrasService;
import com.ventas.key.mis.productos.service.api.IImagenService;
import com.ventas.key.mis.productos.service.api.IProductoImagenService;
import com.ventas.key.mis.productos.service.api.IProductoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductosServiceImpl extends
        CrudAbstractServiceImpl<Producto, List<Producto>, Optional<Producto>, Integer, PginaDto<List<Producto>>>
        implements IProductoService {


    /// 1 validar que el codigo de barras exista, si no existe se agrega el nuevo codigo de barra en caso contrario se se obtiene el codigo de barras
    /// buscar el producto si existe validar el codigo de barra que exxiste y que no se repita validar que el codigo de barras que viene es el mismo que el que trae en caso contrario actualizarlo
    /// si el producto existe actualizar el stock el que viene mas el que existe
    /// falta valida que si se equivoca eliminar el stock
    ///
    ///
    ///

    private final IProductosRepository iProductosRepository;
    private final ILostesProductosRepository iLoteProducto;
    private final ICodigoBarrasService iBarrasService;
    private final ErrorGenerico error;
    private final IImagenService iImagenService;
    private final IProductoImagenService iProductoImagenService;

    public ProductosServiceImpl(final IProductosRepository iProductosRepository,
            final ErrorGenerico error,
            final ILostesProductosRepository iLoteProducto,
            final ICodigoBarrasService iBarrasService,
            final IImagenService iImagenService,
            final IProductoImagenService iProductoImagenService) {
        super(iProductosRepository, error);
        this.iProductosRepository = iProductosRepository;
        this.error = error;
        this.iLoteProducto = iLoteProducto;
        this.iBarrasService = iBarrasService;
        this.iImagenService = iImagenService;
        this.iProductoImagenService = iProductoImagenService;
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
        Page<Producto> productosPaginados = iProductosRepository.findByStockGreaterThanAndHabilitado(0, '1',pageable);
        PginaDto<List<ProductoDTO>> pginaDto = new PginaDto<>();
        List<ProductoDTO> listPtroductos = productosPaginados.getContent()
                .stream()
                .map(this::mapperByRol)
                .toList();
        pginaDto.setPagina(page);
        pginaDto.setTotalPaginas(productosPaginados.getTotalPages());
        pginaDto.setTotalRegistros((int) productosPaginados.getTotalElements());
        pginaDto.setT(listPtroductos);
        return pginaDto;
    }

    private ProductoDTO mapperByRol(Producto p){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
        if(isAdmin){
            ProductoAdmin productoAdmin = getProductoAdmin(p);
            return new ProductoDTO(productoAdmin);
        }else{
            ProductoUser productoUser = new ProductoUser();
            productoUser.setNombre(p.getNombre());
            productoUser.setColor(p.getColor());
            productoUser.setPrecioVenta(p.getPrecioVenta());
            productoUser.setDescripcion(p.getDescripcion());
            productoUser.setCodigoBarras(p.getCodigoBarras().getCodigoBarras());
            productoUser.setIdProducto(p.getId());
            return new ProductoDTO(productoUser);
        }
    }
    private static ProductoAdmin getProductoAdmin(Producto p) {
        ProductoAdmin productoAdmin = new ProductoAdmin();
        productoAdmin.setNombre(p.getNombre());
        productoAdmin.setColor(p.getColor());
        productoAdmin.setPrecioVenta(p.getPrecioVenta());
        productoAdmin.setDescripcion(p.getDescripcion());
        productoAdmin.setCodigoBarras(p.getCodigoBarras() != null && p.getCodigoBarras().getCodigoBarras() != null ? p.getCodigoBarras().getCodigoBarras(): "");
        productoAdmin.setIdProducto(p.getId());
        productoAdmin.setPrecioCosto(p.getPrecioCosto());
        productoAdmin.setPiezas(p.getPiezas());
        productoAdmin.setPrecioRebaja(p.getPrecioRebaja());
        productoAdmin.setStock(p.getStock());
        productoAdmin.setMarca(p.getMarca());
        productoAdmin.setContenido(p.getContenido());
        productoAdmin.setHabilitado(p.getHabilitado());
        return productoAdmin;
    }


    @Override
    public PginaDto<List<ProductoDTO>> findNombreOrCodigoBarra(int size, int page, String nombre) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Producto> productosPaginados = iProductosRepository.findByNombreContainingAndHabilitado(nombre, '1', pageable);

        if (productosPaginados.isEmpty()) {
            productosPaginados = iProductosRepository.findByCodigoBarras_CodigoBarrasContainingAndHabilitado(nombre,'1', pageable);
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
                .filter(stock -> stock.getStock() > 0)
                .map(p -> {
                    final ProductoDTO dto = new ProductoDTO();
                    dto.setNombre(p.getNombre());
                    dto.setPrecioCosto(p.getPrecioCosto());
                    dto.setPiezas(p.getPiezas());
                    dto.setColor(p.getColor());
                    dto.setPrecioVenta(p.getPrecioVenta());
                    dto.setPrecioRebaja(p.getPrecioRebaja());
                    dto.setDescripcion(p.getDescripcion());
                    dto.setStock(p.getStock());
                    dto.setMarca(p.getMarca());
                    dto.setContenido(p.getContenido());
                    dto.setCodigoBarras(p.getCodigoBarras() != null ? p.getCodigoBarras().getCodigoBarras(): null);
                    dto.setIdProducto(p.getId());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public Producto saveProductoLote(ProductoDetalle productoDetalle) throws Exception {


        if (productoDetalle.getStock() == 0) {
            throw new Exception("El stock no debe de ser 0");
        }
        if (productoDetalle.getCodigoBarras() == null) {
            throw new Exception("El codigo de barras es requerido");
        }
        try {
            Producto producto = llenarProductoDTO(productoDetalle);
            producto.setHabilitado('1');

            CodigoBarra codigoBarras = new CodigoBarra();
            codigoBarras.setId(productoDetalle.getCodigoBarras().getId());
            codigoBarras.setCodigoBarras(productoDetalle.getCodigoBarras().getCodigoBarras().isEmpty() ? null : productoDetalle.getCodigoBarras().getCodigoBarras());
            producto.setCodigoBarras(codigoBarras);

            Producto prodExistenteNoOpt = null;
            if( producto.getCodigoBarras().getCodigoBarras() != null ){
                prodExistenteNoOpt = this.iProductosRepository
                        .findByCodigoBarras_CodigoBarras(producto.getCodigoBarras().getCodigoBarras())
                        .orElse(null);
            }
            if(prodExistenteNoOpt == null) {
                List<Imagen> lstImg;
                if (!productoDetalle.getListImagenes().isEmpty()){
                    producto.setCodigoBarras(null);
                    lstImg = this.iImagenService.saveAll(mappImagenes(productoDetalle.getListImagenes()));
                    Producto prodGuar = this.iProductosRepository.save(producto);
                    List<ProductoImagen> mapperRelacionProductoImagen = mapperRelacionProductoImagen(lstImg, prodGuar);
                    relacionProductoImagen(mapperRelacionProductoImagen);
                }else{
                    CodigoBarra codBarr = this.iBarrasService.save(producto.getCodigoBarras());
                    producto.setCodigoBarras(codBarr);
                }
                return this.iProductosRepository.save(producto);
            }


            if (prodExistenteNoOpt.getCodigoBarras() != null && prodExistenteNoOpt.getCodigoBarras().getId() != 31) {
                Producto prductoPtional = prodExistenteNoOpt;
                Producto prductoEdicion = prductoPtional;
                List<Imagen> lstImg;
                if (!productoDetalle.getListImagenes().isEmpty()){
                    lstImg = this.iImagenService.saveAll(mappImagenes(productoDetalle.getListImagenes()));
                    List<ProductoImagen> mapperRelacionProductoImagen = mapperRelacionProductoImagen(lstImg, prductoPtional);
                    relacionProductoImagen(mapperRelacionProductoImagen);
                }

                    prductoPtional = producto;
                    producto.setId(prductoEdicion.getId());
                    prductoPtional.setCodigoBarras(prductoEdicion.getCodigoBarras());
                    if(productoDetalle.getActualizarStock() > 0){
                        prductoPtional.setStock(productoDetalle.getActualizarStock() + prductoPtional.getStock());
                        productoDetalle.setEliminarStock(0);
                    }
                if(productoDetalle.getEliminarStock() > 0){
                    prductoPtional.setStock(prductoPtional.getStock() -  productoDetalle.getEliminarStock());
                    productoDetalle.setActualizarStock(0);
                }

                    Producto prd = this.iProductosRepository.save(prductoPtional);

                    System.out.println(prd + "-----------------------------------------------");
                    return prd;
            } else {
                Producto prodNoOpt = this.iProductosRepository.findById(producto.getId() == null ? 0 : producto.getId() ).orElse(new Producto());
                if (prodNoOpt.getId() != null && prodNoOpt.getId() != 0) {
                    BigDecimal precioBase = new BigDecimal(prodNoOpt.getPrecioVenta());
                    BigDecimal precioReq = new BigDecimal(productoDetalle.getPrecioVenta());


                    if (precioBase.compareTo(precioReq) == 0) {
                        producto.setId(prodNoOpt.getId());
                        producto.setCodigoBarras(prodNoOpt.getCodigoBarras());
                        producto.setStock(productoDetalle.getStock());
                        Producto prd = this.iProductosRepository.save(producto);
                        System.out.println(prd + "-----------------------------------------------");
                        return prd;
                    } else {
                            LotesProductos saveLote = new LotesProductos();
                            saveLote.setPrecioUnitario(productoDetalle.getPrecioVenta());
                            saveLote.setStock(productoDetalle.getStock());
                            saveLote.setProducto(prodNoOpt);
                         this.iLoteProducto.save(saveLote);
                        return producto;
                    }

                }
                CodigoBarra codBarra = saveCodigoBarra(producto.getCodigoBarras());
                producto.setCodigoBarras(codBarra);
                return this.iProductosRepository.save(producto);
            }
        } catch (Exception e) {
            this.error.error(e);
        }
        throw new Exception("No se guardo el producto ");
    }

    public Optional<ProductoResumen> getResumen(int id){
        return Optional.of(this.iProductosRepository.findProductoConImagenes(id));
    }
    private List<ProductoImagen> mapperRelacionProductoImagen(List<Imagen> lstImg,
                                                              Producto prd){
        return lstImg.stream().map(mpa->{
            ProductoImagen p = new ProductoImagen();
            p.setImagen(mpa!= null? mpa : new Imagen());
            p.setProducto(prd);
            return p;
        }).toList();
    }
    private void relacionProductoImagen(List<ProductoImagen>  productoImagens){
        this.iProductoImagenService.saveAll(productoImagens);
    }
    private List<Imagen> mappImagenes( List<ImagenDTO> list){
        return list.stream().map(mpa->{
            Imagen imagen = new Imagen();
            byte[] decodedBytes = mpa.getBase64();
            imagen.setBase64(decodedBytes);
            imagen.setNombreImagen(mpa.getNombreImagen());
            imagen.setExtension(mpa.getExtension());
            return imagen;
        }).toList();
    }

    private CodigoBarra saveCodigoBarra(CodigoBarra codigoBarra) throws Exception {
        Optional<CodigoBarra> findCodigoBarra = this.iBarrasService.findByCodigoBarra(codigoBarra.getCodigoBarras());
        CodigoBarra newCodigoBarra;
        if (findCodigoBarra.isPresent()) {
            newCodigoBarra = findCodigoBarra.get();
        }else{
            codigoBarra.setId(null);
            newCodigoBarra = this.iBarrasService.save(codigoBarra);
        }
        return newCodigoBarra;
    }

    private Producto llenarProductoDTO(ProductoDetalle productoDetalle) {
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
        return producto;
    }

}
