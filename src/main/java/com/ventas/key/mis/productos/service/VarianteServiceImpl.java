package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.VarianteImagen;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.hexagonal.infraestructura.ImageneClienteAWS;
import com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto;
import com.ventas.key.mis.productos.models.*;
import com.ventas.key.mis.productos.models.variantes.VarianteDto;
import com.ventas.key.mis.productos.repository.IImagenRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.service.api.IVarianteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VarianteServiceImpl extends CrudAbstractServiceImpl<Variantes, List<Variantes>, Optional<Variantes>, Integer, PginaDto<List<Variantes>>>
        implements IVarianteService {

    private final IVarianteRepository iVarianteRepository;
    private final IVarianteImagenRepository iVarianteImagenRepository;
    private final IProductosRepository iProductosRepository;
    private final ImageneClienteAWS imageneClienteAWS;
    private final IImagenRepository iImagenRepository;

    public VarianteServiceImpl(IVarianteRepository iVarianteRepository,
                               IVarianteImagenRepository iVarianteImagenRepository,
                               IProductosRepository iProductosRepository,
                               ImageneClienteAWS imageneClienteAWS,
                               IImagenRepository iImagenRepository,
                               ErrorGenerico error) {
        super(iVarianteRepository, error);
        this.iVarianteRepository = iVarianteRepository;
        this.iVarianteImagenRepository = iVarianteImagenRepository;
        this.iProductosRepository = iProductosRepository;
        this.imageneClienteAWS = imageneClienteAWS;
        this.iImagenRepository = iImagenRepository;
    }

    public PginaDto<List<VarianteResumenDto>> buscarVariantes(String nombreOrCodigBarras, int page, int size){

        PginaDto<List<VarianteResumenDto>> buscarCodigoBarras = buscarPorCodigoBarrasPaginadoResumen(nombreOrCodigBarras, page, size);
        if (!buscarCodigoBarras.getT().isEmpty()){
            return buscarCodigoBarras;
        }
        PginaDto<List<VarianteResumenDto>> buscarPorNombre = buscarPorNombrePaginadoResumen(nombreOrCodigBarras, page, size);
        if(!buscarPorNombre.getT().isEmpty()){
            return buscarPorNombre;
        }
        return findAllResumen(page, size);
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
        Page<Variantes> page = iVarianteRepository.findByProductoNombreContainingIgnoreCase(nombre, PageRequest.of(pagina - 1, size));
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
        Page<Variantes> page = iVarianteRepository.findByProductoCodigoBarrasCodigoBarras(codigoBarras, PageRequest.of(pagina - 1, size));
        PginaDto<List<Variantes>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(page.getContent());
        return resultado;
    }

    @Cacheable(value = "variantesImagenesCache", key = "#varianteId")
    public List<ImagenUpdateDto> getImagenesPorVariante(Integer varianteId) {
        List<VarianteImagen> relaciones = iVarianteImagenRepository.findByVarianteId(varianteId);
        return buildImagenUpdateDtos(relaciones);
    }

    @Cacheable(value = "variantesImagenesCache", key = "#varianteId + ':' + #pagina + ':' + #size")
    public PginaDto<List<ImagenUpdateDto>> getImagenesPorVariantePaginado(Integer varianteId, int pagina, int size) {
        Page<VarianteImagen> page = iVarianteImagenRepository.findByVarianteId(varianteId, PageRequest.of(pagina - 1, size));
        PginaDto<List<ImagenUpdateDto>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(buildImagenUpdateDtos(page.getContent()));
        return resultado;
    }

    private List<ImagenUpdateDto> buildImagenUpdateDtos(List<VarianteImagen> relaciones) {
        if (relaciones.isEmpty()) return List.of();
        List<Long> ids = relaciones.stream().map(vi -> vi.getImagen().getId()).toList();
        List<com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto> imagenes;
        try {
            imagenes = ids.stream().map(imageneClienteAWS::getOne).toList();
        } catch (Exception e) {
            log.warn("No se pudieron obtener imágenes del microservicio: {}", e.getMessage());
            imagenes = List.of();
        }
        var mapaBytes = imagenes.stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto::getId,
                        com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto::getImagen,
                        (a, b) -> a));
        return relaciones.stream().map(vi -> {
            var img = vi.getImagen();
            byte[] bytes = mapaBytes.get(img.getId());
            return new ImagenUpdateDto(img.getId(), bytes, img.getExtension(), img.getNombreImagen());
        }).toList();
    }

    @CacheEvict(value = {"variantesProductoCache", "variantesNombreCache", "variantesCodigoBarrasCache", "variantesImagenesCache"}, allEntries = true)
    @Transactional
    public List<Variantes> guardarConImagenes(List<VarianteDetalle> detalles) throws ExceptionDataNotFound {
        // 1. Recolectar imágenes de todos los detalles y subir una sola vez
        List<Long> imageIds = subirImagenes(detalles);

        // 2. Guardar cada variante y vincular las mismas imágenes a todas
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
            String nombre = dto.getNombreImagen();
            ByteArrayResource recurso = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() { return nombre; }
            };
            formData.add("files", recurso);
        }
        return imageneClienteAWS.save(formData).stream().map(ImagenDto::getId).toList();
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

    @Cacheable(value = "variantesProductoCache", key = "'resumen:all:' + #pagina + ':' + #size")
    public PginaDto<List<VarianteResumenDto>> findAllResumen(int pagina, int size) {
        return toResumenPagina(findAllNew(pagina, size));
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

        // 1. Una sola query DB para todas las imágenes de todas las variantes
        List<Integer> varianteIds = variantes.stream().map(Variantes::getId).toList();
        List<VarianteImagen> todasImagenes = iVarianteImagenRepository.findByVarianteIdIn(varianteIds);

        // primera imagen por variante
        Map<Integer, Long> varianteToImagenId = new HashMap<>();
        for (VarianteImagen vi : todasImagenes) {
            varianteToImagenId.putIfAbsent(vi.getVariante().getId(), vi.getImagen().getId());
        }

        // 2. Una sola llamada HTTP al microservicio con todos los IDs
        Map<Long, byte[]> imagenBytes = new HashMap<>();
        if (!varianteToImagenId.isEmpty()) {
            try {
                List<ImagenDto> imagenes = imageneClienteAWS.getAll(new ArrayList<>(varianteToImagenId.values()));
                imagenes.forEach(img -> {
                    if (img.getImagen() != null) imagenBytes.put(img.getId(), img.getImagen());
                });
            } catch (Exception e) {
                log.warn("No se pudieron obtener imágenes en batch: {}", e.getMessage());
            }
        }

        // 3. Construir DTOs con las imágenes ya cargadas
        return variantes.stream().map(v -> {
            VarianteResumenDto dto = buildBaseResumenDto(v);
            Long imagenId = varianteToImagenId.get(v.getId());
            if (imagenId != null) {
                byte[] bytes = imagenBytes.get(imagenId);
                if (bytes != null) dto.setImagenBase64(Base64.getEncoder().encodeToString(bytes));
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
        return dto;
    }

}
