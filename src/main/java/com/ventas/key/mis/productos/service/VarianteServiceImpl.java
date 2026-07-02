package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.dto.variantes.RequestVarianteDto;
import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.entity.PalabraClave;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.VarianteImagen;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenPort;
import com.ventas.key.mis.productos.hexagonal.infraestructura.ImageneClienteDisco;
import com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto;
import com.ventas.key.mis.productos.models.*;
import com.ventas.key.mis.productos.models.variantes.VarianteDto;
import com.ventas.key.mis.productos.entity.ProductoImagen;
import com.ventas.key.mis.productos.repository.IImagenRepository;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IProductoImagenRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.service.api.IVarianteService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.multipart.MultipartFile;

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
    }

    public PginaDto<List<VarianteResumenDto>> buscarVariantes(String termino, int page, int size) {
        if (termino == null || termino.isBlank()) {
            return findAllResumen(page, size);
        }

        PginaDto<List<VarianteResumenDto>> porCodigo = buscarPorCodigoBarrasPaginadoResumen(termino, page, size);
        if (!porCodigo.getT().isEmpty()) return porCodigo;

        PginaDto<List<VarianteResumenDto>> porPalabraClave = buscarPorPalabraClavePaginadoResumen(termino, page, size);
        if (!porPalabraClave.getT().isEmpty()) return porPalabraClave;

        PginaDto<List<VarianteResumenDto>> porNombre = buscarPorNombrePaginadoResumen(termino, page, size);
        if (!porNombre.getT().isEmpty()) return porNombre;

        throw new ExceptionDataNotFound("No se encontraron variantes con la búsqueda: \"" + termino + "\"");
    }
    @Cacheable(value = "variantesProductoCache", key = "#productoId")
    public List<VarianteDto> buscarPorProducto(Integer productoId) {
        return iVarianteRepository.findByProductoId(productoId).stream().map(v -> {
            VarianteDto dto = new VarianteDto();
            dto.setId(v.getId());
            dto.setTalla(v.getTalla());
            dto.setDescripcion(v.getDescripcion());
            dto.setColor(v.getColor());
            dto.setPresentacion(v.getPresentacion());
            dto.setStock(v.getStock());
            dto.setMarca(v.getMarca());
            dto.setContenidoNeto(v.getContenidoNeto());
            dto.setPrecio(v.getProducto().getPrecioVenta() != null ? v.getProducto().getPrecioVenta() : 0.0);
            CodigoBarra cb = v.getProducto().getCodigoBarras();
            dto.setCodigoBarras(cb != null ? cb.getCodigoBarras() : null);
            dto.setPalabraClave(v.getPalabraClave() != null
                    ? new com.ventas.key.mis.productos.models.PalabraClaveResumenDto(v.getPalabraClave().getId(), v.getPalabraClave().getNombre())
                    : null);
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

    @Cacheable(value = "variantesNombreCache", key = "#nombre + ':' + #pagina + ':' + #size")
    public PginaDto<List<Variantes>> buscarPorNombrePaginado(String nombre, int pagina, int size) {
        Page<Variantes> page;
        if(AuthenticationUtils.isAdminContext()){
            page = iVarianteRepository.findByProductoNombreContainingIgnoreCase(nombre, PageRequest.of(pagina - 1, size));
        }else{
            page = iVarianteRepository.findByStockGreaterThanAndProducto_HabilitadoAndProducto_NombreContainingIgnoreCase(0, '1',nombre, PageRequest.of(pagina - 1, size));

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

    @Cacheable(value = "variantesCodigoBarrasCache", key = "#codigoBarras + ':' + #pagina + ':' + #size")
    public PginaDto<List<Variantes>> buscarPorCodigoBarrasPaginado(String codigoBarras, int pagina, int size) {
        boolean isAdmin = AuthenticationUtils.isAdminContext();
        Page<Variantes> page = null;
        if(isAdmin){
            page = iVarianteRepository.findByProductoCodigoBarrasCodigoBarras(codigoBarras, PageRequest.of(pagina - 1, size));
        }else{
            page = iVarianteRepository.findByStockGreaterThanAndProducto_HabilitadoAndProducto_CodigoBarras_CodigoBarrasContaining(0, '1',codigoBarras, PageRequest.of(pagina - 1, size));
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
                .orElseThrow(() -> new ExceptionDataNotFound("Variante no encontrada: " + id));
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

        int stockEnVariantes = obtenerVariantesPorProducto(requestVarianteDto.getProductoId())
                .stream().mapToInt(Variantes::getStock).sum();

        int stockDisponible = producto.getStock() - stockEnVariantes;
        if (stockDisponible < requestVarianteDto.getCantidadVariantes()) {
            throw new ExceptionDataNotFound(
                    String.format("Stock insuficiente para crear %d variantes del producto %d. Stock disponible: %d",
                            requestVarianteDto.getCantidadVariantes(), producto.getId(), stockDisponible));
        }

        List<Long> imageIds = List.of();
        if (requestVarianteDto.isImagenParaTodas() && imagenes != null && imagenes.length > 0) {
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
        }

        final List<Long> finalImageIds = imageIds;
        for (int i = 0; i < requestVarianteDto.getCantidadVariantes(); i++) {
            Variantes variante = new Variantes();
            variante.setProducto(producto);
            variante.setStock(1);
            Variantes savedVariante = save(variante);
            if (!finalImageIds.isEmpty()) {
                vincularImagenes(savedVariante, finalImageIds);
            }
        }

        evictAllCaches();
        return true;
    }

    private List<Long> subirImagenesMultipart(MultipartFile[] imagenes) {
        LinkedMultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        for (MultipartFile file : imagenes) {
            try {
                byte[] bytes = file.getBytes();
                String nombre = file.getOriginalFilename() != null ? file.getOriginalFilename() : "imagen";
                ByteArrayResource recurso = new ByteArrayResource(bytes) {
                    @Override
                    public String getFilename() { return nombre; }
                };
                formData.add("files", recurso);
            } catch (Exception e) {
                throw new ExceptionDataNotFound("Error al procesar imagen: " + e.getMessage());
            }
        }
        return imageneClienteDisco.save(formData).stream().map(ImagenDto::getId).toList();
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

    @Cacheable(value = "variantesImagenesCache", key = "'v2:' + #varianteId")
    public List<ImagenUpdateDto> getImagenesPorVarianteV2(Integer varianteId) {
        List<VarianteImagen> relaciones = filtrarRelacionesConImagen(
                iVarianteImagenRepository.findByVarianteId(varianteId), varianteId);
        if (relaciones.isEmpty()) return List.of();
        List<Long> ids = relaciones.stream().map(vi -> vi.getImagen().getId()).toList();
        List<Long> existentesList;
        try {
            existentesList = imageneClienteDisco.verificarExistentes(ids);
        } catch (Exception e) {
            log.warn("Error verificando existencia en micro para varianteId={}: {}", varianteId, e.getMessage());
            existentesList = List.of();
        }
        // Si la verificación devuelve vacío (micro no disponible o archivo perdido),
        // usar BD local como fallback para que el detalle sea consistente con el listado.
        if (existentesList.isEmpty()) {
            log.warn("verificarExistentes vacío para varianteId={}, usando BD local como fallback", varianteId);
            return buildImagenUpdateDtos(relaciones);
        }
        Set<Long> existentes = new HashSet<>(existentesList);
        return buildImagenUpdateDtos(relaciones.stream()
                .filter(vi -> existentes.contains(vi.getImagen().getId()))
                .toList());
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
        if (relaciones.isEmpty()) return List.of();
        return relaciones.stream().map(vi -> {
            var img = vi.getImagen();
            ImagenUpdateDto dto = new ImagenUpdateDto(img.getId(), (byte[]) null, img.getExtension(), img.getNombreImagen());
            dto.setUrlImagen(endpointImagenes + "v1/imagenes/file/" + img.getId());
            dto.setPrincipal(vi.getPrincipal());
            return dto;
        }).toList();
    }

    @Transactional
    public void marcarImagenPrincipalVariante(Integer varianteImagenId) {
        VarianteImagen target = iVarianteImagenRepository.findById(varianteImagenId)
                .orElseThrow(() -> new ExceptionDataNotFound("Relación variante-imagen no encontrada: " + varianteImagenId));
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
        validarStockContraProducto(detalles);
        List<Long> imageIds = subirImagenes(detalles);

        List<Variantes> resultado = new ArrayList<>();
        for (VarianteDetalle detalle : detalles) {
            if (detalle.getId() != null) {
                ajustarStock(detalle);
            }

            Variantes saved = save(buildVariante(detalle));
            resultado.add(saved);

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

    private List<Long> subirImagenes(List<VarianteDetalle> detalles) {
        List<ImagenDTO> todas = detalles.stream()
                .filter(d -> d.getListImagenes() != null && !d.getListImagenes().isEmpty())
                .flatMap(d -> d.getListImagenes().stream())
                .toList();
        if (todas.isEmpty()) return List.of();

        LinkedMultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        for (ImagenDTO dto : todas) {
            byte[] bytes = dto.getBase64();
            String nombre = dto.getNombreImagen();
            ByteArrayResource recurso = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() { return nombre; }
            };
            formData.add("files", recurso);
        }
        return imageneClienteDisco.save(formData).stream().map(ImagenDto::getId).toList();
    }

    private void vincularImagenes(Variantes variante, List<Long> imageIds) {
        List<VarianteImagen> relaciones = imageIds.stream().map(imgId -> {
            VarianteImagen vi = new VarianteImagen();
            vi.setVariante(variante);
            vi.setImagen(iImagenRepository.getReferenceById(imgId));
            return vi;
        }).toList();
        iVarianteImagenRepository.saveAll(relaciones);
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

            int stockYaAsignado = iVarianteRepository.findByProductoId(productoId).stream()
                    .filter(v -> !idsActualizando.contains(v.getId()))
                    .mapToInt(Variantes::getStock)
                    .sum();

            int stockSolicitado = variantesRequest.stream().mapToInt(VarianteDetalle::getStock).sum();
            int stockDisponible = producto.getStock() - stockYaAsignado;

            if (stockSolicitado > stockDisponible) {
                throw new ExceptionDataNotFound(
                        String.format("Stock insuficiente para el producto '%s' (id=%d). Disponible: %d, Solicitado: %d",
                                producto.getNombre(), productoId, stockDisponible, stockSolicitado));
            }
        }
    }

    private void ajustarStock(VarianteDetalle detalle) throws ExceptionDataNotFound {
        Variantes actual = iVarianteRepository.findById(detalle.getId())
                .orElseThrow(() -> new ExceptionDataNotFound("Variante no encontrada: " + detalle.getId()));
        int diff = detalle.getStock() - actual.getStock();
        if (diff != 0) {
            Producto producto = iProductosRepository.findById(detalle.getProductoId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Producto no encontrado: " + detalle.getProductoId()));
            producto.setStock(producto.getStock() + diff);
            iProductosRepository.save(producto);
        }
    }

    private Variantes buildVariante(VarianteDetalle detalle) {
        Variantes v = new Variantes();
        if (detalle.getId() != null) v.setId(detalle.getId());
        v.setProducto(iProductosRepository.getReferenceById(detalle.getProductoId()));
        v.setTalla(detalle.getTalla());
        v.setColor(detalle.getColor());
        v.setMarca(detalle.getMarca());
        v.setStock(detalle.getStock());
        v.setDescripcion(detalle.getDescripcion());
        v.setPresentacion(detalle.getPresentacion());
        v.setContenidoNeto(detalle.getContenidoNeto());
        if (detalle.getPalabraClaveId() != null) {
            v.setPalabraClave(iPalabraClaveRepository.getReferenceById(detalle.getPalabraClaveId()));
        }
        return v;
    }

    @Cacheable(value = "variantesNombreCache", key = "'resumen:' + #nombre + ':' + #pagina + ':' + #size")
    public PginaDto<List<VarianteResumenDto>> buscarPorNombrePaginadoResumen(String nombre, int pagina, int size) {
        return toResumenPagina(buscarPorNombrePaginado(nombre, pagina, size));
    }

    @Cacheable(value = "variantesCodigoBarrasCache", key = "'resumen:' + #codigoBarras + ':' + #pagina + ':' + #size")
    public PginaDto<List<VarianteResumenDto>> buscarPorCodigoBarrasPaginadoResumen(String codigoBarras, int pagina, int size) {
        return toResumenPagina(buscarPorCodigoBarrasPaginado(codigoBarras, pagina, size));
    }

    @Cacheable(value = "variantesPalabraClaveCache",
            key = "'resumen:' + #nombre + ':' + #pagina + ':' + #size + ':' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getAuthorities()")
    public PginaDto<List<VarianteResumenDto>> buscarPorPalabraClavePaginadoResumen(String nombre, int pagina, int size) {
        boolean isAdmin = AuthenticationUtils.isAdminContext();
        Page<Variantes> page = isAdmin
                ? iVarianteRepository.findByPalabraClave_NombreIgnoreCase(nombre, PageRequest.of(pagina - 1, size))
                : iVarianteRepository.findByStockGreaterThanAndProducto_HabilitadoAndPalabraClave_NombreIgnoreCase(0, '1', nombre, PageRequest.of(pagina - 1, size));
        PginaDto<List<Variantes>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(page.getContent());
        return toResumenPagina(resultado);
    }

    @Cacheable(value = "variantesProductoCache", key = "'resumen:all:' + #pagina + ':' + #size")
    public PginaDto<List<VarianteResumenDto>> findAllResumen(int pagina, int size) {
        return toResumenPagina(findAllNew(pagina, size));
    }

    @Override
    public PginaDto<List<Variantes>> findAllNew(int pagina, int size){
        PginaDto<List<Variantes>> pginaDto = new PginaDto<>();
        Pageable pageable = PageRequest.of(pagina - 1, size);
        Page<Variantes> dataPaginacion;
        if(AuthenticationUtils.isAdminContext()){
            dataPaginacion = this.repoGenerico.findAll(pageable);
        }else{
            dataPaginacion = this.iVarianteRepository.findByStockGreaterThanAndProducto_Habilitado(0, '1', pageable);
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

@Cacheable(value = "variantesProductoCache", key = "'resumen:' + #productoId + ':' + #pagina + ':' + #size")
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
        List<VarianteImagen> todasImagenes = iVarianteImagenRepository.findByVarianteIdIn(varianteIds);

        // La query ordena: principal=true primero, luego por id ASC.
        // putIfAbsent conserva solo el primero (el preferido) por variante.
        Map<Integer, Long> variantePrimeraImagen = new LinkedHashMap<>();
        for (VarianteImagen vi : todasImagenes) {
            variantePrimeraImagen.putIfAbsent(vi.getVariante().getId(), vi.getImagen().getId());
        }

        return variantes.stream().map(v -> {
            VarianteResumenDto dto = buildBaseResumenDto(v);
            Long imagenId = variantePrimeraImagen.get(v.getId());
            if (imagenId != null) {
                dto.setImagenUrl(endpointImagenes + "v1/imagenes/file/" + imagenId);
            }
            return dto;
        }).toList();
    }

    private VarianteResumenDto buildBaseResumenDto(Variantes v) {
        VarianteResumenDto dto = new VarianteResumenDto();
        dto.setId(v.getId());
        dto.setTalla(v.getTalla());
        dto.setDescripcion(v.getDescripcion());
        dto.setColor(v.getColor());
        dto.setPresentacion(v.getPresentacion());
        dto.setStock(v.getStock());
        dto.setMarca(v.getMarca());
        dto.setContenidoNeto(v.getContenidoNeto());
        dto.setPrecio(v.getProducto().getPrecioVenta());
        String codBarras = Optional.ofNullable(v.getProducto())
                .map(Producto::getCodigoBarras)
                .map(CodigoBarra::getCodigoBarras)
                .orElse("");
        dto.setCodigoBarras(codBarras);
        dto.setNombreProducto(Optional.ofNullable(v.getProducto()).map(Producto::getNombre).orElse(""));
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

    @Cacheable(value = "variantesProductoCache", key = "'sin-stock-deshabilitadas:' + #pagina + ':' + #size")
    public PginaDto<List<VarianteResumenDto>> getVariantesSinStockDeshabilitadas(int pagina, int size) {
        Page<Variantes> page = iVarianteRepository.findVariantesSinStockDeshabilitadas(PageRequest.of(pagina - 1, size));
        PginaDto<List<VarianteResumenDto>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(buildResumenDtosBatch(page.getContent()));
        return resultado;
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
