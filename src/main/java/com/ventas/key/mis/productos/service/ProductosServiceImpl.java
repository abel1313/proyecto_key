package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.Utils.NombreArchivoImagen;
import com.ventas.key.mis.productos.entity.*;
import com.ventas.key.mis.productos.entity.productoVariantes.VarianteImagen;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionDuplicado;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenPort;
import com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto;
import com.ventas.key.mis.productos.hexagonal.infraestructura.ImagenProductoClienteVPS;
import com.ventas.key.mis.productos.mapper.ProductoAdmin;
import com.ventas.key.mis.productos.mapper.ProductoUser;
import com.ventas.key.mis.productos.models.*;
import com.ventas.key.mis.productos.entity.PalabraClave;
import com.ventas.key.mis.productos.repository.ILostesProductosRepository;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IImagenRepository;
import com.ventas.key.mis.productos.repository.IProductoImagenRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.service.api.ICodigoBarrasService;
import com.ventas.key.mis.productos.service.api.IProductoService;
import com.ventas.key.mis.productos.config.RabbitMQConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductosServiceImpl extends
        CrudAbstractServiceImpl<Producto, List<Producto>, Optional<Producto>, Integer, PginaDto<List<Producto>>>
        implements IProductoService {

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
    private final IVarianteRepository varianteRepository;
    private final IVarianteImagenRepository iVarianteImagenRepository;
    private final IProductoImagenRepository iProductoImagenRepository;
    private final IImagenRepository iImagenRepository;
    private final IPalabraClaveRepository iPalabraClaveRepository;

    private final ImagenProductoClienteVPS imagenProductoClienteVPS;
    private final ImagenPort imagenPort;

    @Autowired private CacheService cacheService;
    @Autowired private RabbitTemplate rabbitTemplate;

    public ProductosServiceImpl(final IProductosRepository iProductosRepository,
            final ErrorGenerico error,
            final ILostesProductosRepository iLoteProducto,
            final ICodigoBarrasService iBarrasService,
            final ImagenProductoClienteVPS imagenProductoClienteVPS,
            final IVarianteRepository iVarianteRepository,
            final IVarianteImagenRepository iVarianteImagenRepository,
            final IProductoImagenRepository iProductoImagenRepository,
            final IImagenRepository iImagenRepository,
            final ImagenPort imagenPort,
            final IPalabraClaveRepository iPalabraClaveRepository
    ) {
        super(iProductosRepository, error);
        this.iProductosRepository = iProductosRepository;
        this.error = error;
        this.iLoteProducto = iLoteProducto;
        this.iBarrasService = iBarrasService;
        this.imagenProductoClienteVPS = imagenProductoClienteVPS;
        this.iVarianteImagenRepository = iVarianteImagenRepository;
        this.varianteRepository = iVarianteRepository;
        this.iProductoImagenRepository = iProductoImagenRepository;
        this.iImagenRepository = iImagenRepository;
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
    @Transactional(readOnly = true)
    @Cacheable(value = "obtenerProductosCache",
            key = "#page + ':' + #size + ':' + T(com.ventas.key.mis.productos.Utils.AuthenticationUtils).isAdminContext()")
    public PginaDto<List<ProductoDTO>> getAll(int size, int page) {
        log.info("**********************************************************************");
        log.info("endpointImagenes {}", this.endpointImagenes);
        log.info("**********************************************************************");

        Pageable pageable = PageRequest.of(page - 1, size);
        boolean isAdmin = isAdminContext();
        // Se resuelve la query ANTES de ejecutarla: antes se llamaba siempre a findAll() y para el
        // cliente normal ese resultado se descartaba una linea despues. Como es un Page, eran dos
        // viajes a la BD tirados (el SELECT paginado y su COUNT(*) sin filtro) en cada request del
        // catalogo publico, que es el endpoint mas llamado del sistema.
        Page<Producto> productosPaginados = isAdmin
                ? iProductosRepository.findVisibleParaAdmin(pageable)
                // Cliente normal: solo productos con stock, habilitados Y con al menos una imagen.
                : iProductosRepository.findConStockYImagenPublico(pageable);

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
            // Miniatura para listado/busqueda -- el detalle (findById) sigue usando la imagen completa.
            img.setUrlImagen(endpointImagenes + "v1/imagenes/thumbnail/" + imagenId);
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

    // Proyeccion (productoId, imagenId) en vez de entidades ProductoImagen: de la imagen solo se
    // ocupa el id para armar la URL de la miniatura, y traer la entidad obligaba a Hibernate a
    // cargar ademas cada Imagen (@ManyToOne EAGER) con un SELECT por fila del listado.
    private Map<Integer, Long> getPrimerasImagenes(List<Integer> productoIds) {
        Map<Integer, Long> result = new LinkedHashMap<>();
        if (productoIds.isEmpty()) return result;
        iProductoImagenRepository.findIdsPrimeraImagenByProductoIdIn(productoIds)
            .forEach(fila -> result.putIfAbsent((Integer) fila[0], (Long) fila[1]));
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
        productoAdmin.setFechaCreacion(p.getFechaCreacion());

        return productoAdmin;
    }


    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "buscarNombreOrCodigoBarrasCache",
            key = "#nombre + ':' + #page + ':' + #size + ':' + T(com.ventas.key.mis.productos.Utils.AuthenticationUtils).isAdminContext()")
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
                ? iProductosRepository.buscarProductosAdmin(nombre, null, null, null, null, null, null, pageable)
                : iProductosRepository.buscarProductosAdmin(nombre, true, true, true, null, null, null, pageable);

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
        Producto producto = iProductosRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("No existe el producto con el id: " + id));

        List<Integer> productoIds = List.of(producto.getId());

        // La misma imagen cuelga a la vez del producto (producto_imagen_copy) y de sus variantes
        // (variante_imagen), porque al guardar el producto con fotos estas se replican en las
        // variantes que ya tenia. Hay que juntar los dos lados y borrar ambas relaciones antes de
        // tocar la tabla imagen: borrando solo variante_imagen, la FK de producto_imagen_copy
        // seguia apuntando a la fila y el DELETE moria en un 500 sin motivo visible.
        List<Long> imagenIds = new ArrayList<>(iProductoImagenRepository.findImagenIdsByProductoIdIn(productoIds));
        imagenIds.addAll(iVarianteImagenRepository.findImagenIdsByProductoIdIn(productoIds));

        iVarianteImagenRepository.deleteByProductoIdIn(productoIds);
        iProductoImagenRepository.deleteByProductoIdIn(productoIds);

        if (!imagenIds.isEmpty()) {
            List<Long> huerfanas = iImagenRepository.findOrphanIds(imagenIds);
            if (!huerfanas.isEmpty()) {
                iImagenRepository.deleteByIdIn(huerfanas);
                try {
                    imagenPort.delete(huerfanas);
                } catch (Exception e) {
                    log.warn("No se pudieron eliminar imagenes del microservicio ids={}: {}", huerfanas, e.getMessage());
                }
            }
        }

        producto.setHabilitado('0');
        iProductosRepository.save(producto);
        log.info("Producto id={} dado de baja (habilitado=0) y sus imagenes eliminadas", id);

        cacheService.evictAll();
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        } catch (Exception e) {
            log.warn("No se pudo avisar a Rabbit para invalidar cache de productos (no bloquea el guardado): {}", e.getMessage());
        }
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


    // @CacheEvict va aquí (clase concreta), no en la interfaz: con proxying CGLIB (default de
    // Spring Boot, spring.aop.proxy-target-class=true) las anotaciones de caché puestas solo en
    // el método de la interfaz no se aplican -- el guardado corría igual, pero
    // obtenerProductosCache/buscarNombreOrCodigoBarrasCache/findByIdCache nunca se invalidaban,
    // así que tras guardar/actualizar un producto (ej. sumar stock) el listado y el detalle
    // seguían mostrando el valor viejo hasta que expirara el TTL de Redis o alguien limpiara la
    // caché a mano (AdminController DELETE /cache). Bug reportado 2026-08-28: "tengo 10, agrego
    // 10, y me sigue mostrando 10".
    @Override
    @Transactional
    @CacheEvict(value = {"obtenerProductosCache","buscarNombreOrCodigoBarrasCache","findByIdCache","buscarImagenIdCache","detalleImagen","detalle"}, allEntries = true)
    public Producto saveProductoLote(ProductoDetalle productoDetalle) {
        log.info("Estamos en el inicio del guardado del producto {}",1);
        Producto resultado = guardarProducto(productoDetalle);
        cacheService.evictAll();
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        } catch (Exception e) {
            log.warn("No se pudo avisar a Rabbit para invalidar cache de productos (no bloquea el guardado): {}", e.getMessage());
        }
        return resultado;
    }

    @Override
    public CompartirImagenesVarianteDto compartirImagenesVarianteDto(CompartirImagenesVarianteDto compartirImagenesVarianteDto) {
        Producto producto = iProductosRepository.findById(compartirImagenesVarianteDto.getIdProducto()).orElseThrow(() -> new ExceptionDataNotFound("No existe el producto con el id"));
        List<ProductoImagen> existeImagenes = iProductoImagenRepository.findByProductoId(producto.getId());
        if(existeImagenes.isEmpty()){
            throw new ExceptionDataNotFound("No existen imagenes para este producto ");
        }
        List<Variantes> variantes = varianteRepository.findByProductoId(producto.getId());
        // Pares (variante, imagen) que ya existen: sin este filtro, cada llamada a este endpoint
        // reinsertaba el catalogo completo de imagenes del producto en todas sus variantes, y de
        // ahi salen las variantes con cientos de filas en variante_imagen para 15-16 imagenes
        // reales. Esas filas basura son las que despues hay que leer y ordenar en cada listado.
        Set<String> yaVinculadas = variantes.isEmpty() ? Set.of()
                : iVarianteImagenRepository.findByVarianteIdIn(variantes.stream().map(Variantes::getId).toList())
                    .stream()
                    .map(vi -> vi.getVariante().getId() + ":" + vi.getImagen().getId())
                    .collect(Collectors.toSet());

        List<VarianteImagen> relaciones = new ArrayList<>();
        variantes.forEach(variante ->
            existeImagenes.forEach(imagen -> {
                if (yaVinculadas.contains(variante.getId() + ":" + imagen.getImagen().getId())) return;
                VarianteImagen varianteImagen = new VarianteImagen();
                varianteImagen.setVariante(variante);
                varianteImagen.setImagen(imagen.getImagen());
                relaciones.add(varianteImagen);
            })
        );
        if (!relaciones.isEmpty()) {
            iVarianteImagenRepository.saveAll(relaciones);
        }
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

            log.info("Se va a guardar el codigo de barras {}",2);
            String nuevoCodigoBarrasStr = productoDetalle.getCodigoBarras().getCodigoBarras() == null
                    || productoDetalle.getCodigoBarras().getCodigoBarras().isEmpty()
                    ? null : productoDetalle.getCodigoBarras().getCodigoBarras();
            CodigoBarra codigoBarras = new CodigoBarra();
            codigoBarras.setId(productoDetalle.getCodigoBarras().getId());
            codigoBarras.setCodigoBarras(nuevoCodigoBarrasStr);
            producto.setCodigoBarras(codigoBarras);

            Producto prodExistenteNoOpt = null;
            // Si el front manda el id del producto (edicion real), se busca directo por id: asi
            // si tambien cambia el codigo de barras, se actualiza el mismo producto en vez de
            // crear uno nuevo (ver aclaracion 2026-07-21 en CAMBIOS_FRONT.md).
            if (productoDetalle.getId() != null) {
                prodExistenteNoOpt = this.iProductosRepository.findById(productoDetalle.getId()).orElse(null);
                log.info("Se busco el producto por id {} -> {}", productoDetalle.getId(), prodExistenteNoOpt);
            }
            // Sin id (alta nueva o carga por Excel sin id): se busca por coincidencia exacta de codigo de barras
            if (prodExistenteNoOpt == null && nuevoCodigoBarrasStr != null) {
                log.info("El codigo de barras no es nul {}", nuevoCodigoBarrasStr);
                prodExistenteNoOpt = this.iProductosRepository
                        .findByCodigoBarras_CodigoBarrasIgnoreCase(nuevoCodigoBarrasStr)
                        .orElse(null);
                log.info("Se busco el codigo de barras {}", prodExistenteNoOpt);
            }

            // Solo set habilitado='1' para productos nuevos. Si el producto ya existe (borrador o no),
            // preservar su estado habilitado para no quebrar los borradores de carga-imagenes
            // (codigoBarrasGenerado=true, habilitado=false) — esos no deben cambiar a habilitado='1'
            // hasta que el usuario los complete via CargaImagenesService.completarProducto().
            if (prodExistenteNoOpt == null) {
                producto.setHabilitado('1');
            }

            // Si el producto ya existia y el codigo de barras cambio, se crea el codigo nuevo,
            // se asigna al producto y se elimina el anterior (huerfano: la relacion es 1 a 1 unica)
            if (prodExistenteNoOpt != null && nuevoCodigoBarrasStr != null) {
                String codigoActual = prodExistenteNoOpt.getCodigoBarras() != null
                        ? prodExistenteNoOpt.getCodigoBarras().getCodigoBarras() : null;
                if (!nuevoCodigoBarrasStr.equalsIgnoreCase(codigoActual)) {
                    Optional<Producto> enUsoPorOtro = this.iProductosRepository
                            .findByCodigoBarras_CodigoBarrasIgnoreCase(nuevoCodigoBarrasStr);
                    if (enUsoPorOtro.isPresent() && !enUsoPorOtro.get().getId().equals(prodExistenteNoOpt.getId())) {
                        throw new ExceptionDuplicado("El codigo de barras " + nuevoCodigoBarrasStr + " ya esta en uso por otro producto");
                    }
                    CodigoBarra anterior = prodExistenteNoOpt.getCodigoBarras();
                    CodigoBarra nuevo = new CodigoBarra();
                    nuevo.setCodigoBarras(nuevoCodigoBarrasStr);
                    nuevo = this.iBarrasService.save(nuevo);
                    prodExistenteNoOpt.setCodigoBarras(nuevo);
                    if (anterior != null) {
                        this.iBarrasService.delete(anterior.getId());
                        log.info("Se elimino el codigo de barras anterior huerfano id={} codigo={}", anterior.getId(), anterior.getCodigoBarras());
                    }
                }
            }

            // [FLUJO 2] PRODUCTO NUEVO: no existe en BD → se crea
            if(prodExistenteNoOpt == null) {
                CodigoBarra codBarr = this.iBarrasService.save(producto.getCodigoBarras());
                log.info("se guardo el codigo de barras {}", codBarr);
                producto.setCodigoBarras(codBarr);
                Producto savedProducto = this.iProductosRepository.save(producto);
                log.info("Se guardo el producto nuevo {}", savedProducto);

                if (!productoDetalle.getListImagenes().isEmpty()) {
                    // [FLUJO 3] Los bytes van directo al micro; él asigna el id y escribe el archivo.
                    List<ImagenDto> microImagenes = subirImagenesAlMicro(productoDetalle.getListImagenes());
                    // [FLUJO 4] Con ese id real se guarda la relación en producto_imagen_copy --
                    // la que usa el listado/búsqueda de productos.
                    guardarRelacionLocal(microImagenes, savedProducto);
                    log.info("Se guardaron {} imagenes para el producto nuevo {}", microImagenes.size(), savedProducto.getId());
                }

                if (productoDetalle.getImagenPrincipalId() != null) {
                    aplicarPrincipalProducto(savedProducto.getId(), productoDetalle.getImagenPrincipalId());
                }

                return savedProducto;
            }

            // [FLUJO 2B] PRODUCTO EXISTENTE: ya existe en BD → se actualiza
            if (!productoDetalle.getListImagenes().isEmpty()){
                // [FLUJO 3] Los bytes van directo al micro; él asigna el id y escribe el archivo.
                List<ImagenDto> microImagenes = subirImagenesAlMicro(productoDetalle.getListImagenes());
                // [FLUJO 4] Con ese id real se guarda la relación en producto_imagen_copy.
                guardarRelacionLocal(microImagenes, prodExistenteNoOpt);

                List<Variantes> variantes = varianteRepository.findByProductoId(prodExistenteNoOpt.getId());
                if (!variantes.isEmpty() && !microImagenes.isEmpty()) {
                    // Mismas imágenes ya subidas, con el id real del micro -- antes se usaba
                    // `lstImg` (el id local generado antes de subir, distinto al que el micro
                    // asignó), así que las miniaturas de estas variantes en tienda/buscar tenían
                    // el mismo problema que el listado de productos.
                    List<VarianteImagen> varianteImagenes = new ArrayList<>();
                    for (Variantes variante : variantes) {
                        for (ImagenDto microImagen : microImagenes) {
                            VarianteImagen vi = new VarianteImagen();
                            vi.setVariante(variante);
                            vi.setImagen(iImagenRepository.getReferenceById(microImagen.getId()));
                            varianteImagenes.add(vi);
                        }
                    }
                    iVarianteImagenRepository.saveAll(varianteImagenes);
                    log.info("Se asignaron {} imágenes a {} variantes del producto {}", microImagenes.size(), variantes.size(), prodExistenteNoOpt.getId());
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
                // "Eliminar stock" no validaba contra el stock real -- se podía mandar un valor
                // mayor al disponible y el producto quedaba guardado con stock negativo (bug
                // reportado 2026-08-28: "con la validación que no puede quedar -1").
                if (nuevoStock < 0) {
                    throw new ExceptionErrorInesperado("No se puede eliminar " + productoDetalle.getEliminarStock()
                            + " unidades: solo hay " + prodExistenteNoOpt.getStock() + " en stock");
                }
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
        } catch (Exception e) {
            this.error.error(e);
            throw new RuntimeException("No se guardo el producto: " + e.getMessage(), e);
        }
    }

    // Sin @Cacheable a proposito: un Optional como valor raiz de cache se guarda en Redis como
    // {"empty":true,"present":false} -- sin type id y sin el contenido -- y truena al leerlo de
    // vuelta, o sea que la 2a llamada al detalle reventaba. Ver RedisSerializacionImagenTest.
    public Optional<ProductoResumen> getResumen(int id){
        return Optional.of(this.iProductosRepository.findProductoConImagenes(id));
    }
    // Sube al micro de imagenes los bytes que vinieron en la peticion y devuelve las imagenes
    // con el id que ESE micro les asigno -- el unico id real. El caller lo usa para guardar la
    // relacion en producto_imagen_copy (ver guardarRelacionLocal()).
    //
    // Hasta el 2026-09-18 estos bytes se leian del disco (Files.readAllBytes) porque
    // mappImagenes() escribia antes el archivo con un UUID propio y un id inventado en
    // imagenes_copy. Eso dejaba la MISMA foto dos veces en /app/imagenes -- mismo md5, distinto
    // UUID -- y una fila huerfana que el limpiador nocturno daba por buena porque si estaba en
    // imagenes_copy. El alta de productos (VarianteServiceImpl.subirImagenes()) siempre lo hizo
    // asi: bytes de memoria -> micro -> id.
    //
    // Si el micro no esta disponible se loguea el error pero el producto se guarda igual (lista vacia).
    private List<ImagenDto> subirImagenesAlMicro(List<ImagenDTO> imagenes) {
        if (imagenes.isEmpty()) return List.of();

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        for (ImagenDTO dto : imagenes) {
            byte[] imagenBytes = dto.getBase64();
            final String nombre = NombreArchivoImagen.normalizar(dto.getNombreImagen(), imagenBytes);
            ByteArrayResource recurso = new ByteArrayResource(imagenBytes) {
                @Override
                public String getFilename() { return nombre; }
            };
            builder.part("files", recurso);
        }

        try {
            List<ImagenDto> microImagenes = imagenPort.save(builder.build());
            if (microImagenes == null || microImagenes.isEmpty()) {
                log.warn("El micro de imágenes devolvió lista vacía — imágenes no sincronizadas");
                return List.of();
            }
            log.info("Imágenes subidas al micro, IDs: {}", microImagenes.stream().map(ImagenDto::getId).toList());
            return microImagenes;
        } catch (Exception e) {
            // El mensaje ya viene con el motivo que dio el micro -- ImageneClienteDisco.save()
            // desempaqueta su body antes de propagar.
            log.error("Error al sincronizar imágenes con micro_imagenes — producto guardado pero imágenes no disponibles en micro: {}", e.getMessage(), e);
            return List.of();
        }
    }

    // Persiste producto_imagen_copy (BD local) usando el id REAL que asignó el micro -- ver
    // comentario de subirImagenesAlMicro(). Sin esto, el listado/búsqueda de productos
    // no tiene de dónde sacar el imagenId para armar la miniatura.
    private void guardarRelacionLocal(List<ImagenDto> microImagenes, Producto producto) {
        if (microImagenes.isEmpty()) return;
        List<ProductoImagen> relacionesLocales = java.util.stream.IntStream
                .range(0, microImagenes.size())
                .mapToObj(i -> {
                    ProductoImagen pi = new ProductoImagen();
                    pi.setProducto(producto);
                    pi.setImagen(iImagenRepository.getReferenceById(microImagenes.get(i).getId()));
                    pi.setPrincipal(i == 0);
                    return pi;
                }).toList();
        iProductoImagenRepository.saveAll(relacionesLocales);
        log.info("Relaciones producto-imagen guardadas localmente para productoId={}: {}",
                producto.getId(), microImagenes.stream().map(ImagenDto::getId).toList());
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
            key = "'filtro:' + #nombreOCodigo + ':' + #conStock + ':' + #conImagenes + ':' + #habilitado + ':' + #codigoGenerado + ':' + #fechaDesde + ':' + #fechaHasta + ':' + #page + ':' + #size")
    public PginaDto<List<ProductoDTO>> filtrarProductosAdmin(String nombreOCodigo, Boolean conStock,
            Boolean conImagenes, Boolean habilitado, Boolean codigoGenerado, LocalDate fechaDesde,
            LocalDate fechaHasta, int size, int page) {
        Pageable pageable = PageRequest.of(page - 1, size);
        String texto = (nombreOCodigo != null && !nombreOCodigo.isBlank()) ? nombreOCodigo : null;
        // fechaDesde/fechaHasta llegan como dia calendario (sin hora) -- se expanden al rango
        // completo de ese dia para que "buscar el 22/08" incluya todo desde las 00:00:00 hasta
        // las 23:59:59.999999999, no solo el instante exacto de medianoche.
        LocalDateTime desde = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime hasta = fechaHasta != null ? fechaHasta.atTime(LocalTime.MAX) : null;
        Page<Producto> productosPaginados = iProductosRepository.buscarProductosAdmin(
                texto, conStock, conImagenes, habilitado, codigoGenerado, desde, hasta, pageable);
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

    // Un borrador de la carga rapida NUNCA puede quedar habilitado: todas las consultas del
    // catalogo publico filtran por habilitado='1', asi que habilitarlo aqui lo sacaria a la
    // tienda sin nombre, sin precio y con el codigo placeholder BRD-. El unico camino valido
    // para habilitarlo es PUT /v1/carga-imagenes/{id}/completar, que primero exige el codigo real.
    @Transactional
    public Producto habilitarDeshabilitarProducto(Integer id, boolean habilitar) {
        Producto producto = iProductosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
        if (habilitar && esBorradorCargaRapida(producto)) {
            throw new ExceptionErrorInesperado("No se puede habilitar el producto " + id
                    + ": es un borrador de Carga rapida de imagenes, completalo ahi primero para que"
                    + " se le asigne el codigo de barras real");
        }
        producto.setHabilitado(habilitar ? '1' : '0');
        Producto resultado = iProductosRepository.save(producto);
        cacheService.evictAll();
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        } catch (Exception e) {
            log.warn("No se pudo avisar a Rabbit para invalidar cache de productos (no bloquea el guardado): {}", e.getMessage());
        }
        return resultado;
    }

    @Transactional
    public void habilitarDeshabilitarProductosLote(List<Integer> ids, boolean habilitar) {
        List<Producto> productos = iProductosRepository.findAllById(ids);
        if (habilitar) {
            List<Integer> borradores = productos.stream()
                    .filter(ProductosServiceImpl::esBorradorCargaRapida).map(Producto::getId).toList();
            if (!borradores.isEmpty()) {
                throw new ExceptionErrorInesperado("No se pueden habilitar los productos " + borradores
                        + ": son borradores de Carga rapida de imagenes, completalos ahi primero");
            }
        }
        productos.forEach(p -> p.setHabilitado(habilitar ? '1' : '0'));
        iProductosRepository.saveAll(productos);
        cacheService.evictAll();
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        } catch (Exception e) {
            log.warn("No se pudo avisar a Rabbit para invalidar cache de productos (no bloquea el guardado): {}", e.getMessage());
        }
    }

    // Mismo criterio que IProductosRepository.findBorradores(): el flag O el codigo placeholder.
    private static boolean esBorradorCargaRapida(Producto producto) {
        if (Boolean.TRUE.equals(producto.getCodigoBarrasGenerado())) return true;
        String codigo = producto.getCodigoBarras() != null ? producto.getCodigoBarras().getCodigoBarras() : null;
        return codigo != null && codigo.toUpperCase().startsWith("BRD-");
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

        // La miniatura del listado es un recurso DISTINTO al que se probó arriba -- ver
        // getPrimerasImagenes()/mapperByRol(). Se prueba por separado porque puede fallar
        // aunque la imagen completa (arriba) sí exista.
        Long imagenIdListado = getPrimerasImagenes(List.of(productoId)).get(productoId);
        if (imagenIdListado == null) {
            dto.setMiniaturaPresenteEnMicroservicio(false);
            dto.setDetalleMiniatura("sin imagenId — este producto no tiene ninguna imagen registrada en producto_imagen (BD local)");
        } else {
            boolean miniaturaOk = imagenProductoClienteVPS.verificarMiniatura(imagenIdListado);
            dto.setMiniaturaPresenteEnMicroservicio(miniaturaOk);
            dto.setDetalleMiniatura(miniaturaOk
                    ? "miniatura presente (imagenId=" + imagenIdListado + ")"
                    : "miniatura NO disponible en el microservicio (imagenId=" + imagenIdListado + ") -- esto es lo que hace que el listado/búsqueda se vea sin imagen");
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
