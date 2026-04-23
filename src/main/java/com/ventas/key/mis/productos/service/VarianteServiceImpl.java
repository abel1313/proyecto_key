package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.entity.productoVariantes.VarianteImagen;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.ImagenDTO;
import com.ventas.key.mis.productos.models.ImagenUpdateDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.VarianteDetalle;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.service.api.IImagenService;
import com.ventas.key.mis.productos.service.api.IVarianteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class VarianteServiceImpl extends CrudAbstractServiceImpl<Variantes, List<Variantes>, Optional<Variantes>, Integer, PginaDto<List<Variantes>>>
        implements IVarianteService {

    @Value("${guardar-imagenes.ruta_imagenes}")
    private String rutaImagenes;

    private final IVarianteRepository iVarianteRepository;
    private final IVarianteImagenRepository iVarianteImagenRepository;
    private final IImagenService iImagenService;
    private final IProductosRepository iProductosRepository;

    public VarianteServiceImpl(IVarianteRepository iVarianteRepository,
                               IVarianteImagenRepository iVarianteImagenRepository,
                               IImagenService iImagenService,
                               IProductosRepository iProductosRepository,
                               ErrorGenerico error) {
        super(iVarianteRepository, error);
        this.iVarianteRepository = iVarianteRepository;
        this.iVarianteImagenRepository = iVarianteImagenRepository;
        this.iImagenService = iImagenService;
        this.iProductosRepository = iProductosRepository;
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
        Variantes variante = buildVariante(detalle);
        Variantes saved = save(variante);

        if (detalle.getListImagenes() != null && !detalle.getListImagenes().isEmpty()) {
            List<Imagen> imagenes = mappImagenes(detalle.getListImagenes());
            List<Imagen> savedImagenes = iImagenService.saveAll(imagenes);
            List<VarianteImagen> relaciones = savedImagenes.stream().map(img -> {
                VarianteImagen vi = new VarianteImagen();
                vi.setVariante(saved);
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

    private List<Imagen> mappImagenes(List<ImagenDTO> list) {
        return list.stream().map(dto -> {
            Imagen imagen = new Imagen();
            byte[] decodedBytes = dto.getBase64();
            String urlImagen = UUID.randomUUID() + "_" + dto.getNombreImagen();
            Path path = Paths.get(rutaImagenes, urlImagen);
            try {
                File directorio = new File(rutaImagenes);
                if (!directorio.exists()) directorio.mkdirs();
                Files.write(path, decodedBytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Long idImagen = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
            imagen.setId(idImagen);
            imagen.setBase64(urlImagen);
            imagen.setNombreImagen(dto.getNombreImagen());
            imagen.setExtension(dto.getExtension());
            return imagen;
        }).toList();
    }
}
