package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.*;
import com.ventas.key.mis.productos.entity.productoVariantes.VarianteImagen;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.hexagonal.dominio.mapper.RequestProductoImagen;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenPort;
import com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto;
import com.ventas.key.mis.productos.hexagonal.infraestructura.ImagenProductoClienteVPS;
import com.ventas.key.mis.productos.mapper.ProductoAdmin;
import com.ventas.key.mis.productos.mapper.ProductoUser;
import com.ventas.key.mis.productos.models.*;
import com.ventas.key.mis.productos.entity.PalabraClave;
import com.ventas.key.mis.productos.repository.ILostesProductosRepository;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IProductoImagenRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.service.api.ICodigoBarrasService;
import com.ventas.key.mis.productos.service.api.IImagenService;
import com.ventas.key.mis.productos.service.api.IProductoService;
import com.ventas.key.mis.productos.config.RabbitMQConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    @Value("${api.imagenes}")
    private String endpointImagenes;

    @jakarta.annotation.PostConstruct
    public void normalizarEndpoints() {
        if (!endpointImagenes.endsWith("/")) endpointImagenes = endpointImagenes + "/";
    }

    private final IProductosRepository iProductosRepository;
    private final ILostesProductosRepository iLoteProducto;
    private final ICodigoBarrasService iBarrasService;
    private final ErrorGenerico error;
    private final IImagenService iImagenService;
    private final IVarianteRepository varianteRepository;
    private final IVarianteImagenRepository iVarianteImagenRepository;
    private final IProductoImagenRepository iProductoImagenRepository;
    private final IPalabraClaveRepository iPalabraClaveRepository;

    private final ImagenProductoClienteVPS imagenProductoClienteVPS;
    private final ImagenPort imagenPort;

    @Autowired private CacheService cacheService;
    @Autowired private RabbitTemplate rabbitTemplate;

    public ProductosServiceImpl(final IProductosRepository iProductosRepository,
            final ErrorGenerico error,
            final ILostesProductosRepository iLoteProducto,
            final ICodigoBarrasService iBarrasService,
            final IImagenService iImagenService,
            final ImagenProductoClienteVPS imagenProductoClienteVPS,
            final IVarianteRepository iVarianteRepository,
            final IVarianteImagenRepository iVarianteImagenRepository,
            final IProductoImagenRepository iProductoImagenRepository,
            final ImagenPort imagenPort,
            final IPalabraClaveRepository iPalabraClaveRepository
    ) {
        super(iProductosRepository, error);
        this.iProductosRepository = iProductosRepository;
        this.error = error;
        this.iLoteProducto = iLoteProducto;
        this.iBarrasService = iBarrasService;
        this.iImagenService = iImagenService;
        this.imagenProductoClienteVPS = imagenProductoClienteVPS;
        this.iVarianteImagenRepository = iVarianteImagenRepository;
        this.varianteRepository = iVarianteRepository;
        this.iProductoImagenRepository = iProductoImagenRepository;
        this.imagenPort = imagenPort;
        this.iPalabraClaveRepository = iPalabraClaveRepository;
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
        log.info("**********************************************************************");
        log.info("endpointImagenes {}", this.endpointImagenes);
        log.info("**********************************************************************");

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Producto> productosPaginados =  iProductosRepository.findAll(pageable);
        boolean isAdmin = isAdminContext();
        if (!isAdmin) {
            // Cliente normal: solo productos con stock, habilitados Y con al menos una imagen.
            productosPaginados = iProductosRepository.findConStockYImagenPublico(pageable);
        }

        List<Integer> productoIds = productosPaginados.getContent().stream().map(Producto::getId).toList();
        Map<Integer, Long> imagenes = getPrimerasImagenes(productoIds);
        PginaDto<List<ProductoDTO>> pginaDto = new PginaDto<>();
        List<ProductoDTO> listPtroductos = productosPaginados.getContent()
                .stream()
                .map(p -> mapperByRol(p, isAdmin, imagenes.get(p.getId())))
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

    private ProductoDTO mapperByRol(Producto p, boolean isAdmin, Long imagenId) {
        com.ventas.key.mis.productos.hexagonal.dominio.Imagen img =
                new com.ventas.key.mis.productos.hexagonal.dominio.Imagen();
        if (imagenId != null) {
            img.setUrlImagen(endpointImagenes + "v1/imagenes/file/" + imagenId);
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

    private Map<Integer, Long> getPrimerasImagenes(List<Integer> productoIds) {
        Map<Integer, Long> result = new LinkedHashMap<>();
        iProductoImagenRepository.findPrimeraImagenByProductoIdIn(productoIds)
            .forEach(pi -> result.putIfAbsent(pi.getProducto().getId(), pi.getImagen().getId()));
        return result;
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
        boolean isAdmin = isAdminContext();

        // Una sola query con OR (nombre / código de barras / palabra clave) en vez de la cascada
        // vieja de hasta 3 llamadas secuenciales que se detenía en el primer paso con resultados
        // -- eso ocultaba productos que solo coincidían por nombre si otro producto ya había
        // matcheado por código. Reusa buscarProductosAdmin (mismo patrón OR ya probado en el
        // filtro de admin): el público fija stock>0 + con imagen + habilitado (tri-state en TRUE
        // en vez de null).
        Page<Producto> resultado = isAdmin
                ? iProductosRepository.buscarProductosAdmin(nombre, null, null, null, pageable)
                : iProductosRepository.buscarProductosAdmin(nombre, true, true, true, pageable);

        if (resultado.isEmpty()) {
            throw new ExceptionDataNotFound("No se encontraron productos con la búsqueda: \"" + nombre + "\"");
        }
        return buildPagina(resultado, page, isAdmin);
    }

    private PginaDto<List<ProductoDTO>> buildPagina(Page<Producto> pagina, int page, boolean isAdmin) {
        List<Integer> productoIds = pagina.getContent().stream().map(Producto::getId).toList();
        Map<Integer, Long> imagenes = getPrimerasImagenes(productoIds);
        PginaDto<List<ProductoDTO>> pginaDto = new PginaDto<>();
        pginaDto.setPagina(page);
        pginaDto.setTotalPaginas(pagina.getTotalPages());
        pginaDto.setTotalRegistros((int) pagina.getTotalElements());
        pginaDto.setT(pagina.getContent().stream()
            .map(p -> mapperByRol(p, isAdmin, imagenes.get(p.getId()))).toList());
        return pginaDto;
    }

    @Transactional
    @Override
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
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
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
                    com.ventas.key.mis.productos.hexagonal.dominio.Imagen img =
                            new com.ventas.key.mis.productos.hexagonal.dominio.Imagen();
                    img.setUrlImagen(endpointImagenes + "v1/producto-imagen/buscarImagenProducto/" + p.getId());
                    dto.setImagen(img);
                    return dto;
                })
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public Producto saveProductoLote(ProductoDetalle productoDetalle) {
        log.info("Estamos en el inicio del guardado del producto {}",1);
        Producto resultado = guardarProducto(productoDetalle);
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        return resultado;
    }

    @Override
    public CompartirImagenesVarianteDto compartirImagenesVarianteDto(CompartirImagenesVarianteDto compartirImagenesVarianteDto) {
        Producto producto = iProductosRepository.findById(compartirImagenesVarianteDto.getIdProducto()).orElseThrow(() -> new ExceptionDataNotFound("No existe el producto con el id"));
        List<ProductoImagen> existeImagenes = iProductoImagenRepository.findByProductoId(producto.getId());
        if(existeImagenes.isEmpty()){
            throw new ExceptionDataNotFound("No existen imagenes para este producto ");
        }
        List<VarianteImagen> relaciones = new ArrayList<>();
        varianteRepository.findByProductoId(producto.getId()).forEach(variante ->
            existeImagenes.forEach(imagen -> {
                VarianteImagen varianteImagen = new VarianteImagen();
                varianteImagen.setVariante(variante);
                varianteImagen.setImagen(imagen.getImagen());
                relaciones.add(varianteImagen);
            })
        );
        iVarianteImagenRepository.saveAll(relaciones);
        return compartirImagenesVarianteDto;
    }

    // [FLUJO 1] INICIO: el controlador llama saveProductoLote() → llega aqui
    @Transactional
    protected Producto guardarProducto(ProductoDetalle productoDetalle) {
        if (productoDetalle.getStock() == 0) {
            throw new ExceptionErrorInesperado("El stock no debe de ser 0");
        }
        if (productoDetalle.getCodigoBarras() == null) {
            throw new ExceptionDataNotFound("El codigo de barras es requerido");
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
                        .findByCodigoBarras_CodigoBarrasIgnoreCase(producto.getCodigoBarras().getCodigoBarras())
                        .orElse(null);
                log.info("Se busco el codigo de barras {}", prodExistenteNoOpt);
            }
            // [FLUJO 2] PRODUCTO NUEVO: no existe en BD → se crea
            if(prodExistenteNoOpt == null) {
                CodigoBarra codBarr = this.iBarrasService.save(producto.getCodigoBarras());
                log.info("se guardo el codigo de barras {}", codBarr);
                producto.setCodigoBarras(codBarr);
                Producto savedProducto = this.iProductosRepository.save(producto);
                log.info("Se guardo el producto nuevo {}", savedProducto);

                if (!productoDetalle.getListImagenes().isEmpty()) {
                    // [FLUJO 3] Genera IDs UUID, guarda archivos en disco y registra en imagenes_copy (BD local)
                    List<Imagen> lstImg = this.iImagenService.saveAll(mappImagenes(productoDetalle.getListImagenes()));
                    List<ProductoImagen> relaciones = mapperRelacionProductoImagen(lstImg, savedProducto);
                    // [FLUJO 4] → pasa a relacionProductoImagen() para publicar a RabbitMQ
                    relacionProductoImagen(relaciones);
                    log.info("Se guardaron {} imagenes para el producto nuevo {}", lstImg.size(), savedProducto.getId());
                }

                if (productoDetalle.getImagenPrincipalId() != null) {
                    aplicarPrincipalProducto(savedProducto.getId(), productoDetalle.getImagenPrincipalId());
                }

                return savedProducto;
            }

            // [FLUJO 2B] PRODUCTO EXISTENTE: ya existe en BD → se actualiza
            if (prodExistenteNoOpt.getCodigoBarras() != null && prodExistenteNoOpt.getCodigoBarras().getId() != 31) {
                if (!productoDetalle.getListImagenes().isEmpty()){
                    // [FLUJO 3] Genera IDs UUID, guarda archivos en disco y registra en imagenes_copy (BD local)
                    List<Imagen> lstImg = this.iImagenService.saveAll(mappImagenes(productoDetalle.getListImagenes()));
                    List<ProductoImagen> mapperRelacionProductoImagen = mapperRelacionProductoImagen(lstImg, prodExistenteNoOpt);
                    // [FLUJO 4] → pasa a relacionProductoImagen() para publicar a RabbitMQ
                    relacionProductoImagen(mapperRelacionProductoImagen);

                    List<Variantes> variantes = varianteRepository.findByProductoId(prodExistenteNoOpt.getId());
                    if (!variantes.isEmpty()) {
                        List<VarianteImagen> varianteImagenes = new ArrayList<>();
                        for (Variantes variante : variantes) {
                            for (Imagen imagen : lstImg) {
                                VarianteImagen vi = new VarianteImagen();
                                vi.setVariante(variante);
                                vi.setImagen(imagen);
                                varianteImagenes.add(vi);
                            }
                        }
                        iVarianteImagenRepository.saveAll(varianteImagenes);
                        log.info("Se asignaron {} imágenes a {} variantes del producto {}", lstImg.size(), variantes.size(), prodExistenteNoOpt.getId());
                    }
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
                if (productoDetalle.getPalabraClaveId() != null) {
                    prodExistenteNoOpt.setPalabraClave(iPalabraClaveRepository.getReferenceById(productoDetalle.getPalabraClaveId()));
                }

                // Ajuste de stock contra el stock real de la BD
                int nuevoStock;
                if (productoDetalle.getActualizarStock() > 0) {
                    nuevoStock = prodExistenteNoOpt.getStock() + productoDetalle.getActualizarStock();
                } else if (productoDetalle.getEliminarStock() > 0) {
                    nuevoStock = prodExistenteNoOpt.getStock() - productoDetalle.getEliminarStock();
                } else {
                    nuevoStock = productoDetalle.getStock();
                }

                ajustarVariantesSiExceden(prodExistenteNoOpt.getId(), nuevoStock);
                prodExistenteNoOpt.setStock(nuevoStock);

                Producto prd = this.iProductosRepository.save(prodExistenteNoOpt);
                log.info("Producto actualizado: {}", prd);

                if (productoDetalle.getImagenPrincipalId() != null) {
                    aplicarPrincipalProducto(prd.getId(), productoDetalle.getImagenPrincipalId());
                }

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

    // [FLUJO 4] Sube archivos al micro de imágenes y guarda la relación producto-imagen en su BD.
    // Si el micro no está disponible se loguea el error pero el producto se guarda igual.
    private void relacionProductoImagen(List<ProductoImagen> productoImagens) throws IOException {
        if (productoImagens.isEmpty()) return;

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        for (ProductoImagen p : productoImagens) {
            Path path = Paths.get(rutaImagenes, p.getImagen().getBase64());
            byte[] imagenBytes = Files.readAllBytes(path);
            final String nombre = p.getImagen().getNombreImagen();
            ByteArrayResource recurso = new ByteArrayResource(imagenBytes) {
                @Override
                public String getFilename() { return nombre; }
            };
            builder.part("files", recurso)
                    .header("Content-Disposition", "form-data; name=files; filename=" + nombre);
        }

        try {
            List<ImagenDto> microImagenes = imagenPort.save(builder.build());
            if (microImagenes == null || microImagenes.isEmpty()) {
                log.warn("El micro de imágenes devolvió lista vacía — imágenes no sincronizadas");
                return;
            }
            log.info("Imágenes subidas al micro, IDs: {}", microImagenes.stream().map(ImagenDto::getId).toList());

            Integer productoId = productoImagens.get(0).getProducto().getId();
            List<ImagenDto> microList = microImagenes;
            List<RequestProductoImagen> relaciones = java.util.stream.IntStream
                    .range(0, microList.size())
                    .mapToObj(i -> {
                        RequestProductoImagen r = new RequestProductoImagen();
                        r.setProductoId(productoId);
                        r.setImagenId(microList.get(i).getId());
                        r.setPrincipal(i == 0);
                        return r;
                    }).toList();

            imagenProductoClienteVPS.saveAll(relaciones);
            log.info("Relaciones producto-imagen guardadas en micro para productoId={}", productoId);
        } catch (Exception e) {
            if (e instanceof WebClientResponseException wcre) {
                log.error("Error al sincronizar imágenes con micro_imagenes — producto guardado pero imágenes no disponibles en micro: {} — body respuesta: {}",
                        e.getMessage(), wcre.getResponseBodyAsString(), e);
            } else {
                log.error("Error al sincronizar imágenes con micro_imagenes — producto guardado pero imágenes no disponibles en micro: {}", e.getMessage(), e);
            }
        }
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
            UUID _uuid = UUID.randomUUID();
            Long idImagen = Math.abs(_uuid.getMostSignificantBits() ^ _uuid.getLeastSignificantBits());
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

    private void aplicarPrincipalProducto(Integer productoId, Long imagenId) {
        iProductoImagenRepository.desmarcarTodosPrincipal(productoId);
        iProductoImagenRepository.marcarComoPrincipal(imagenId, productoId);
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
        if (productoDetalle.getPalabraClaveId() != null) {
            producto.setPalabraClave(iPalabraClaveRepository.getReferenceById(productoDetalle.getPalabraClaveId()));
        }
        return producto;
    }

    @Cacheable(value = "obtenerProductosCache", key = "'no-habilitados:' + #page + ':' + #size")
    public PginaDto<List<ProductoDTO>> getProductosNoHabilitados(int size, int page) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Producto> productosPaginados = iProductosRepository.findProductosNoHabilitados(pageable);
        List<Integer> productoIds = productosPaginados.getContent().stream().map(Producto::getId).toList();
        Map<Integer, Long> imagenes = getPrimerasImagenes(productoIds);
        PginaDto<List<ProductoDTO>> pginaDto = new PginaDto<>();
        pginaDto.setPagina(page);
        pginaDto.setTotalPaginas(productosPaginados.getTotalPages());
        pginaDto.setTotalRegistros((int) productosPaginados.getTotalElements());
        pginaDto.setT(productosPaginados.getContent().stream()
                .map(p -> mapperByRol(p, true, imagenes.get(p.getId()))).toList());
        return pginaDto;
    }

    @Cacheable(value = "obtenerProductosCache", key = "'sin-stock:' + #page + ':' + #size")
    public PginaDto<List<ProductoDTO>> getProductosSinStock(int size, int page) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Producto> productosPaginados = iProductosRepository.findByStock(0, pageable);
        List<Integer> productoIds = productosPaginados.getContent().stream().map(Producto::getId).toList();
        Map<Integer, Long> imagenes = getPrimerasImagenes(productoIds);
        PginaDto<List<ProductoDTO>> pginaDto = new PginaDto<>();
        pginaDto.setPagina(page);
        pginaDto.setTotalPaginas(productosPaginados.getTotalPages());
        pginaDto.setTotalRegistros((int) productosPaginados.getTotalElements());
        pginaDto.setT(productosPaginados.getContent().stream()
                .map(p -> mapperByRol(p, true, imagenes.get(p.getId()))).toList());
        return pginaDto;
    }

    // Filtros de admin: ve TODO el catálogo (sin restricción de stock/habilitado salvo
    // el filtro elegido) — a diferencia de getAll()/findNombreOrCodigoBarra() que para
    // clientes normales exigen stock>0 + habilitado + con imagen.
    @Cacheable(value = "obtenerProductosCache",
            key = "'filtro:' + #nombreOCodigo + ':' + #conStock + ':' + #conImagenes + ':' + #habilitado + ':' + #page + ':' + #size")
    public PginaDto<List<ProductoDTO>> filtrarProductosAdmin(String nombreOCodigo, Boolean conStock,
            Boolean conImagenes, Boolean habilitado, int size, int page) {
        Pageable pageable = PageRequest.of(page - 1, size);
        String texto = (nombreOCodigo != null && !nombreOCodigo.isBlank()) ? nombreOCodigo : null;
        Page<Producto> productosPaginados = iProductosRepository.buscarProductosAdmin(
                texto, conStock, conImagenes, habilitado, pageable);
        List<Integer> productoIds = productosPaginados.getContent().stream().map(Producto::getId).toList();
        Map<Integer, Long> imagenes = getPrimerasImagenes(productoIds);
        PginaDto<List<ProductoDTO>> pginaDto = new PginaDto<>();
        pginaDto.setPagina(page);
        pginaDto.setTotalPaginas(productosPaginados.getTotalPages());
        pginaDto.setTotalRegistros((int) productosPaginados.getTotalElements());
        pginaDto.setT(productosPaginados.getContent().stream()
                .map(p -> mapperByRol(p, true, imagenes.get(p.getId()))).toList());
        return pginaDto;
    }

    @Transactional
    public Producto habilitarDeshabilitarProducto(Integer id, boolean habilitar) {
        Producto producto = iProductosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
        producto.setHabilitado(habilitar ? '1' : '0');
        Producto resultado = iProductosRepository.save(producto);
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        return resultado;
    }

    @Transactional
    public void habilitarDeshabilitarProductosLote(List<Integer> ids, boolean habilitar) {
        List<Producto> productos = iProductosRepository.findAllById(ids);
        productos.forEach(p -> p.setHabilitado(habilitar ? '1' : '0'));
        iProductosRepository.saveAll(productos);
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
    }

    public DiagnosticoImagenProductoDto diagnosticarImagenesProducto(Integer productoId) {
        Producto producto = iProductosRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + productoId));

        DiagnosticoImagenProductoDto dto = new DiagnosticoImagenProductoDto();
        dto.setProductoId(productoId);
        dto.setNombreProducto(producto.getNombre());

        List<ProductoImagen> relaciones = iProductoImagenRepository.findByProductoId(productoId);
        List<ImagenDiagnosticoItem> itemsLocalDB = relaciones.stream()
                .map(pi -> new ImagenDiagnosticoItem(
                        pi.getImagen().getId(),
                        pi.getImagen().getNombreImagen(),
                        pi.getImagen().getExtension(),
                        pi.getImagen().getBase64()))
                .toList();
        dto.setImagenesLocalDB(itemsLocalDB);
        dto.setTotalImagenesLocalDB(itemsLocalDB.size());

        try {
            com.ventas.key.mis.productos.hexagonal.dominio.Imagen imgExterna =
                    imagenProductoClienteVPS.buscarImagenProducto(productoId);
            boolean tieneBytes = imgExterna != null && imgExterna.getImagen() != null;
            dto.setImagenPresenteEnMicroservicio(tieneBytes);
            dto.setDetalleExternoLista(imgExterna == null
                    ? "null — el microservicio no devolvió respuesta"
                    : !tieneBytes
                    ? "respuesta sin bytes de imagen"
                    : "imagen presente con datos");
        } catch (Exception e) {
            dto.setImagenPresenteEnMicroservicio(false);
            dto.setDetalleExternoLista("error al consultar microservicio: " + e.getMessage());
        }

        return dto;
    }

    private void ajustarVariantesSiExceden(Integer productoId, int nuevoStock) {
        List<Variantes> variantes = varianteRepository.findByProductoIdAndHabilitadoOrderByIdDesc(productoId, '1');
        int sumVariantes = variantes.stream().mapToInt(Variantes::getStock).sum();
        if (nuevoStock >= sumVariantes) return;

        int exceso = sumVariantes - nuevoStock;
        for (Variantes v : variantes) {
            if (exceso <= 0) break;
            int quitar = Math.min(v.getStock(), exceso);
            v.setStock(v.getStock() - quitar);
            varianteRepository.save(v);
            exceso -= quitar;
        }
        log.info("ajustarVariantesSiExceden: productoId={} nuevoStock={} sumAntes={}", productoId, nuevoStock, sumVariantes);
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
