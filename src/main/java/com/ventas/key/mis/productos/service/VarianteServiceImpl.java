package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.dto.variantes.IndependizarVarianteRequestDto;
import com.ventas.key.mis.productos.dto.variantes.RequestVarianteDto;
import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.Favorito;
import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.entity.PalabraClave;
import com.ventas.key.hexagonal.articulo.dominio.modelo.ArticuloDeAlta;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.VarianteImagen;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.exeption.ExceptionDuplicado;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenPort;
import com.ventas.key.mis.productos.hexagonal.infraestructura.ImageneClienteDisco;
import com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto;
import com.ventas.key.mis.productos.models.*;
import com.ventas.key.mis.productos.models.variantes.IndependizarVarianteResponseDto;
import com.ventas.key.mis.productos.models.variantes.VarianteDto;
import com.ventas.key.mis.productos.entity.ProductoImagen;
import com.ventas.key.mis.productos.repository.ICodigoBarrasRepository;
import com.ventas.key.mis.productos.repository.IFavoritoRepository;
import com.ventas.key.mis.productos.repository.IImagenRepository;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IProductoImagenRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.service.api.IVarianteService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ventas.key.mis.productos.Utils.NombreArchivoImagen;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VarianteServiceImpl extends CrudAbstractServiceImpl<Variantes, List<Variantes>, Optional<Variantes>, Integer, PginaDto<List<Variantes>>>
        implements IVarianteService {

    private final IVarianteRepository iVarianteRepository;
    private final IVarianteImagenRepository iVarianteImagenRepository;
    private final IProductosRepository iProductosRepository;
    private final IProductoImagenRepository iProductoImagenRepository;
    private final ImageneClienteDisco imageneClienteDisco;
    private final IImagenRepository iImagenRepository;
    private final ImagenPort imagenPort;
    private final IPalabraClaveRepository iPalabraClaveRepository;
    private final ICodigoBarrasRepository iCodigoBarrasRepository;

    @Autowired private IFavoritoRepository iFavoritoRepository;
    @Autowired private EmailService emailService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Value("${api.imagenes}")
    private String endpointImagenes;

    @PostConstruct
    public void normalizarEndpoints() {
        if (!endpointImagenes.endsWith("/")) endpointImagenes = endpointImagenes + "/";
    }

    public VarianteServiceImpl(IVarianteRepository iVarianteRepository,
                               IVarianteImagenRepository iVarianteImagenRepository,
                               IProductosRepository iProductosRepository,
                               IProductoImagenRepository iProductoImagenRepository,
                               ImageneClienteDisco imageneClienteDisco,
                               IImagenRepository iImagenRepository,
                               ImagenPort imagenPort,
                               IPalabraClaveRepository iPalabraClaveRepository,
                               ICodigoBarrasRepository iCodigoBarrasRepository,
                               ErrorGenerico error) {
        super(iVarianteRepository, error);
        this.iVarianteRepository = iVarianteRepository;
        this.iVarianteImagenRepository = iVarianteImagenRepository;
        this.iProductosRepository = iProductosRepository;
        this.iProductoImagenRepository = iProductoImagenRepository;
        this.imageneClienteDisco = imageneClienteDisco;
        this.iImagenRepository = iImagenRepository;
        this.imagenPort = imagenPort;
        this.iPalabraClaveRepository = iPalabraClaveRepository;
        this.iCodigoBarrasRepository = iCodigoBarrasRepository;
    }

    /**
     * El @Cacheable va AQUI y no solo en los metodos delegados: al llamarlos directamente
     * (this.filtrarVariantesAdmin / this.buscarVariantesPublicoFiltrado) la llamada no pasa por el
     * proxy de Spring, asi que sus anotaciones no se aplican y este endpoint nunca cacheaba nada.
     * Es el mismo efecto de self-invocation que con @Transactional.
     *
     * <p>La key incluye el rol a proposito: este metodo bifurca segun admin/publico y devuelve
     * conjuntos distintos para el mismo termino. Sin el rol se reintroduciria el bug que corrigio
     * 005eccd — un cliente normal recibiendo resultados sin filtrar que un admin cacheo antes.
     *
     * <p>Usa variantesProductoCache (el mismo de los metodos delegados) para heredar sus
     * invalidaciones: ImagenServiceImpl lo limpia con allEntries=true al cambiar imagenes.
     */
    @Cacheable(value = "variantesProductoCache",
            key = "'buscar:' + #termino + ':' + #page + ':' + #size + ':' + T(com.ventas.key.mis.productos.Utils.AuthenticationUtils).isAdminContext()")
    public PginaDto<List<VarianteResumenDto>> buscarVariantes(String termino, int page, int size) {
        if (termino == null || termino.isBlank()) {
            return findAllResumen(page, size);
        }

        // Una sola query con OR (nombre / código de barras / palabra clave, + marca en el caso
        // público) en vez de la cascada vieja de hasta 3 llamadas secuenciales que se detenía en
        // el primer paso con resultados -- eso ocultaba variantes que solo coincidían por nombre
        // si otra variante ya había matcheado por código. Reusa los métodos ya probados del
        // filtro de admin/público (mismo patrón OR).
        PginaDto<List<VarianteResumenDto>> resultado = AuthenticationUtils.isAdminContext()
                ? filtrarVariantesAdmin(termino, null, null, null, null, null, null, null, null, null, null, null, page, size)
                : buscarVariantesPublicoFiltrado(termino, null, null, null, null, null, page, size);

        if (resultado.getT().isEmpty()) {
            throw new ExceptionDataNotFound("No se encontraron artículos con la búsqueda: \"" + termino + "\"");
        }
        return resultado;
    }
    // Resolver publico varianteId -> productoId (ficha de producto por link directo, ver
    // VarianteController). No aplica el filtro de visibilidad del catalogo publico (stock,
    // habilitado) a proposito: /variantes/v1/porProducto/{productoId}, que es el endpoint al que
    // el front llama despues con este id, tampoco lo aplica -- mismo criterio que ya rige ese flujo.
    @Cacheable(value = "variantesProductoCache", key = "'productoId:' + #varianteId")
    public Integer resolverProductoId(Integer varianteId) {
        return iVarianteRepository.findProductoIdByVarianteId(varianteId)
                .orElseThrow(() -> new ExceptionDataNotFound("No existe el artículo con id: " + varianteId));
    }

    @Cacheable(value = "variantesProductoCache", key = "#productoId")
    public List<VarianteDto> buscarPorProducto(Integer productoId) {
        return iVarianteRepository.findByProductoId(productoId).stream().map(v -> {
            VarianteDto dto = new VarianteDto();
            dto.setId(v.getId());
            dto.setNombreProducto(v.getProducto().getNombre());
            dto.setTalla(v.getTalla());
            dto.setDescripcion(v.getDescripcion());
            dto.setColor(v.getColor());
            dto.setPresentacion(v.getPresentacion());
            dto.setStock(v.getStock());
            dto.setMarca(v.getMarca());
            dto.setContenidoNeto(v.getContenidoNeto());
            dto.setPrecio(v.precioACobrar() != null ? v.precioACobrar() : 0.0);
            CodigoBarra cb = v.getProducto().getCodigoBarras();
            dto.setCodigoBarras(cb != null ? cb.getCodigoBarras() : null);
            dto.setPalabraClave(v.getPalabraClave() != null
                    ? new com.ventas.key.mis.productos.models.PalabraClaveResumenDto(v.getPalabraClave().getId(), v.getPalabraClave().getNombre())
                    : null);
            dto.setHabilitado(v.getHabilitado());
            return dto;
        }).collect(Collectors.toList());
    }

    @Cacheable(value = "variantesProductoCache", key = "#productoId + ':' + #pagina + ':' + #size")
    public PginaDto<List<Variantes>> buscarPorProductoPaginado(Integer productoId, int pagina, int size) {
        Page<Variantes> page = iVarianteRepository.findByProductoId(productoId, PageRequest.of(pagina - 1, size));
        PginaDto<List<Variantes>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(page.getContent());
        return resultado;
    }

    @Cacheable(value = "variantesNombreCache", key = "#nombre")
    public List<Variantes> buscarPorNombre(String nombre) {
        return iVarianteRepository.findByProductoNombreContainingIgnoreCase(nombre);
    }

    @Cacheable(value = "variantesNombreCache",
            key = "#nombre + ':' + #pagina + ':' + #size + ':' + T(com.ventas.key.mis.productos.Utils.AuthenticationUtils).isAdminContext()")
    public PginaDto<List<Variantes>> buscarPorNombrePaginado(String nombre, int pagina, int size) {
        Page<Variantes> page;
        if(AuthenticationUtils.isAdminContext()){
            page = iVarianteRepository.findByProductoNombreContainingIgnoreCase(nombre, PageRequest.of(pagina - 1, size));
        }else{
            // Cliente normal: stock + habilitado + con imagen.
            page = iVarianteRepository.findByNombrePublico(nombre, PageRequest.of(pagina - 1, size));
        }
        PginaDto<List<Variantes>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(page.getContent());
        return resultado;
    }

    @Cacheable(value = "variantesCodigoBarrasCache", key = "#codigoBarras")
    public List<Variantes> buscarPorCodigoBarras(String codigoBarras) {
        return iVarianteRepository.findByProductoCodigoBarrasCodigoBarras(codigoBarras);
    }

    @Cacheable(value = "variantesCodigoBarrasCache",
            key = "#codigoBarras + ':' + #pagina + ':' + #size + ':' + T(com.ventas.key.mis.productos.Utils.AuthenticationUtils).isAdminContext()")
    public PginaDto<List<Variantes>> buscarPorCodigoBarrasPaginado(String codigoBarras, int pagina, int size) {
        boolean isAdmin = AuthenticationUtils.isAdminContext();
        Page<Variantes> page = null;
        if(isAdmin){
            page = iVarianteRepository.findByProductoCodigoBarrasCodigoBarrasContainingIgnoreCase(codigoBarras, PageRequest.of(pagina - 1, size));
        }else{
            // Cliente normal: stock + habilitado + con imagen.
            page = iVarianteRepository.findByCodigoBarrasPublico(codigoBarras, PageRequest.of(pagina - 1, size));
        }
        PginaDto<List<Variantes>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(page.getContent());
        return resultado;
    }

    @Transactional
    @Override
    public Variantes delete(Integer id) throws Exception {
        Variantes variante = iVarianteRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Artículo no encontrado: " + id));
        variante.setHabilitado('0');
        variante.setStock(0);
        Variantes saved = iVarianteRepository.save(variante);
        evictAllCaches();
        return saved;
    }

    @Transactional
    @Override
    public Boolean guardarVariantesPorProductoConImagenes(RequestVarianteDto requestVarianteDto, MultipartFile[] imagenes) {
        Producto producto = iProductosRepository.findById(requestVarianteDto.getProductoId())
                .orElseThrow(() -> new ExceptionDataNotFound("No existe el producto con id: " + requestVarianteDto.getProductoId()));

        List<Variantes> conStock = obtenerVariantesPorProducto(requestVarianteDto.getProductoId())
                .stream().filter(v -> v.getHabilitado() == '1' && v.getStock() > 0).toList();
        int stockEnVariantes = conStock.stream().mapToInt(Variantes::getStock).sum();

        int stockDisponible = producto.getStock() - stockEnVariantes;
        if (stockDisponible < requestVarianteDto.getCantidadVariantes()) {
            // Los articulos sin foto no salen en la tienda pero si ocupan stock del modelo: sin
            // decirlo, el dueño ve 1 articulo en la tienda y no entiende por que no le alcanza.
            long sinFoto = conStock.stream()
                    .filter(v -> iVarianteImagenRepository.findByVarianteId(v.getId()).isEmpty()).count();
            String detalleSinFoto = sinFoto == 0 ? ""
                    : String.format(" (%d sin foto: no salen en la tienda, búscalos con el filtro \"Sin imágenes\")", sinFoto);
            throw new ExceptionDataNotFound(String.format(
                    "No alcanza el stock para crear %d artículo(s): el modelo tiene %d y sus artículos ya tienen %d%s. "
                            + "Puedes crear %d. Sube el stock del modelo o quítale stock a un artículo.",
                    requestVarianteDto.getCantidadVariantes(), producto.getStock(), stockEnVariantes,
                    detalleSinFoto, Math.max(stockDisponible, 0)));
        }

        List<Long> imageIds = List.of();
        if (requestVarianteDto.isImagenParaTodas()) {
            if (imagenes != null && imagenes.length > 0) {
                imageIds = subirImagenesMultipart(imagenes);

                if (iProductoImagenRepository.findByProductoId(requestVarianteDto.getProductoId()).isEmpty()) {
                    List<ProductoImagen> pis = new ArrayList<>();
                    for (Long imgId : imageIds) {
                        ProductoImagen pi = new ProductoImagen();
                        pi.setProducto(producto);
                        pi.setImagen(iImagenRepository.getReferenceById(imgId));
                        pis.add(pi);
                    }
                    iProductoImagenRepository.saveAll(pis);
                }
            } else {
                imageIds = obtenerImagenPrincipalProducto(requestVarianteDto.getProductoId());
                if (imageIds.isEmpty()) {
                    throw new ExceptionDataNotFound(
                            "El modelo " + producto.getId() + " no tiene una imagen para copiar a los artículos. "
                                    + "Sube una imagen o desmarca la casilla de 'misma imagen para todas'.");
                }
            }
        }

        final List<Long> finalImageIds = imageIds;
        for (int i = 0; i < requestVarianteDto.getCantidadVariantes(); i++) {
            Variantes variante = new Variantes();
            variante.setProducto(producto);
            variante.setStock(1);
            // Nacen con los datos del modelo (R2 del dominio `articulo`); antes nacian vacios y
            // solo mostraban lo que se lee del modelo (nombre, codigo, precio).
            heredarDelModelo(variante, producto);
            Variantes savedVariante = save(variante);
            if (!finalImageIds.isEmpty()) {
                vincularImagenes(savedVariante, finalImageIds);
            }
        }

        evictAllCaches();
        return true;
    }

    /** Color, marca, descripcion, contenido neto y categoria del modelo, para un articulo nuevo. */
    static void heredarDelModelo(Variantes variante, Producto modelo) {
        variante.setColor(modelo.getColor());
        variante.setMarca(modelo.getMarca());
        variante.setDescripcion(modelo.getDescripcion());
        variante.setContenidoNeto(modelo.getContenido());
        variante.setPalabraClave(modelo.getPalabraClave());
    }

    private List<Long> subirImagenesMultipart(MultipartFile[] imagenes) {
        LinkedMultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        for (MultipartFile file : imagenes) {
            try {
                byte[] bytes = file.getBytes();
                String nombre = NombreArchivoImagen.normalizar(file.getOriginalFilename(), bytes);
                ByteArrayResource recurso = new ByteArrayResource(bytes) {
                    @Override
                    public String getFilename() { return nombre; }
                };
                formData.add("files", recurso);
            } catch (Exception e) {
                throw new ExceptionDataNotFound("Error al procesar imagen: " + e.getMessage());
            }
        }
        try {
            return imageneClienteDisco.save(formData).stream().map(ImagenDto::getId).toList();
        } catch (Exception e) {
            log.error("Error al subir imagenes al microservicio de imagenes", e);
            throw new ExceptionDataNotFound("No se pudo subir la imagen al servicio de imagenes, intenta de nuevo");
        }
    }

    /**
     * Imagen principal marcada del producto (ProductoImagen.principal = true); si no hay ninguna
     * marcada como principal, cae a la primera imagen vinculada al producto.
     */
    private List<Long> obtenerImagenPrincipalProducto(Integer productoId) {
        List<ProductoImagen> imagenesProducto = iProductoImagenRepository.findByProductoId(productoId);
        if (imagenesProducto.isEmpty()) {
            return List.of();
        }
        return imagenesProducto.stream()
                .filter(pi -> Boolean.TRUE.equals(pi.getPrincipal()))
                .findFirst()
                .or(() -> imagenesProducto.stream().findFirst())
                .map(pi -> List.of(pi.getImagen().getId()))
                .orElse(List.of());
    }

    /**
     * Independiza una variante en su propio producto: crea un Producto nuevo con codigo de
     * barras propio (la variante nunca tuvo uno, hereda el del producto padre), le copia las
     * imagenes que la variante ya tenia, resta del producto origen el stock que se lleva la
     * variante, y reasigna la variante (su producto_id) al producto nuevo. La variante en si
     * no se borra ni se recrea, solo cambia de dueno — conserva intactas sus propias imagenes
     * (VarianteImagen), talla, color, etc.
     */
    @Transactional
    @Override
    public IndependizarVarianteResponseDto independizarVariante(Integer varianteId, IndependizarVarianteRequestDto request) {
        Variantes variante = iVarianteRepository.findById(varianteId)
                .orElseThrow(() -> new ExceptionDataNotFound("No existe el artículo con id: " + varianteId));

        if (request.getCodigoBarras() == null || request.getCodigoBarras().isBlank()) {
            throw new ExceptionDataNotFound("El codigo de barras es requerido");
        }
        if (iProductosRepository.findByCodigoBarras_CodigoBarrasIgnoreCase(request.getCodigoBarras()).isPresent()) {
            throw new ExceptionDuplicado(
                    "El codigo de barras " + request.getCodigoBarras() + " ya esta en uso por otro producto");
        }

        Producto productoOrigen = variante.getProducto();

        CodigoBarra codigoBarra = new CodigoBarra();
        codigoBarra.setCodigoBarras(request.getCodigoBarras());
        codigoBarra = iCodigoBarrasRepository.save(codigoBarra);

        Producto productoNuevo = new Producto();
        productoNuevo.setNombre(request.getNombre());
        productoNuevo.setDescripcion(request.getDescripcion());
        productoNuevo.setMarca(request.getMarca());
        productoNuevo.setColor(request.getColor());
        productoNuevo.setContenido(request.getContenido());
        productoNuevo.setPiezas(request.getPiezas());
        productoNuevo.setPrecioCosto(request.getPrecioCosto());
        productoNuevo.setPrecioVenta(request.getPrecioVenta());
        productoNuevo.setPrecioRebaja(request.getPrecioRebaja());
        productoNuevo.setStock(variante.getStock());
        productoNuevo.setHabilitado('1');
        productoNuevo.setCodigoBarras(codigoBarra);
        if (request.getPalabraClaveId() != null) {
            productoNuevo.setPalabraClave(iPalabraClaveRepository.getReferenceById(request.getPalabraClaveId()));
        }
        productoNuevo = iProductosRepository.save(productoNuevo);

        List<VarianteImagen> imagenesVariante = iVarianteImagenRepository.findByVarianteId(varianteId);
        if (!imagenesVariante.isEmpty()) {
            Long principalId = request.getImagenPrincipalId();
            if (principalId == null && imagenesVariante.size() == 1) {
                principalId = imagenesVariante.get(0).getImagen().getId();
            }
            final Long principalIdFinal = principalId;
            final Producto productoNuevoFinal = productoNuevo;
            List<ProductoImagen> nuevasRelaciones = imagenesVariante.stream().map(vi -> {
                ProductoImagen pi = new ProductoImagen();
                pi.setProducto(productoNuevoFinal);
                pi.setImagen(vi.getImagen());
                pi.setPrincipal(principalIdFinal != null && principalIdFinal.equals(vi.getImagen().getId()));
                return pi;
            }).toList();
            iProductoImagenRepository.saveAll(nuevasRelaciones);
        }

        productoOrigen.setStock(productoOrigen.getStock() - variante.getStock());
        iProductosRepository.save(productoOrigen);

        variante.setProducto(productoNuevo);
        iVarianteRepository.save(variante);

        evictAllCaches();

        return new IndependizarVarianteResponseDto(
                productoNuevo.getId(), request.getCodigoBarras(), productoOrigen.getStock());
    }

    private List<Variantes> obtenerVariantesPorProducto(int idProducto){
        return iVarianteRepository.findByProductoId(idProducto);
    }

    /**
     * @deprecated Usar getImagenesPorVarianteV2 — no verifica existencia en micro, puede devolver URLs rotas
     */
    @Deprecated
    @Cacheable(value = "variantesImagenesCache", key = "#varianteId")
    public List<ImagenUpdateDto> getImagenesPorVariante(Integer varianteId) {
        List<VarianteImagen> relaciones = filtrarRelacionesConImagen(
                iVarianteImagenRepository.findByVarianteId(varianteId), varianteId);
        return buildImagenUpdateDtos(relaciones);
    }

    /**
     * Descarta relaciones variante-imagen huérfanas (imagen_id apunta a una Imagen que ya no
     * existe o es null) — sin esto, vi.getImagen().getId() truena con NPE y el endpoint
     * responde 500 en vez de simplemente omitir esa imagen rota.
     */
    private List<VarianteImagen> filtrarRelacionesConImagen(List<VarianteImagen> relaciones, Integer varianteId) {
        List<VarianteImagen> validas = relaciones.stream().filter(vi -> vi.getImagen() != null).toList();
        if (validas.size() < relaciones.size()) {
            log.warn("varianteId={} tiene {} relacion(es) variante_imagen huerfana(s) (imagen_id nulo/inexistente), se omiten",
                    varianteId, relaciones.size() - validas.size());
        }
        return validas;
    }

    /**
     * Las imágenes del detalle/carrusel de una variante.
     *
     * <p>Lee (imagen_id, principal) por columna en vez de cargar la entidad Imagen: si el
     * registro local de `imagen` ya no está pero la fila de variante_imagen sí, cargarla como
     * entidad dejaba getImagen() en null y la fila se descartaba entera, así que el carrusel
     * salía vacío mientras el listado —que lee esa misma FK por columna— sí pintaba la foto.
     * El archivo vive en el micro, que es quien manda: basta el id para armar la URL.
     *
     * <p><b>Devuelve ArrayList a proposito, no List.of() ni .toList().</b> El valor raiz de un
     * @Cacheable va a Redis con GenericJackson2JsonRedisSerializer + activateDefaultTyping
     * NON_FINAL (ver CacheTtlConfig): las listas inmutables son clases final, asi que NO llevan
     * el type id y al leerlas de vuelta Jackson truena con "expected VALUE_STRING: need ...
     * type id". El controller se come esa excepcion y responde [], o sea: la 1a llamada trae las
     * fotos y la 2a el carrusel sale vacio hasta que se limpia la cache. Mismo bug que tumbo el
     * login el 2026-09-08 (ver ImagenPresentacionService). Dentro de un objeto contenedor
     * (PginaDto) una lista inmutable si funciona -- el problema es solo en la raiz.
     */
    @Cacheable(value = "variantesImagenesCache", key = "'v2:' + #varianteId")
    public List<ImagenUpdateDto> getImagenesPorVarianteV2(Integer varianteId) {
        List<Object[]> filas = iVarianteImagenRepository.findImagenIdsConPrincipalByVarianteId(varianteId);
        List<Long> ids = filas.stream().map(f -> (Long) f[0]).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) return new ArrayList<>();

        List<Long> existentesList;
        try {
            existentesList = imageneClienteDisco.verificarExistentes(ids);
        } catch (Exception e) {
            log.warn("Error verificando existencia en micro para varianteId={}: {}", varianteId, e.getMessage());
            existentesList = List.of();
        }
        // Micro sin responder: se mandan todas igual. Una URL que quizá falle es mejor que un
        // carrusel vacío — el navegador se salta la rota y las buenas se siguen viendo.
        if (existentesList.isEmpty()) {
            log.warn("verificarExistentes vacío para varianteId={}, se mandan todas las URLs", varianteId);
        }
        Set<Long> aMostrar = existentesList.isEmpty() ? Set.copyOf(ids) : Set.copyOf(existentesList);

        // extension/nombre solo existen si la Imagen local sigue ahí; el carrusel pinta con la
        // URL, así que una huérfana viaja con esos campos en null en vez de perderse.
        Map<Long, Imagen> locales = iImagenRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Imagen::getId, i -> i, (a, b) -> a));

        return filas.stream()
                .filter(f -> f[0] != null && aMostrar.contains((Long) f[0]))
                .map(f -> {
                    Long imagenId = (Long) f[0];
                    Imagen local = locales.get(imagenId);
                    ImagenUpdateDto dto = new ImagenUpdateDto(imagenId, (byte[]) null,
                            local != null ? local.getExtension() : null,
                            local != null ? local.getNombreImagen() : null);
                    dto.setUrlImagen(endpointImagenes + "v1/imagenes/file/" + imagenId);
                    dto.setPrincipal((Boolean) f[1]);
                    return dto;
                }).collect(Collectors.toCollection(ArrayList::new));
    }

    @Cacheable(value = "variantesImagenesCache", key = "#varianteId + ':' + #pagina + ':' + #size")
    public PginaDto<List<ImagenUpdateDto>> getImagenesPorVariantePaginado(Integer varianteId, int pagina, int size) {
        List<VarianteImagen> todas = filtrarRelacionesConImagen(
                iVarianteImagenRepository.findByVarianteId(varianteId), varianteId);
        if (todas.isEmpty()) {
            PginaDto<List<ImagenUpdateDto>> vacio = new PginaDto<>();
            vacio.setPagina(pagina);
            vacio.setTotalPaginas(0);
            vacio.setTotalRegistros(0);
            vacio.setT(List.of());
            return vacio;
        }

        List<Long> imagenIds = todas.stream().map(vi -> vi.getImagen().getId()).toList();
        List<Long> existentesList;
        try {
            existentesList = imageneClienteDisco.verificarExistentes(imagenIds);
        } catch (Exception e) {
            log.warn("Error verificando imágenes en micro para varianteId={}: {}", varianteId, e.getMessage());
            existentesList = List.of();
        }

        // Si la verificación devuelve vacío, usar BD local como fallback (consistente con el listado).
        List<VarianteImagen> conImagen;
        if (existentesList.isEmpty()) {
            log.warn("verificarExistentes vacío para varianteId={} (paginado), usando BD local como fallback", varianteId);
            conImagen = todas;
        } else {
            Set<Long> existentes = new HashSet<>(existentesList);
            conImagen = todas.stream()
                    .filter(vi -> existentes.contains(vi.getImagen().getId()))
                    .toList();
        }

        List<ImagenUpdateDto> dtos = buildImagenUpdateDtos(conImagen);
        int fromIndex = (pagina - 1) * size;
        int toIndex = Math.min(fromIndex + size, dtos.size());
        List<ImagenUpdateDto> paginado = fromIndex >= dtos.size() ? List.of() : dtos.subList(fromIndex, toIndex);
        int totalPaginas = size == 0 ? 0 : (int) Math.ceil((double) dtos.size() / size);

        PginaDto<List<ImagenUpdateDto>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(totalPaginas);
        resultado.setTotalRegistros(dtos.size());
        resultado.setT(paginado);
        return resultado;
    }

    private List<ImagenUpdateDto> buildImagenUpdateDtos(List<VarianteImagen> relaciones) {
        if (relaciones.isEmpty()) return new ArrayList<>();
        return relaciones.stream().map(vi -> {
            var img = vi.getImagen();
            ImagenUpdateDto dto = new ImagenUpdateDto(img.getId(), (byte[]) null, img.getExtension(), img.getNombreImagen());
            dto.setUrlImagen(endpointImagenes + "v1/imagenes/file/" + img.getId());
            dto.setPrincipal(vi.getPrincipal());
            return dto;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    @Transactional
    public void marcarImagenPrincipalVariante(Integer varianteImagenId) {
        VarianteImagen target = iVarianteImagenRepository.findById(varianteImagenId)
                .orElseThrow(() -> new ExceptionDataNotFound("Relación artículo-imagen no encontrada: " + varianteImagenId));
        aplicarPrincipalVariante(target.getVariante().getId(), target.getImagen().getId());
        evictAllCaches();
    }

    private void aplicarPrincipalVariante(Integer varianteId, Long imagenId) {
        iVarianteImagenRepository.findAllByVarianteId(varianteId).forEach(vi -> {
            vi.setPrincipal(vi.getImagen().getId().equals(imagenId));
            iVarianteImagenRepository.save(vi);
        });
    }

    @Transactional
    public List<Variantes> guardarConImagenes(List<VarianteDetalle> detalles) throws ExceptionDataNotFound {
        // Antes de nada: sacar los articulos que no describen nada (R1 del dominio `articulo`).
        // La pantalla de alta tiene el formulario base y la seccion de varias tallas, y son
        // independientes; si alguien llena el base, agrega 2 tallas y despues vacia el base, el
        // base seguia viajando y se guardaban 3 articulos -- el tercero sin talla, sin color y
        // sin nada. Es el "dice que voy a guardar 3 cuando agregue 2" (reportado 2026-09-22).
        detalles = soloLosQueDescribenAlgo(detalles);
        if (detalles.isEmpty()) {
            throw new ExceptionDataNotFound(
                    "No hay ningun articulo que guardar: todos llegaron vacios. Hay que llenar al "
                            + "menos la talla, el color u otro dato, o ponerle stock");
        }

        aplicarAjusteStockModelo(detalles);
        validarStockContraProducto(detalles);
        List<List<Long>> imagenesPorArticulo = repartirImagenes(detalles, subirImagenes(detalles));

        List<Variantes> resultado = new ArrayList<>();
        for (int i = 0; i < detalles.size(); i++) {
            VarianteDetalle detalle = detalles.get(i);
            boolean esRestock = false;
            if (detalle.getId() != null) {
                esRestock = esRestock(detalle);
            }

            Variantes saved = save(buildVariante(detalle));
            resultado.add(saved);

            if (esRestock) {
                notificarRestock(saved);
            }

            List<Long> imageIds = new ArrayList<>(imagenesPorArticulo.get(i));
            // A8: la foto del modelo se reusa, no se vuelve a subir. Si el articulo se da de baja,
            // la foto no se borra: findOrphanIds la ve todavia en producto_imagen_copy.
            if (Boolean.TRUE.equals(detalle.getUsarImagenDelModelo())) {
                imageIds.addAll(obtenerImagenPrincipalProducto(detalle.getProductoId()));
            }
            if (!imageIds.isEmpty()) {
                vincularImagenes(saved, imageIds);
            }

            if (detalle.getImagenPrincipalId() != null) {
                aplicarPrincipalVariante(saved.getId(), detalle.getImagenPrincipalId());
            }
        }
        evictAllCaches();
        return resultado;
    }

    /**
     * Que fotos subidas le tocan a cada articulo del alta, en el mismo orden que {@code detalles}.
     *
     * <p>{@code subidas} son los ids que devolvio el micro, en el orden en que se mandaron (todas las
     * de cada detalle, detalle por detalle). Un detalle con {@code imagenesPropias=true} se queda
     * solo con las suyas; los demas comparten las de todos los que no son propias -- que es lo que
     * siempre hizo Agregar articulo con varias tallas (la foto va en el primero y la llevan todos).
     */
    static List<List<Long>> repartirImagenes(List<VarianteDetalle> detalles, List<Long> subidas) {
        boolean hayPropias = detalles.stream().anyMatch(d -> Boolean.TRUE.equals(d.getImagenesPropias()));
        if (!hayPropias) {
            // Como siempre: todas las fotos del alta para todos los articulos.
            return detalles.stream().map(d -> List.copyOf(subidas)).toList();
        }
        int esperadas = detalles.stream().mapToInt(d -> d.getListImagenes() == null ? 0 : d.getListImagenes().size()).sum();
        if (subidas.size() != esperadas) {
            throw new ExceptionErrorInesperado(String.format(
                    "No se pudieron subir todas las fotos (se mandaron %d y llegaron %d). Intenta de nuevo", esperadas, subidas.size()));
        }
        List<List<Long>> propias = new ArrayList<>();
        List<Long> compartidas = new ArrayList<>();
        int cursor = 0;
        for (VarianteDetalle d : detalles) {
            int n = d.getListImagenes() == null ? 0 : d.getListImagenes().size();
            List<Long> suyas = subidas.subList(cursor, cursor + n);
            cursor += n;
            propias.add(suyas);
            if (!Boolean.TRUE.equals(d.getImagenesPropias())) {
                compartidas.addAll(suyas);
            }
        }
        List<List<Long>> resultado = new ArrayList<>();
        for (int i = 0; i < detalles.size(); i++) {
            resultado.add(Boolean.TRUE.equals(detalles.get(i).getImagenesPropias())
                    ? List.copyOf(propias.get(i)) : List.copyOf(compartidas));
        }
        return resultado;
    }

    private List<Long> subirImagenes(List<VarianteDetalle> detalles) {
        List<ImagenDTO> todas = detalles.stream()
                .filter(d -> d.getListImagenes() != null && !d.getListImagenes().isEmpty())
                .flatMap(d -> d.getListImagenes().stream())
                .toList();
        if (todas.isEmpty()) return List.of();

        LinkedMultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        for (ImagenDTO dto : todas) {
            byte[] bytes = dto.getBase64();
            String nombre = NombreArchivoImagen.normalizar(dto.getNombreImagen(), bytes);
            ByteArrayResource recurso = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() { return nombre; }
            };
            formData.add("files", recurso);
        }
        return imageneClienteDisco.save(formData).stream().map(ImagenDto::getId).toList();
    }

    /**
     * Vincula imagenes a la variante saltandose las que ya estaban vinculadas. Sin este filtro
     * cada nueva llamada volvia a insertar el mismo par (variante_id, imagen_id): en QA hay
     * variantes con 778 filas en variante_imagen para 16 imagenes reales, y esas filas basura
     * son las que despues hay que leer y ordenar en cada listado.
     */
    private void vincularImagenes(Variantes variante, List<Long> imageIds) {
        Set<Long> yaVinculadas = new HashSet<>(
                iVarianteImagenRepository.findImagenIdsByVarianteIdIn(List.of(variante.getId())));
        List<VarianteImagen> relaciones = imageIds.stream()
                .distinct()
                .filter(imgId -> !yaVinculadas.contains(imgId))
                .map(imgId -> {
                    VarianteImagen vi = new VarianteImagen();
                    vi.setVariante(variante);
                    vi.setImagen(iImagenRepository.getReferenceById(imgId));
                    return vi;
                }).toList();
        if (relaciones.isEmpty()) return;
        iVarianteImagenRepository.saveAll(relaciones);
    }

    /**
     * B1 de PLAN_ALTA_MODELO_Y_ARTICULOS.md: desde Agregar articulo se le puede subir o bajar el
     * stock al modelo sin salir de la pantalla. Va en la misma transaccion que el articulo: si el
     * articulo no se guarda, el modelo tampoco cambia. Pide el mismo permiso que actualizar el
     * modelo (Escritura en Modelos, Agregar modelo o Nuevo producto).
     */
    private void aplicarAjusteStockModelo(List<VarianteDetalle> detalles) {
        Map<Integer, Integer> ajustes = new LinkedHashMap<>();
        for (VarianteDetalle d : detalles) {
            Integer ajuste = d.getAjusteStockModelo();
            if (d.getProductoId() != null && ajuste != null && ajuste != 0) {
                ajustes.putIfAbsent(d.getProductoId(), ajuste);
            }
        }
        if (ajustes.isEmpty()) {
            return;
        }
        if (!puedeActualizarModelo()) {
            throw new ExceptionErrorInesperado(
                    "No tienes permiso para cambiar el stock del modelo. Pídele a alguien con permiso de editar modelos");
        }
        for (Map.Entry<Integer, Integer> e : ajustes.entrySet()) {
            Producto producto = iProductosRepository.findById(e.getKey())
                    .orElseThrow(() -> new ExceptionDataNotFound("Producto no encontrado: " + e.getKey()));
            int actual = producto.getStock() != null ? producto.getStock() : 0;
            int nuevo = actual + e.getValue();
            int repartido = iVarianteRepository.findByProductoId(e.getKey()).stream()
                    .filter(v -> v.getHabilitado() == '1').mapToInt(Variantes::getStock).sum();
            if (nuevo < repartido) {
                throw new ExceptionErrorInesperado(String.format(
                        "No se puede dejar el modelo en %d: ya tiene %d repartidos en sus artículos", nuevo, repartido));
            }
            producto.setStock(nuevo);
            iProductosRepository.save(producto);
            log.info("Stock del modelo {} ajustado desde Agregar artículo: {} -> {}", e.getKey(), actual, nuevo);
        }
    }

    private static boolean puedeActualizarModelo() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        Set<String> permitidas = new HashSet<>(List.of("ROLE_ADMIN"));
        for (String ruta : List.of("productos/buscar", "productos/agregar", "tienda/venta")) {
            permitidas.add(com.ventas.key.mis.productos.filter.JwtAuthenticationFilter.PREFIJO_AUTORIDAD_PANTALLA
                    + ruta + com.ventas.key.mis.productos.filter.JwtAuthenticationFilter.SUFIJO_AUTORIDAD_ESCRITURA);
        }
        return auth.getAuthorities().stream().anyMatch(a -> permitidas.contains(a.getAuthority()));
    }

    private void validarStockContraProducto(List<VarianteDetalle> detalles) {
        Map<Integer, List<VarianteDetalle>> porProducto = detalles.stream()
                .collect(Collectors.groupingBy(VarianteDetalle::getProductoId));

        for (Map.Entry<Integer, List<VarianteDetalle>> entry : porProducto.entrySet()) {
            Integer productoId = entry.getKey();
            List<VarianteDetalle> variantesRequest = entry.getValue();

            Producto producto = iProductosRepository.findById(productoId)
                    .orElseThrow(() -> new ExceptionDataNotFound("Producto no encontrado: " + productoId));

            Set<Integer> idsActualizando = variantesRequest.stream()
                    .filter(v -> v.getId() != null)
                    .map(VarianteDetalle::getId)
                    .collect(Collectors.toSet());

            // Solo las variantes HABILITADAS retienen stock. Una dada de baja ya no existe para
            // el negocio y su stock vuelve a estar disponible -- antes se sumaban todas, asi que
            // dar de baja una variante no liberaba nada y el disponible bajaba para siempre. En
            // produccion eso dejaba productos en "Disponible: 0" con variantes muertas reteniendo
            // todo, y la unica salida era inflar el stock del producto a mano (2026-09-22).
            // Incluye a las que se estan editando, con su stock ACTUAL: lo que se valida abajo es el
            // aumento, y el aumento solo puede salir de lo que nadie tiene asignado. Excluirlas
            // contaba su stock actual como libre y dejaba pasar base 10 con A=5 y B=5 -> A a 7.
            int stockYaAsignado = iVarianteRepository.findByProductoId(productoId).stream()
                    .filter(v -> v.getHabilitado() == '1')
                    .mapToInt(Variantes::getStock)
                    .sum();

            Map<Integer, Integer> stockActualPorVariante = idsActualizando.isEmpty()
                    ? Map.of()
                    : iVarianteRepository.findAllById(idsActualizando).stream()
                            .collect(Collectors.toMap(Variantes::getId, Variantes::getStock));

            // Lo que hay que pedirle al producto es el AUMENTO, no el total. Una variante que ya
            // tenia 3 y sigue con 3 no consume nada nuevo: antes se sumaba su total, asi que
            // renombrar una variante fallaba por falta de stock aunque el stock no cambiara.
            int stockSolicitado = variantesRequest.stream()
                    .mapToInt(v -> v.getId() == null
                            ? v.getStock()
                            : v.getStock() - stockActualPorVariante.getOrDefault(v.getId(), 0))
                    .sum();

            if (stockSolicitado <= 0) {
                continue;   // no pide nada nuevo (o libera): no hay nada que validar
            }

            int stockDisponible = producto.getStock() - stockYaAsignado;

            if (stockSolicitado > stockDisponible) {
                throw new ExceptionDataNotFound(
                        String.format("Stock insuficiente en el modelo '%s' (id=%d). Disponible: %d, Solicitado: %d",
                                producto.getNombre(), productoId, stockDisponible, stockSolicitado));
            }
        }
    }

    /**
     * Mover stock de una variante NO toca el stock base del producto: el base es el total fisico
     * y las variantes solo se reparten lo que hay. Subirle 3 a una variante consume 3 del
     * disponible (base - suma de variantes habilitadas), no crea 3 unidades nuevas. Para eso
     * esta la pantalla del producto: si llega mercancia, se sube ahi el stock base y recien
     * entonces hay mas disponible que repartir.
     *
     * <p>Antes este metodo hacia producto.stock += (nuevo - viejo), asi que editar una variante
     * inflaba el total: base 10 con una variante en 2, editarla a 5 dejaba el producto en 13 y el
     * disponible seguia en 8 -- se podia repartir indefinidamente stock que no existia.
     *
     * @return true si este ajuste hace que la variante pase de sin stock a con stock (0 -> N).
     */
    private boolean esRestock(VarianteDetalle detalle) throws ExceptionDataNotFound {
        // El front manda el stock final ya calculado (actual + agregar - quitar) -- guard acá
        // por si llega negativo de todos modos: validarStockContraProducto() suma stocks
        // solicitados y solo revienta si el TOTAL excede lo disponible, así que un valor
        // negativo aislado no lo detecta (reduce la suma en vez de superarla).
        if (detalle.getStock() < 0) {
            throw new ExceptionDataNotFound("El stock del artículo no puede quedar negativo");
        }
        Variantes actual = iVarianteRepository.findById(detalle.getId())
                .orElseThrow(() -> new ExceptionDataNotFound("Artículo no encontrado: " + detalle.getId()));
        return actual.getStock() == 0 && detalle.getStock() > 0;
    }

    /**
     * Avisa por correo a quienes tienen esta variante en Favoritos de que volvió a haber stock.
     * No hay bandera de "ya avisado" en BD -- se apoya en que esRestock() solo devuelve true en
     * la transición real 0->N, así que una variante que ya tiene stock no vuelve a dispararlo
     * hasta que se agote y se reabastezca de nuevo. Nunca debe tumbar el guardado de la variante
     * si el envío falla.
     */
    private void notificarRestock(Variantes variante) {
        try {
            List<Favorito> favoritos = iFavoritoRepository.findAllByVariante_Id(variante.getId());
            if (favoritos.isEmpty()) return;
            String nombreProducto = variante.getProducto() != null ? variante.getProducto().getNombre() : "un producto";
            String detalleVariante = descripcionVariante(variante);
            for (Favorito favorito : favoritos) {
                Cliente cliente = favorito.getCliente();
                if (cliente == null || !Boolean.TRUE.equals(cliente.getRecibirCorreos())) continue;
                String correo = cliente.getCorreoElectronico();
                if (correo == null || correo.isBlank()) continue;
                emailService.enviarAlertaStock(correo, cliente.getNombrePersona(), nombreProducto, detalleVariante);
            }
        } catch (Exception e) {
            log.warn("No se pudo notificar restock de variante id={}: {}", variante.getId(), e.getMessage());
        }
    }

    private String descripcionVariante(Variantes v) {
        StringBuilder sb = new StringBuilder();
        if (v.getTalla() != null && !v.getTalla().isBlank()) sb.append("Talla ").append(v.getTalla());
        if (v.getColor() != null && !v.getColor().isBlank()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(v.getColor());
        }
        return sb.toString();
    }

    /**
     * Descarta los articulos vacios del alta, preguntandole al dominio (R1).
     *
     * <p>Aqui solo vive la traduccion del DTO al modelo; la regla de que cuenta como articulo esta
     * en {@link ArticuloDeAlta}, para poder leerla sola y porque va a seguir valiendo cuando
     * `variante` pase a llamarse `articulo`.
     */
    private List<VarianteDetalle> soloLosQueDescribenAlgo(List<VarianteDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return List.of();
        }
        List<VarianteDetalle> reales = detalles.stream()
                .filter(d -> aArticuloDeAlta(d).describeAlgo())
                .toList();

        int descartados = detalles.size() - reales.size();
        if (descartados > 0) {
            log.info("Alta de articulos: se descartaron {} de {} por venir vacios (sin datos "
                    + "propios, sin imagenes y sin stock)", descartados, detalles.size());
        }
        return reales;
    }

    private static ArticuloDeAlta aArticuloDeAlta(VarianteDetalle d) {
        boolean traeImagenes = d.getListImagenes() != null && !d.getListImagenes().isEmpty();
        return new ArticuloDeAlta(
                d.getId(), d.getTalla(), d.getColor(), d.getMarca(), d.getDescripcion(),
                d.getPresentacion(), d.getContenidoNeto(), d.getStock(), traeImagenes,
                d.getPalabraClaveId());
    }

    private Variantes buildVariante(VarianteDetalle detalle) {
        Variantes v = new Variantes();
        if (detalle.getId() != null) v.setId(detalle.getId());
        v.setProducto(iProductosRepository.getReferenceById(detalle.getProductoId()));
        // Categoria y datos basicos se heredan del modelo cuando el articulo nuevo no trae los suyos
        // (R2 y R3 del dominio `articulo`). Lo que si trae se conserva.
        Producto modelo = iProductosRepository.getReferenceById(detalle.getProductoId());
        ArticuloDeAlta alta = aArticuloDeAlta(detalle);
        v.setTalla(detalle.getTalla());
        v.setColor(alta.datoEfectivo(detalle.getColor(), modelo.getColor()));
        v.setMarca(alta.datoEfectivo(detalle.getMarca(), modelo.getMarca()));
        v.setStock(detalle.getStock());
        v.setDescripcion(alta.datoEfectivo(detalle.getDescripcion(), modelo.getDescripcion()));
        v.setPresentacion(detalle.getPresentacion());
        v.setContenidoNeto(alta.datoEfectivo(detalle.getContenidoNeto(), modelo.getContenido()));
        Integer categoria = alta
                .categoriaEfectiva(modelo.getPalabraClave() != null ? modelo.getPalabraClave().getId() : null);
        if (categoria != null) {
            v.setPalabraClave(iPalabraClaveRepository.getReferenceById(categoria));
        }
        return v;
    }

    @Cacheable(value = "variantesProductoCache",
            key = "'resumen:all:' + #pagina + ':' + #size + ':' + T(com.ventas.key.mis.productos.Utils.AuthenticationUtils).isAdminContext()")
    public PginaDto<List<VarianteResumenDto>> findAllResumen(int pagina, int size) {
        return toResumenPagina(findAllNew(pagina, size));
    }

    @Override
    public PginaDto<List<Variantes>> findAllNew(int pagina, int size){
        PginaDto<List<Variantes>> pginaDto = new PginaDto<>();
        Pageable pageable = PageRequest.of(pagina - 1, size);
        Page<Variantes> dataPaginacion;
        if(AuthenticationUtils.isAdminContext()){
            dataPaginacion = this.iVarianteRepository.findVisibleParaAdmin(pageable);
        }else{
            // Cliente normal: stock + habilitado + con imagen.
            dataPaginacion = this.iVarianteRepository.findConStockYImagenPublico(pageable);
        }
        pginaDto.setPagina(pagina);
        pginaDto.setTotalPaginas(dataPaginacion.getTotalPages());
        pginaDto.setTotalRegistros((int) dataPaginacion.getTotalElements());
        pginaDto.setT(dataPaginacion.getContent() );
        return pginaDto;
    }

    private PginaDto<List<VarianteResumenDto>> toResumenPagina(PginaDto<List<Variantes>> origen) {
        PginaDto<List<VarianteResumenDto>> resultado = new PginaDto<>();
        resultado.setPagina(origen.getPagina());
        resultado.setTotalPaginas(origen.getTotalPaginas());
        resultado.setTotalRegistros(origen.getTotalRegistros());
        resultado.setT(buildResumenDtosBatch(origen.getT()));
        return resultado;
    }

@Cacheable(value = "variantesProductoCache", key = "'resumen:' + #productoId + ':' + #pagina + ':' + #size + ':' + T(com.ventas.key.mis.productos.Utils.AuthenticationUtils).isAdminContext()")
    public PginaDto<List<VarianteResumenDto>> buscarPorProductoPaginadoResumen(Integer productoId, int pagina, int size) {
        Page<Variantes> page = iVarianteRepository.findByProductoId(productoId, PageRequest.of(pagina - 1, size));
        PginaDto<List<VarianteResumenDto>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(buildResumenDtosBatch(page.getContent()));
        return resultado;
    }

    private List<VarianteResumenDto> buildResumenDtosBatch(List<Variantes> variantes) {
        if (variantes.isEmpty()) return List.of();

        List<Integer> varianteIds = variantes.stream().map(Variantes::getId).toList();
        // Proyeccion (varianteId, imagenId) en vez de entidades VarianteImagen: de la imagen solo
        // se ocupa el id para armar la URL de la miniatura, y traer la entidad obligaba a Hibernate
        // a cargar tambien cada Imagen (@ManyToOne EAGER) con un SELECT por fila del listado.
        List<Object[]> todasImagenes = iVarianteImagenRepository.findIdsPrimeraImagenByVarianteIdIn(varianteIds);

        // La query ordena: principal=true primero, luego por id ASC.
        // putIfAbsent conserva solo el primero (el preferido) por variante.
        Map<Integer, Long> variantePrimeraImagen = new LinkedHashMap<>();
        for (Object[] fila : todasImagenes) {
            variantePrimeraImagen.putIfAbsent((Integer) fila[0], (Long) fila[1]);
        }

        return variantes.stream().map(v -> {
            VarianteResumenDto dto = buildBaseResumenDto(v);
            Long imagenId = variantePrimeraImagen.get(v.getId());
            if (imagenId != null) {
                // Miniatura para listado/busqueda -- el detalle sigue usando la imagen completa.
                dto.setImagenUrl(endpointImagenes + "v1/imagenes/thumbnail/" + imagenId);
            }
            return dto;
        }).toList();
    }

    private VarianteResumenDto buildBaseResumenDto(Variantes v) {
        VarianteResumenDto dto = new VarianteResumenDto();
        dto.setId(v.getId());
        dto.setProductoId(Optional.ofNullable(v.getProducto()).map(Producto::getId).orElse(null));
        dto.setTalla(v.getTalla());
        dto.setDescripcion(v.getDescripcion());
        dto.setColor(v.getColor());
        dto.setPresentacion(v.getPresentacion());
        dto.setStock(v.getStock());
        dto.setMarca(v.getMarca());
        dto.setContenidoNeto(v.getContenidoNeto());
        dto.setFechaCreacion(v.getFechaCreacion());
        // Al que se vende: el descuento si el admin lo activo (R8), si no el normal. Es el precio
        // de lista para todos, clientes incluidos.
        Double precioACobrar = v.precioACobrar();
        dto.setPrecio(precioACobrar != null ? precioACobrar : 0.0);
        // El precio con descuento NO viaja aqui, ni para el admin (R9): quedaba en el navegador
        // de todo el catalogo aunque nadie lo pidiera. Se consulta uno por uno en
        // GET /v1/precios/articulo/{id}/descuento. Esto solo dice lo que el admin necesita para
        // armar la pantalla, sin el monto oculto.
        if (AuthenticationUtils.isAdminContext()) {
            dto.setPrecioPropio(v.tienePrecioPropio());
            dto.setPrecioNormal(v.precioNormal());
            dto.setUsarDescuento(v.cobraConDescuento());
        }
        String codBarras = Optional.ofNullable(v.getProducto())
                .map(Producto::getCodigoBarras)
                .map(CodigoBarra::getCodigoBarras)
                .orElse("");
        dto.setCodigoBarras(codBarras);
        dto.setNombreProducto(Optional.ofNullable(v.getProducto()).map(Producto::getNombre).orElse(""));
        // Estado efectivo: la variante cuenta como habilitada solo si su producto tambien lo esta.
        // Los borradores de carga rapida nacen con producto '0' y variante '1'; sin esto el filtro
        // admin habilitado=false los lista pero el DTO los mostraria como habilitados.
        char habilitadoProducto = Optional.ofNullable(v.getProducto()).map(Producto::getHabilitado).orElse('1');
        dto.setHabilitado(v.getHabilitado() == '1' && habilitadoProducto == '1' ? '1' : '0');
        return dto;
    }

    @Transactional
    public void eliminarImagenesEspecificas(Integer varianteId, List<Long> imagenIds) {
        iVarianteImagenRepository.deleteByVarianteIdAndImagenIdIn(varianteId, imagenIds);

        List<Long> huerfanas = iImagenRepository.findOrphanIds(imagenIds);
        if (!huerfanas.isEmpty()) {
            iImagenRepository.deleteByIdIn(huerfanas);
            try {
                imagenPort.delete(huerfanas);
            } catch (Exception e) {
                log.warn("No se pudieron eliminar imágenes del microservicio ids={}: {}", huerfanas, e.getMessage());
            }
        }
        evictAllCaches();
    }

    @Cacheable(value = "variantesProductoCache", key = "'sin-stock-deshabilitadas:' + #pagina + ':' + #size + ':' + T(com.ventas.key.mis.productos.Utils.AuthenticationUtils).isAdminContext()")
    public PginaDto<List<VarianteResumenDto>> getVariantesSinStockDeshabilitadas(int pagina, int size) {
        Page<Variantes> page = iVarianteRepository.findVariantesSinStockDeshabilitadas(PageRequest.of(pagina - 1, size));
        PginaDto<List<VarianteResumenDto>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(buildResumenDtosBatch(page.getContent()));
        return resultado;
    }

    // Filtros de admin: ve TODO el catálogo de variantes (sin restricción de habilitado
    // salvo el filtro elegido) — a diferencia de las búsquedas públicas que para clientes
    // normales exigen stock>0 + producto habilitado + con imagen.
    @Cacheable(value = "variantesProductoCache",
            // La llave lleva TODOS los filtros: antes no llevaba las fechas y dos rangos distintos
            // devolvian el mismo resultado guardado (2026-10-08).
            key = "'filtro:' + #nombreOCodigo + ':' + #conStock + ':' + #conImagenes + ':' + #habilitado + ':' + #codigoGenerado + ':' + #fechaDesde + ':' + #fechaHasta + ':' + #talla + ':' + #color + ':' + #marca + ':' + #precioMin + ':' + #precioMax + ':' + #pagina + ':' + #size + ':' + T(com.ventas.key.mis.productos.Utils.AuthenticationUtils).isAdminContext()")
    public PginaDto<List<VarianteResumenDto>> filtrarVariantesAdmin(String nombreOCodigo, Boolean conStock,
            Boolean conImagenes, Boolean habilitado, Boolean codigoGenerado, LocalDate fechaDesde,
            LocalDate fechaHasta, String talla, String color, String marca, Double precioMin, Double precioMax,
            int pagina, int size) {
        Pageable pageable = PageRequest.of(pagina - 1, size);
        String texto = (nombreOCodigo != null && !nombreOCodigo.isBlank()) ? nombreOCodigo : null;
        // Mismo criterio que ProductosServiceImpl.filtrarProductosAdmin: dia calendario expandido
        // al rango completo (00:00:00 - 23:59:59.999999999) para que incluya todo ese dia.
        LocalDateTime desde = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime hasta = fechaHasta != null ? fechaHasta.atTime(LocalTime.MAX) : null;
        // Talla / color / marca / precio (los del catalogo) tambien aqui: en Tienda se combinan con los
        // de admin en una sola busqueda (antes cada grupo ignoraba al otro, QA 2026-10-08).
        Page<Variantes> page = iVarianteRepository.buscarVariantesAdmin(texto, conStock, conImagenes, habilitado,
                codigoGenerado, desde, hasta, blankToNull(talla), blankToNull(color), blankToNull(marca),
                precioMin, precioMax, pageable);
        PginaDto<List<VarianteResumenDto>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(buildResumenDtosBatch(page.getContent()));
        return resultado;
    }

    /**
     * Buscador del detalle de pedido: solo articulos que se pueden vender ahora (stock y
     * habilitados, articulo y modelo). <b>Sin cache a proposito</b>: el stock cambia con cada venta
     * y cada edicion de pedido, y una lista vieja vuelve a ofrecer lo que ya se acabo.
     * Sin resultados devuelve lista vacia (no 404).
     */
    public PginaDto<List<VarianteResumenDto>> buscarParaPedido(String termino, int pagina, int size) {
        PginaDto<List<VarianteResumenDto>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        String texto = blankToNull(termino);
        if (texto == null || texto.trim().length() < 3) {
            resultado.setTotalPaginas(0);
            resultado.setTotalRegistros(0);
            resultado.setT(List.of());
            return resultado;
        }
        Page<Variantes> page = iVarianteRepository.buscarVariantesParaPedido(texto.trim(),
                PageRequest.of(Math.max(pagina, 1) - 1, size));
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(buildResumenDtosBatch(page.getContent()));
        return resultado;
    }

    // Catalogo publico con filtros combinables (precio, talla, color, marca + texto libre).
    // Blanks se tratan como "sin filtro" para que el front pueda mandar "" en vez de omitir el
    // parametro sin que eso reduzca los resultados a cero.
    // Toda key de un metodo que devuelve VarianteResumenDto lleva isAdminContext(): el DTO trae
    // precioRebaja/precioNormal solo para el admin. Sin eso, el primero en llegar decide que ven
    // todos: un cliente recibia el descuento oculto que un admin cacheo antes, o al reves.
    @Cacheable(value = "variantesProductoCache",
            key = "'publico-filtro:' + #termino + ':' + #precioMin + ':' + #precioMax + ':' + #talla + ':' + #color + ':' + #marca + ':' + #pagina + ':' + #size + ':' + T(com.ventas.key.mis.productos.Utils.AuthenticationUtils).isAdminContext()")
    public PginaDto<List<VarianteResumenDto>> buscarVariantesPublicoFiltrado(String termino, Double precioMin,
            Double precioMax, String talla, String color, String marca, int pagina, int size) {
        Pageable pageable = PageRequest.of(pagina - 1, size);
        Page<Variantes> page = iVarianteRepository.buscarVariantesPublicoFiltrado(
                blankToNull(termino), precioMin, precioMax, blankToNull(talla), blankToNull(color), blankToNull(marca), pageable);
        PginaDto<List<VarianteResumenDto>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(buildResumenDtosBatch(page.getContent()));
        return resultado;
    }

    private String blankToNull(String texto) {
        return (texto != null && !texto.isBlank()) ? texto : null;
    }

    // Usado por FavoritoServiceImpl para armar el resumen de las variantes marcadas como favoritas
    // sin duplicar la logica de imagenes/precio de buildResumenDtosBatch. Conserva el orden de
    // varianteIds (findAllById NO garantiza orden) porque el llamador ya trae ese orden con
    // significado (mas reciente agregado primero).
    public List<VarianteResumenDto> resumenPorIds(List<Integer> varianteIds) {
        if (varianteIds.isEmpty()) return List.of();
        List<Variantes> variantes = iVarianteRepository.findAllById(varianteIds);
        Map<Integer, Variantes> porId = variantes.stream().collect(Collectors.toMap(Variantes::getId, v -> v));
        List<Variantes> ordenadas = varianteIds.stream().map(porId::get).filter(Objects::nonNull).toList();
        return buildResumenDtosBatch(ordenadas);
    }

    @Cacheable(value = "variantesProductoCache", key = "'filtros-disponibles'")
    public FiltrosDisponiblesDto filtrosDisponiblesPublico() {
        Object[] rango = iVarianteRepository.findRangoPreciosPublico();
        Double precioMin = rango != null && rango[0] != null ? ((Number) rango[0]).doubleValue() : null;
        Double precioMax = rango != null && rango[1] != null ? ((Number) rango[1]).doubleValue() : null;
        return new FiltrosDisponiblesDto(
                iVarianteRepository.findTallasDisponiblesPublico(),
                iVarianteRepository.findColoresDisponiblesPublico(),
                iVarianteRepository.findMarcasDisponiblesPublico(),
                precioMin,
                precioMax);
    }

    @Transactional
    public String habilitarDeshabilitarVariantesLote(List<Integer> ids, boolean habilitar) {
        List<Variantes> variantes = iVarianteRepository.findAllById(ids);
        Set<Integer> idsEncontrados = variantes.stream().map(Variantes::getId).collect(Collectors.toSet());

        // Al deshabilitar, la variante se deja en 0 y con eso su stock vuelve a estar disponible
        // para repartir (el disponible es base - suma de habilitadas; el stock base del producto
        // no se toca). Al habilitar de nuevo NO se le devuelve stock solo -- si quedo en 0 hay que
        // asignarle de nuevo, a proposito: evita que reactivar algo viejo se coma stock que ya se
        // repartio en otras variantes mientras tanto.
        if (!habilitar) {
            variantes.forEach(v -> v.setStock(0));
        }
        List<String> ajustados = habilitar ? ajustarStockAlHabilitar(variantes) : List.of();
        variantes.forEach(v -> v.setHabilitado(habilitar ? '1' : '0'));
        iVarianteRepository.saveAll(variantes);
        iVarianteRepository.flush();
        entityManager.clear();

        // Relectura directa (sesion de Hibernate limpiada) para confirmar que el UPDATE
        // realmente llego a la BD dentro de esta misma transaccion, sin depender de la
        // cache de primer nivel ni de herramientas externas para verificar.
        Map<Integer, Character> valoresTrasGuardar = iVarianteRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Variantes::getId, Variantes::getHabilitado));

        if (log.isDebugEnabled()) {
            String diagnostico = ids.stream()
                    .map(id -> String.format(
                            "{\"id\":%d,\"encontradoEnBD\":%b,\"habilitadoTrasGuardar\":\"%s\"}",
                            id,
                            idsEncontrados.contains(id),
                            valoresTrasGuardar.getOrDefault(id, '?')))
                    .collect(Collectors.joining(",", "{\"idsEnviados\":" + ids + ",\"resultado\":[", "]}"));
            log.debug("Diagnostico habilitar-lote variantes: {}", diagnostico);
        }

        evictAllCaches();

        if (!habilitar) return "Artículos deshabilitados correctamente.";
        return ajustados.isEmpty() ? "Artículos habilitados correctamente."
                : "Artículos habilitados. Se ajustó el stock a lo que quedaba libre del modelo: "
                        + String.join("; ", ajustados) + ".";
    }

    /**
     * Un articulo dado de baja antes de que la baja lo dejara en 0 todavia guarda su stock viejo.
     * Al habilitarlo solo puede llevarse lo que el modelo tenga libre (stock del modelo menos lo
     * que ya tienen sus articulos habilitados): si no, el modelo queda descuadrado. Devuelve los
     * ajustes hechos, en palabras para el dueño.
     */
    private List<String> ajustarStockAlHabilitar(List<Variantes> aHabilitar) {
        List<String> ajustes = new ArrayList<>();
        Map<Integer, List<Variantes>> porModelo = aHabilitar.stream()
                .filter(v -> v.getHabilitado() != '1' && v.getStock() > 0)
                .collect(Collectors.groupingBy(v -> v.getProducto().getId()));
        for (Map.Entry<Integer, List<Variantes>> e : porModelo.entrySet()) {
            Producto modelo = e.getValue().get(0).getProducto();
            int repartido = iVarianteRepository.findByProductoIdAndHabilitado(modelo.getId(), '1').stream()
                    .mapToInt(Variantes::getStock).sum();
            int libre = Math.max((modelo.getStock() != null ? modelo.getStock() : 0) - repartido, 0);
            for (Variantes v : e.getValue()) {
                int queda = Math.min(v.getStock(), libre);
                if (queda < v.getStock()) {
                    ajustes.add(String.format("%s de %d a %d", nombreArticulo(modelo, v), v.getStock(), queda));
                    v.setStock(queda);
                }
                libre -= queda;
            }
        }
        return ajustes;
    }

    private static String nombreArticulo(Producto modelo, Variantes v) {
        StringBuilder sb = new StringBuilder(modelo.getNombre() != null ? modelo.getNombre() : "Artículo " + v.getId());
        if (v.getTalla() != null && !v.getTalla().isBlank()) sb.append(" ").append(v.getTalla());
        if (v.getColor() != null && !v.getColor().isBlank()) sb.append(" ").append(v.getColor());
        return sb.toString();
    }

    public DiagnosticoImagenVarianteDto diagnosticarImagenesVariante(Integer varianteId) {
        DiagnosticoImagenVarianteDto dto = new DiagnosticoImagenVarianteDto();
        dto.setVarianteId(varianteId);

        List<VarianteImagen> relaciones = iVarianteImagenRepository.findByVarianteId(varianteId);
        List<ImagenDiagnosticoItem> itemsLocalDB = relaciones.stream()
                .map(vi -> new ImagenDiagnosticoItem(
                        vi.getImagen().getId(),
                        vi.getImagen().getNombreImagen(),
                        vi.getImagen().getExtension(),
                        vi.getImagen().getBase64()))
                .toList();
        dto.setImagenesLocalDB(itemsLocalDB);
        dto.setTotalImagenesLocalDB(itemsLocalDB.size());

        if (relaciones.isEmpty()) {
            dto.setIdsConDatosEnMicroservicio(List.of());
            dto.setIdsSinDatosEnMicroservicio(List.of());
            dto.setConsistente(true);
            return dto;
        }

        List<Long> ids = relaciones.stream().map(vi -> vi.getImagen().getId()).toList();
        List<ImagenDto> imagenesExternas;
        try {
            imagenesExternas = imageneClienteDisco.getAll(ids);
        } catch (Exception e) {
            log.warn("Error al consultar microservicio para diagnóstico de variante {}: {}", varianteId, e.getMessage());

            imagenesExternas = List.of();
        }

        Set<Long> idsConDatos = imagenesExternas.stream()
                .filter(img -> img.getImagen() != null)
                .map(ImagenDto::getId)
                .collect(Collectors.toSet());

        dto.setIdsConDatosEnMicroservicio(new ArrayList<>(idsConDatos));
        dto.setIdsSinDatosEnMicroservicio(ids.stream().filter(id -> !idsConDatos.contains(id)).toList());
        dto.setConsistente(dto.getIdsSinDatosEnMicroservicio().isEmpty());

        return dto;
    }

    /**
     * Baja de una variante. Es borrado logico a proposito: hay 13 tablas que apuntan a
     * variante (detalle_pedido, detalle_venta_variante, resena, favorito, promocion_detalle,
     * configurar_rifa_variante...), asi que un DELETE real dejaria el historial de ventas y
     * pedidos apuntando a una fila que ya no existe. Mismo criterio que
     * ProductosServiceImpl.deleteByIdProducto(), que tambien deja el producto en habilitado=0.
     *
     * La variante se deja en stock 0, y con eso su stock vuelve a estar disponible para repartir
     * en otras variantes -- el stock base del producto NO se toca: nunca bajo al asignarlo a esta
     * variante, asi que tampoco tiene que subir al soltarlo. Lo disponible se calcula siempre como
     * base menos la suma de las variantes HABILITADAS (ver validarStockContraProducto).
     *
     * Ejemplo: base 10 con una variante en 2 -> disponible 8. Al dar de baja esa variante el base
     * sigue en 10 y el disponible vuelve a 10. Si despues se vuelve a habilitar, entra con 0: hay
     * que asignarle stock de nuevo, no lo recupera solo.
     *
     * Las imagenes si se borran de verdad (y del micro si quedan huerfanas): con la variante
     * deshabilitada ya no se muestran en ningun lado y solo ocupan disco.
     */
    @Transactional
    public void deleteByIdVariante(Integer id) {
        Variantes variante = iVarianteRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("No existe el artículo con el id: " + id));

        eliminarImagenesDeVariantes(List.of(id));

        variante.setHabilitado('0');
        variante.setStock(0);
        iVarianteRepository.save(variante);
        log.info("Variante id={} dada de baja (habilitado=0, stock liberado al disponible del producto) "
                + "y sus imagenes eliminadas", id);
        evictAllCaches();
    }

    @Transactional
    public void eliminarImagenesDeVariantes(List<Integer> varianteIds) {
        List<Long> imagenIds = iVarianteImagenRepository.findImagenIdsByVarianteIdIn(varianteIds);
        iVarianteImagenRepository.deleteByVarianteIdIn(varianteIds);

        if (!imagenIds.isEmpty()) {
            List<Long> huerfanas = iImagenRepository.findOrphanIds(imagenIds);
            if (!huerfanas.isEmpty()) {
                iImagenRepository.deleteByIdIn(huerfanas);
                try {
                    imagenPort.delete(huerfanas);
                } catch (Exception e) {
                    log.warn("No se pudieron eliminar imágenes del microservicio ids={}: {}", huerfanas, e.getMessage());
                }
            }
        }
        evictAllCaches();
    }

}
