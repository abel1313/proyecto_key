package com.ventas.key.mis.productos.service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.mis.productos.entity.*;
import com.ventas.key.mis.productos.models.*;
import com.ventas.key.mis.productos.service.api.IImagenService;
import com.ventas.key.mis.productos.service.api.IProductoImagenService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.repository.ILostesProductosRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.service.api.ICodigoBarrasService;
import com.ventas.key.mis.productos.service.api.IProductoService;

@Service
public class ProductosServiceImpl extends
        CrudAbstractServiceImpl<Producto, List<Producto>, Optional<Producto>, Integer, PginaDto<List<Producto>>>
        implements IProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductosServiceImpl.class);
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
        Page<Producto> productosPaginados = iProductosRepository.findByStockGreaterThan(0, pageable);
        PginaDto<List<ProductoDTO>> pginaDto = new PginaDto<>();

        List<Integer> productoIds = productosPaginados.getContent()
                .stream()
                .map(Producto::getId)
                .toList();
        Map<Integer, Integer> imagenesPorProducto = iImagenService
                .findIdsImagenesProducto(productoIds) // este método lo defines tú
                .stream()
                .collect(Collectors.toMap(
                        ImagenProductoResult::getProductoId,
                        ImagenProductoResult::getImagenId
                ));

        List<ProductoDTO> listPtroductos = productosPaginados.getContent()
                .stream()
                .map(p -> {
                    ProductoDTO dto = new ProductoDTO();
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
                    dto.setCodigoBarras(p.getCodigoBarras().getCodigoBarras());
                    dto.setIdProducto(p.getId());
                    return dto;
                })
                .toList();
        pginaDto.setPagina(page);
        pginaDto.setTotalPaginas(productosPaginados.getTotalPages());
        pginaDto.setTotalRegistros((int) productosPaginados.getTotalElements());
        pginaDto.setT(listPtroductos);
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
        try {
            Producto producto = llenarProductoDTO(productoDetalle);

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
                BigDecimal precioBase = new BigDecimal(prductoPtional.getPrecioVenta());
                BigDecimal precioReq = new BigDecimal(productoDetalle.getPrecioVenta());

                List<Imagen> lstImg = List.of();
                if (!productoDetalle.getListImagenes().isEmpty()){
                    lstImg = this.iImagenService.saveAll(mappImagenes(productoDetalle.getListImagenes()));
                    List<ProductoImagen> mapperRelacionProductoImagen = mapperRelacionProductoImagen(lstImg, prductoPtional);
                    relacionProductoImagen(mapperRelacionProductoImagen);
                }

                if (precioBase.compareTo(precioReq) == 0) {

                    prductoPtional = producto;
                    producto.setId(prductoEdicion.getId());
                    prductoPtional.setCodigoBarras(prductoEdicion.getCodigoBarras());
                    prductoPtional.setStock(productoDetalle.getStock() + prductoPtional.getStock());

                    Producto prd = this.iProductosRepository.save(prductoPtional);

                    System.out.println(prd + "-----------------------------------------------");
                    return prd;
                } else {
                    Optional<LotesProductos> existeProducto = this.iLoteProducto
                            .findByProducto_CodigoBarras_CodigoBarras(
                                    productoDetalle.getCodigoBarras().getCodigoBarras());

                    LotesProductos saveLote = getLotesProductos(productoDetalle, existeProducto, prductoEdicion);
                    saveLote = this.iLoteProducto.save(saveLote);
                    return producto;
                }

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
                        saveLote = this.iLoteProducto.save(saveLote);
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

    private static LotesProductos getLotesProductos(ProductoDetalle productoDetalle, Optional<LotesProductos> existeProducto, Producto prductoEdicion) {
        LotesProductos saveLote = new LotesProductos();
        int totStock = productoDetalle.getStock() + prductoEdicion.getStock();
        if (existeProducto.isPresent()) {
            LotesProductos optionalLot = existeProducto.get();
            saveLote.setId(optionalLot.getId());
            saveLote.setPrecioUnitario(productoDetalle.getPrecioVenta());
            saveLote.setStock(totStock);
            saveLote.setProducto(prductoEdicion);
        } else {
            saveLote.setProducto(prductoEdicion);
            saveLote.setStock(totStock);
            saveLote.setPrecioUnitario(productoDetalle.getPrecioVenta());
        }
        return saveLote;
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

    private List<ImagenDTO> llenarListaImagenSoloUna(Integer id){
        try {
            List<ProductoImagen> listImg = this.iProductoImagenService.findByProductoId(id)
                    .stream()
                    .findFirst()
                    .stream()
                    .toList();

            if(!listImg.isEmpty()){
                return listImg.stream().map(mpaDto-> {
                   // Imagen imagen = mpaDto.getImagen();
                    Imagen imagen = mpaDto.getImagen();
                    ImagenDTO img = new ImagenDTO();
                    img.setBase64(null);
                    img.setExtension(imagen.getExtension());
                    img.setNombreImagen(imagen.getNombreImagen());
                    return img;
                }).toList();
            }
            return  new ArrayList<>();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    // total 4337.0
}
