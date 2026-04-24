package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.entity.productoVariantes.VarianteImagen;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.hexagonal.infraestructura.ImageneClienteAWS;
import com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto;
import com.ventas.key.mis.productos.models.ImagenDTO;
import com.ventas.key.mis.productos.models.ImagenUpdateDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.VarianteDetalle;
import com.ventas.key.mis.productos.models.VarianteResumenDto;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.service.api.IImagenService;
import com.ventas.key.mis.productos.service.api.IVarianteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class VarianteServiceImpl extends CrudAbstractServiceImpl<Variantes, List<Variantes>, Optional<Variantes>, Integer, PginaDto<List<Variantes>>>
        implements IVarianteService {

    private final IVarianteRepository iVarianteRepository;
    private final IVarianteImagenRepository iVarianteImagenRepository;
    private final IProductosRepository iProductosRepository;
    private final ImageneClienteAWS imageneClienteAWS;

    public VarianteServiceImpl(IVarianteRepository iVarianteRepository,
                               IVarianteImagenRepository iVarianteImagenRepository,
                               IProductosRepository iProductosRepository,
                               ImageneClienteAWS imageneClienteAWS,
                               ErrorGenerico error) {
        super(iVarianteRepository, error);
        this.iVarianteRepository = iVarianteRepository;
        this.iVarianteImagenRepository = iVarianteImagenRepository;
        this.iProductosRepository = iProductosRepository;
        this.imageneClienteAWS = imageneClienteAWS;
    }

    public List<Variantes> buscarPorProducto(Integer productoId) {
        return iVarianteRepository.findByProductoId(productoId);
    }

    public PginaDto<List<Variantes>> buscarPorProductoPaginado(Integer productoId, int pagina, int size) {
        Page<Variantes> page = iVarianteRepository.findByProductoId(productoId, PageRequest.of(pagina - 1, size));
        PginaDto<List<Variantes>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(page.getContent());
        return resultado;
    }

    public List<Variantes> buscarPorNombre(String nombre) {
        return iVarianteRepository.findByProductoNombreContainingIgnoreCase(nombre);
    }

    public PginaDto<List<Variantes>> buscarPorNombrePaginado(String nombre, int pagina, int size) {
        Page<Variantes> page = iVarianteRepository.findByProductoNombreContainingIgnoreCase(nombre, PageRequest.of(pagina - 1, size));
        PginaDto<List<Variantes>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(page.getContent());
        return resultado;
    }

    public List<Variantes> buscarPorCodigoBarras(String codigoBarras) {
        return iVarianteRepository.findByProductoCodigoBarrasCodigoBarras(codigoBarras);
    }

    public PginaDto<List<Variantes>> buscarPorCodigoBarrasPaginado(String codigoBarras, int pagina, int size) {
        Page<Variantes> page = iVarianteRepository.findByProductoCodigoBarrasCodigoBarras(codigoBarras, PageRequest.of(pagina - 1, size));
        PginaDto<List<Variantes>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(page.getContent());
        return resultado;
    }

    public List<ImagenUpdateDto> getImagenesPorVariante(Integer varianteId) {
        return iVarianteImagenRepository.getImagenByVarianteId(varianteId);
    }

    @Transactional
    public Variantes guardarConImagenes(VarianteDetalle detalle) throws Exception {
        if (detalle.getId() != null) {
            Variantes actual = iVarianteRepository.findById(detalle.getId())
                    .orElseThrow(() -> new RuntimeException("Variante no encontrada: " + detalle.getId()));
            int diff = detalle.getStock() - actual.getStock();
            if (diff != 0) {
                com.ventas.key.mis.productos.entity.Producto producto =
                        iProductosRepository.findById(detalle.getProductoId())
                                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalle.getProductoId()));
                producto.setStock(producto.getStock() + diff);
                iProductosRepository.save(producto);
            }
        }

        Variantes variante = buildVariante(detalle);
        Variantes saved = save(variante);

        if (detalle.getListImagenes() != null && !detalle.getListImagenes().isEmpty()) {
            LinkedMultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            for (ImagenDTO dto : detalle.getListImagenes()) {
                byte[] bytes = dto.getBase64();
                String nombre = dto.getNombreImagen();
                ByteArrayResource recurso = new ByteArrayResource(bytes) {
                    @Override
                    public String getFilename() { return nombre; }
                };
                formData.add("files", recurso);
            }

            // micro_imagenes escribe en disco Y guarda en imagenes_copy, devuelve los IDs
            List<ImagenDto> savedImagenes = imageneClienteAWS.save(formData);

            List<VarianteImagen> relaciones = savedImagenes.stream().map(dto -> {
                VarianteImagen vi = new VarianteImagen();
                vi.setVariante(saved);
                Imagen img = new Imagen();
                img.setId(dto.getId());
                vi.setImagen(img);
                return vi;
            }).toList();
            iVarianteImagenRepository.saveAll(relaciones);
        }
        return saved;
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

    public PginaDto<List<VarianteResumenDto>> buscarPorNombrePaginadoResumen(String nombre, int pagina, int size) {
        return toResumenPagina(buscarPorNombrePaginado(nombre, pagina, size));
    }

    public PginaDto<List<VarianteResumenDto>> buscarPorCodigoBarrasPaginadoResumen(String codigoBarras, int pagina, int size) {
        return toResumenPagina(buscarPorCodigoBarrasPaginado(codigoBarras, pagina, size));
    }

    public PginaDto<List<VarianteResumenDto>> findAllResumen(int pagina, int size) {
        return toResumenPagina(findAllNew(pagina, size));
    }

    private PginaDto<List<VarianteResumenDto>> toResumenPagina(PginaDto<List<Variantes>> origen) {
        PginaDto<List<VarianteResumenDto>> resultado = new PginaDto<>();
        resultado.setPagina(origen.getPagina());
        resultado.setTotalPaginas(origen.getTotalPaginas());
        resultado.setTotalRegistros(origen.getTotalRegistros());
        resultado.setT(origen.getT().stream().map(this::toResumenDto).toList());
        return resultado;
    }

    private VarianteResumenDto toResumenSinImagen(Variantes v) {
        VarianteResumenDto dto = new VarianteResumenDto();
        dto.setId(v.getId());
        dto.setTalla(v.getTalla());
        dto.setDescripcion(v.getDescripcion());
        dto.setColor(v.getColor());
        dto.setPresentacion(v.getPresentacion());
        dto.setStock(v.getStock());
        dto.setMarca(v.getMarca());
        dto.setContenidoNeto(v.getContenidoNeto());
        return dto;
    }

    public PginaDto<List<VarianteResumenDto>> buscarPorProductoPaginadoResumen(Integer productoId, int pagina, int size) {
        Page<Variantes> page = iVarianteRepository.findByProductoId(productoId, PageRequest.of(pagina - 1, size));
        PginaDto<List<VarianteResumenDto>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(page.getContent().stream().map(this::toResumenDto).toList());
        return resultado;
    }

    private VarianteResumenDto toResumenDto(Variantes v) {
        VarianteResumenDto dto = new VarianteResumenDto();
        dto.setId(v.getId());
        dto.setTalla(v.getTalla());
        dto.setDescripcion(v.getDescripcion());
        dto.setColor(v.getColor());
        dto.setPresentacion(v.getPresentacion());
        dto.setStock(v.getStock());
        dto.setMarca(v.getMarca());
        dto.setContenidoNeto(v.getContenidoNeto());

        List<VarianteImagen> imagenes = iVarianteImagenRepository.findByVarianteId(v.getId());
        if (!imagenes.isEmpty()) {
            Long imagenId = imagenes.get(0).getImagen().getId();
            try {
                List<ImagenDto> bytesList = imageneClienteAWS.getAll(List.of(imagenId));
                if (bytesList != null && !bytesList.isEmpty() && bytesList.get(0).getImagen() != null) {
                    dto.setImagenBase64(Base64.getEncoder().encodeToString(bytesList.get(0).getImagen()));
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener imagen para variante {}: {}", v.getId(), e.getMessage());
            }
        }
        return dto;
    }

}
