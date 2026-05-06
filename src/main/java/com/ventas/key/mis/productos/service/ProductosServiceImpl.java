package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.*;
import com.ventas.key.mis.productos.entity.productoVariantes.VarianteImagen;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.hexagonal.dominio.mapper.RequestProductoImagen;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenPort;
import com.ventas.key.mis.productos.hexagonal.infraestructura.ImagenProductoClienteAWS;
import com.ventas.key.mis.productos.mapper.ProductoAdmin;
import com.ventas.key.mis.productos.mapper.ProductoUser;
import com.ventas.key.mis.productos.models.*;
import com.ventas.key.mis.productos.repository.ILostesProductosRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.service.api.ICodigoBarrasService;
import com.ventas.key.mis.productos.service.api.IImagenService;
import com.ventas.key.mis.productos.service.api.IProductoService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductosServiceImpl extends
        CrudAbstractServiceImpl<Producto, List<Producto>, Optional<Producto>, Integer, PginaDto<List<Producto>>>
        implements IProductoService {

    @Value("${guardar-imagenes.ruta_imagenes}")
    private String rutaImagenes;
    private final IProductosRepository iProductosRepository;
    private final ILostesProductosRepository iLoteProducto;
    private final ICodigoBarrasService iBarrasService;
    private final ErrorGenerico error;
    private final IImagenService iImagenService;
    private final IVarianteRepository varianteRepository;
    private final IVarianteImagenRepository iVarianteImagenRepository;


    private final ImagenProductoClienteAWS imagenProductoClienteAWS;
    private final ImagenPort imagenPort;

    public ProductosServiceImpl(final IProductosRepository iProductosRepository,
            final ErrorGenerico error,
            final ILostesProductosRepository iLoteProducto,
            final ICodigoBarrasService iBarrasService,
            final IImagenService iImagenService,
            final ImagenProductoClienteAWS imagenProductoClienteAWS,
            final IVarianteRepository iVarianteRepository,
            final IVarianteImagenRepository iVarianteImagenRepository,
                                final ImagenPort imagenPort
    ) {
        super(iProductosRepository, error);
        this.iProductosRepository = iProductosRepository;
        this.error = error;
        this.iLoteProducto = iLoteProducto;
        this.iBarrasService = iBarrasService;
        this.iImagenService = iImagenService;
        this.imagenProductoClienteAWS = imagenProductoClienteAWS;
        this.iVarianteImagenRepository = iVarianteImagenRepository;
        this.varianteRepository = iVarianteRepository;
        this.imagenPort = imagenPort;
        // TODO Auto-generated constructor stub
    }

    @Override
    public Producto actualizarStock(Integer id, Integer nuevoStock) {
        // TODO Auto-generated method stub
        return null;
    }


    @SneakyThrows
    @Override
    @Cacheable(value = "obtenerProductosCache",
            key = "#page + ':' + #size + ':' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getAuthorities()")
    public PginaDto<List<ProductoDTO>> getAll(int size, int page) {





        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Producto> productosPaginados =  iProductosRepository.findAll(pageable);
        boolean isAdmin = isAdminContext();
        if (!isAdmin) {
            productosPaginados = iProductosRepository.findDistinctByStockGreaterThanAndHabilitado(0, '1',pageable);
        }

        PginaDto<List<ProductoDTO>> pginaDto = new PginaDto<>();
        List<ProductoDTO> listPtroductos = productosPaginados.getContent()
                .stream()
                .map(p -> mapperByRol(p, isAdmin))
                .toList();
        pginaDto.setPagina(page);
        pginaDto.setTotalPaginas(productosPaginados.getTotalPages());
        pginaDto.setTotalRegistros((int) productosPaginados.getTotalElements());
        pginaDto.setT(listPtroductos);
        return pginaDto;
    }

    private boolean isAdminContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private ProductoDTO mapperByRol(Producto p, boolean isAdmin) {
        com.ventas.key.mis.productos.hexagonal.dominio.Imagen img;
        try {
            img = imagenProductoClienteAWS.buscarImagenProducto(p.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (isAdmin) {
            ProductoAdmin productoAdmin = getProductoAdmin(p);
            productoAdmin.setImagen(img);
            return new ProductoDTO(productoAdmin);
        } else {
            ProductoUser productoUser = new ProductoUser();
            productoUser.setNombre(p.getNombre());
            productoUser.setColor(p.getColor());
            productoUser.setPrecioVenta(p.getPrecioVenta());
            productoUser.setDescripcion(p.getDescripcion());
            productoUser.setCodigoBarras(p.getCodigoBarras().getCodigoBarras());
            productoUser.setIdProducto(p.getId());
            productoUser.setImagen(img);
            productoUser.setStock(p.getStock());
            return new ProductoDTO(productoUser);
        }
    }
    private ProductoAdmin getProductoAdmin(Producto p) {
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
    @Transactional(readOnly = true)
    @Cacheable(value = "buscarNombreOrCodigoBarrasCache",
            key = "#nombre + ':' + #page + ':' + #size + ':' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getAuthorities()")
    public PginaDto<List<ProductoDTO>> findNombreOrCodigoBarra(int size, int page, String nombre) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Producto> productosPaginados;

        // 1. Buscar por código de barras exacto primero
        boolean isAdmin = isAdminContext();
        Optional<Producto> productoExacto = iProductosRepository.findByCodigoBarras_CodigoBarras(nombre);
        if (!isAdmin) {
            productoExacto = iProductosRepository.findByStockGreaterThanAndHabilitadoAndCodigoBarras_CodigoBarras(0, '1', nombre);
        }
        if (productoExacto.isPresent()) {
            productosPaginados = new org.springframework.data.domain.PageImpl<>(
                    List.of(productoExacto.get()), pageable, 1);
        } else {
            // 2. Buscar por nombre
            productosPaginados = iProductosRepository.findByNombreContaining(nombre, pageable);
            if(!isAdmin){
                productosPaginados = iProductosRepository.findByNombreContainingAndHabilitado(nombre, '1', pageable);
            }

            // 3. Si nombre no encontró nada, buscar por código de barras parcial
            if (productosPaginados.isEmpty()) {
                productosPaginados = iProductosRepository.findByCodigoBarras_CodigoBarrasContaining(nombre, pageable);
                if(!isAdmin) {
                    productosPaginados = iProductosRepository.findByStockGreaterThanAndHabilitadoAndCodigoBarras_CodigoBarrasContaining( 0, '1', nombre, pageable);
                }
            }
        }

        List<ProductoDTO> resultados = productosPaginados.getContent().stream()
                .map(p -> mapperByRol(p, isAdmin))
                .toList();

        if (resultados.isEmpty()) {
            throw new ExceptionDataNotFound("No se encontraron productos con la búsqueda: \"" + nombre + "\"");
        }

        PginaDto<List<ProductoDTO>> pginaDto = new PginaDto<>();
        pginaDto.setPagina(page);
        pginaDto.setTotalPaginas(productosPaginados.getTotalPages());
        pginaDto.setTotalRegistros((int) productosPaginados.getTotalElements());
        pginaDto.setT(resultados);
        return pginaDto;
    }

    @Transactional
    @Override
    @CacheEvict(value = {"obtenerProductosCache", "buscarNombreOrCodigoBarrasCache", "findByIdCache", "buscarImagenIdCache"}, allEntries = true)
    public void deleteByIdProducto(Integer id) throws ExceptionErrorInesperado {
        log.info("Buscar producto con el ID {}",id);
        Optional<Producto> existeProducto = iProductosRepository.findById(id);

        if (existeProducto.isEmpty()) {
            throw new ExceptionDataNotFound("No existe el producto con el id: " + id);
        }
        existeProducto.ifPresent(producto -> {
            log.info("Existe el producto {}", producto);
            log.info("Buscar Variante con el ID de producto  {}", producto.getId());
            List<Variantes> existenVariantes = varianteRepository.findByProductoId(producto.getId());
            log.info("Lista de variantes existentes {}", existenVariantes);
            List<Integer> variablesIds = existenVariantes.stream().map(Variantes::getId).toList();
            log.info("Ids de las variables {}",variablesIds);
            List<VarianteImagen> existenVariblesConImagenes = iVarianteImagenRepository.findByVarianteIdIn(variablesIds);
            log.info("Lista de variables con imagen {}", existenVariblesConImagenes);
            List<Imagen> listImagenes = existenVariblesConImagenes.stream().map(VarianteImagen::getImagen).toList();
            log.info("Lista de imagenes {}",listImagenes);
            List<String> listNombreImageneEliminarDisco = new ArrayList<>();
            List<Long> listaIdsImagenesEliminarBase = new ArrayList<>();

            listImagenes.forEach(imagen -> {
                listNombreImageneEliminarDisco.add(imagen.getBase64());
                listaIdsImagenesEliminarBase.add(imagen.getId());
            });
            log.info("Lista de nombres para eliminar en el disco {} Lista de imagenes a eliminar en la base {}", listNombreImageneEliminarDisco ,  listaIdsImagenesEliminarBase);
            imagenPort.deleteInagenesDisco(listNombreImageneEliminarDisco);
            iVarianteImagenRepository.deleteByVarianteIdIn(variablesIds);
            iImagenService.deleteByIds(listaIdsImagenesEliminarBase);
            producto.setHabilitado((char) 0);
            iProductosRepository.save(producto);
            log.info("Se elimino el producto con las variantes y relaciones con imagenes");

        });
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
                    com.ventas.key.mis.productos.hexagonal.dominio.Imagen img;
                    try {
                        img = imagenProductoClienteAWS.buscarImagenProducto(p.getId());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    dto.setImagen(img);
                    return dto;
                })
                .collect(Collectors.toList());
    }


    @Override
    public Producto saveProductoLote(ProductoDetalle productoDetalle) {
        log.info("Estamos en el inicio del guardado del producto {}",1);
        return guardarProducto(productoDetalle);
    }
    @Transactional
    private Producto guardarProducto(ProductoDetalle productoDetalle) {
        if (productoDetalle.getStock() == 0) {
            throw new RuntimeException("El stock no debe de ser 0");
        }
        if (productoDetalle.getCodigoBarras() == null) {
            throw new RuntimeException("El codigo de barras es requerido");
        }
        try {
            Producto producto = llenarProductoDTO(productoDetalle);
            producto.setHabilitado('1');

            log.info("Se va a guardar el codigo de barras {}",2);
            CodigoBarra codigoBarras = new CodigoBarra();
            codigoBarras.setId(productoDetalle.getCodigoBarras().getId());
            codigoBarras.setCodigoBarras(productoDetalle.getCodigoBarras().getCodigoBarras().isEmpty() ? null : productoDetalle.getCodigoBarras().getCodigoBarras());
            producto.setCodigoBarras(codigoBarras);

            Producto prodExistenteNoOpt = null;
            if( producto.getCodigoBarras().getCodigoBarras() != null ){
                log.info("El codigo de barras no es nul {}",producto.getCodigoBarras().getCodigoBarras());
                prodExistenteNoOpt = this.iProductosRepository
                        .findByCodigoBarras_CodigoBarras(producto.getCodigoBarras().getCodigoBarras())
                        .orElse(null);
                log.info("Se busco el codigo de barras {}", prodExistenteNoOpt);
            }
            if(prodExistenteNoOpt == null) {
                if (!productoDetalle.getListImagenes().isEmpty()){
                    CodigoBarra codigoBarra = new CodigoBarra();
                    codigoBarra.setCodigoBarras(productoDetalle.getCodigoBarras().getCodigoBarras());
                    CodigoBarra codBarr = this.iBarrasService.save(producto.getCodigoBarras());
                    log.info("se guardo el codigo de barras {}", codBarr);
                    producto.setCodigoBarras(codBarr);
                    log.info("Se guardo el producto y se regreso la respuesta {}", producto);
                    return this.iProductosRepository.save(producto);
                }else{
                    CodigoBarra codBarr = this.iBarrasService.save(producto.getCodigoBarras());
                    producto.setCodigoBarras(codBarr);
                }
                return this.iProductosRepository.save(producto);
            }


            if (prodExistenteNoOpt.getCodigoBarras() != null && prodExistenteNoOpt.getCodigoBarras().getId() != 31) {
                List<Imagen> lstImg;
                if (!productoDetalle.getListImagenes().isEmpty()){
                    lstImg = this.iImagenService.saveAll(mappImagenes(productoDetalle.getListImagenes()));
                    List<ProductoImagen> mapperRelacionProductoImagen = mapperRelacionProductoImagen(lstImg, prodExistenteNoOpt);
                    relacionProductoImagen(mapperRelacionProductoImagen);
                }

                // Actualizar los campos del producto existente con los nuevos valores
                prodExistenteNoOpt.setNombre(productoDetalle.getNombre());
                prodExistenteNoOpt.setPrecioCosto(productoDetalle.getPrecioCosto());
                prodExistenteNoOpt.setPiezas(productoDetalle.getPiezas());
                prodExistenteNoOpt.setColor(productoDetalle.getColor());
                prodExistenteNoOpt.setPrecioVenta(productoDetalle.getPrecioVenta());
                prodExistenteNoOpt.setPrecioRebaja(productoDetalle.getPrecioRebaja());
                prodExistenteNoOpt.setDescripcion(productoDetalle.getDescripcion());
                prodExistenteNoOpt.setMarca(productoDetalle.getMarca());
                prodExistenteNoOpt.setContenido(productoDetalle.getContenido());

                // Ajuste de stock contra el stock real de la BD
                if (productoDetalle.getActualizarStock() > 0) {
                    prodExistenteNoOpt.setStock(prodExistenteNoOpt.getStock() + productoDetalle.getActualizarStock());
                } else if (productoDetalle.getEliminarStock() > 0) {
                    prodExistenteNoOpt.setStock(prodExistenteNoOpt.getStock() - productoDetalle.getEliminarStock());
                } else {
                    prodExistenteNoOpt.setStock(productoDetalle.getStock());
                }

                Producto prd = this.iProductosRepository.save(prodExistenteNoOpt);
                log.info("Producto actualizado: {}", prd);
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
        throw new RuntimeException("No se guardo el producto ");
    }

    @Cacheable(value = "findByIdCache", key = "#id")
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

    private void relacionProductoImagen(List<ProductoImagen>  productoImagens) throws IOException {

        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        log.info("Se prepara las imagenes para subirse al servidor {}", 3);
        for (ProductoImagen p : productoImagens) {

                Path path = Paths.get(rutaImagenes, p.getImagen().getBase64());
                byte[] imagenBytes = Files.readAllBytes(path);

            ByteArrayResource recurso = new ByteArrayResource(imagenBytes) {
                @Override
                public String getFilename() {
                    return p.getImagen().getNombreImagen();
                }
            };

            builder.part("files", recurso)
                    .header("Content-Disposition",
                            "form-data; name=files; filename=" + p.getImagen().getNombreImagen());
        }
        log.info("Se envia la peticion al servicio de iamges {}",4);
        //List<ImagenDto> listImg = imageneClienteAWS.save(builder.build());
        log.info("Se guardaron las imagenes en el servidor imageneClienteAWS {}",productoImagens);
        List<RequestProductoImagen> productoImagen = productoImagens.stream().map(datos->{
            RequestProductoImagen prdoImg = new RequestProductoImagen();
            try {
                Optional<ProductoImagen> primer  = Optional.of(productoImagens.stream().findFirst()).orElseThrow();
                primer.ifPresent(imagen -> prdoImg.setProductoId(imagen.getProducto().getId()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            prdoImg.setImagenId(datos.getImagen().getId());
            return prdoImg;
        }).toList();
        log.info("Se guardara la relacion de las imagenes con el producto {}",productoImagen);
        imagenProductoClienteAWS.saveAll(productoImagen);
        log.info("termino de guardar la relacion de los productos {}", 6);

    }


    private List<Imagen> mappImagenes( List<ImagenDTO> list){
        return list.stream().map(mpa->{
            Imagen imagen = new Imagen();
            byte[] decodedBytes = mpa.getBase64();
            String urlImagen = UUID.randomUUID() + "_" + mpa.getNombreImagen();
            Path path = Paths.get(rutaImagenes, urlImagen);
            try {
                File directorio = new File(rutaImagenes);
                if (!directorio.exists()) {
                    directorio.mkdirs();
                }
                Files.write(path, decodedBytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Long idImagen = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
            imagen.setId(idImagen);
            imagen.setBase64(urlImagen);
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

    @Cacheable(value = "obtenerProductosCache", key = "'no-habilitados:' + #page + ':' + #size")
    public PginaDto<List<ProductoDTO>> getProductosNoHabilitados(int size, int page) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Producto> productosPaginados = iProductosRepository.findProductosNoHabilitados(pageable);
        PginaDto<List<ProductoDTO>> pginaDto = new PginaDto<>();
        pginaDto.setPagina(page);
        pginaDto.setTotalPaginas(productosPaginados.getTotalPages());
        pginaDto.setTotalRegistros((int) productosPaginados.getTotalElements());
        pginaDto.setT(productosPaginados.getContent().stream()
                .map(p -> mapperByRol(p, true)).toList());
        return pginaDto;
    }

    @Cacheable(value = "obtenerProductosCache", key = "'sin-stock:' + #page + ':' + #size")
    public PginaDto<List<ProductoDTO>> getProductosSinStock(int size, int page) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Producto> productosPaginados = iProductosRepository.findByStock(0, pageable);
        PginaDto<List<ProductoDTO>> pginaDto = new PginaDto<>();
        pginaDto.setPagina(page);
        pginaDto.setTotalPaginas(productosPaginados.getTotalPages());
        pginaDto.setTotalRegistros((int) productosPaginados.getTotalElements());
        pginaDto.setT(productosPaginados.getContent().stream()
                .map(p -> mapperByRol(p, true)).toList());
        return pginaDto;
    }

    @CacheEvict(value = {"obtenerProductosCache", "buscarNombreOrCodigoBarrasCache", "findByIdCache"}, allEntries = true)
    @Transactional
    public Producto habilitarDeshabilitarProducto(Integer id, boolean habilitar) {
        Producto producto = iProductosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
        producto.setHabilitado(habilitar ? '1' : '0');
        return iProductosRepository.save(producto);
    }

    public byte[] generarReporteProductosSinVariantes() throws IOException {
        List<Producto> productos = iProductosRepository.findProductosSinVariantes();
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Productos Sin Variantes");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Nombre");
            header.createCell(2).setCellValue("Código de Barras");
            header.createCell(3).setCellValue("Stock");
            header.createCell(4).setCellValue("Precio Venta");
            header.createCell(5).setCellValue("Habilitado");
            for (int i = 0; i < productos.size(); i++) {
                Producto p = productos.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getNombre() != null ? p.getNombre() : "");
                row.createCell(2).setCellValue(p.getCodigoBarras() != null ? p.getCodigoBarras().getCodigoBarras() : "");
                row.createCell(3).setCellValue(p.getStock() != null ? p.getStock() : 0);
                row.createCell(4).setCellValue(p.getPrecioVenta() != null ? p.getPrecioVenta() : 0.0);
                row.createCell(5).setCellValue(p.getHabilitado() == '1' ? "Sí" : "No");
            }
            workbook.write(baos);
            return baos.toByteArray();
        }
    }
}
