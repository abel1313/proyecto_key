package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenPort;
import com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto;
import com.ventas.key.mis.productos.models.*;
import com.ventas.key.mis.productos.repository.BaseRepository;
import com.ventas.key.mis.productos.repository.IImagenRepository;
import com.ventas.key.mis.productos.service.api.IImagenService;
import com.ventas.key.mis.productos.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ImagenServiceImpl extends CrudAbstractServiceImpl<
        Imagen,
        List<Imagen>,
        Optional<Imagen>,
        Long,
        PginaDto<List<Imagen>>> implements IImagenService {

    @Value("${guardar-imagenes.ruta_imagenes}")
    private String rutaImagenes;

    private final IImagenRepository iImagenRepository;
    private final ImagenPort imagenPort;

    @Autowired private CacheService cacheService;
    @Autowired private RabbitTemplate rabbitTemplate;

    public ImagenServiceImpl(BaseRepository<Imagen, Long> repoGenerico, ErrorGenerico error,
                             final IImagenRepository iImagenRepository,
                             final ImagenPort imagenPort) {
        super(repoGenerico, error);
        this.iImagenRepository = iImagenRepository;
        this.imagenPort = imagenPort;
    }


    @Override
    public List<Imagen> saveAll(List<Imagen> list) {
        return this.iImagenRepository.saveAll(list);
    }

    @Override
    public List<ImagenProductoResult> findIdsImagenesProducto(List<Integer> list) {
        return this.iImagenRepository.findImagenPrincipalPorProductoIds(list);
    }

    @Override
    @Cacheable(value = "detalle", key = "'id:' + #id + ':page:' + #page + ':size:' + #size")
    public PageableDto<List<ImagenProductoBase64>> findImagenPrincipalPorProductoIds(Integer id, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ImagenProductoDto> paginaDB = iImagenRepository.findImagenPrincipalPorProductoIds(id, pageable);

        List<ImagenProductoBase64> conImagen = paginaDB.getContent().stream()
                .filter(mpa -> mpa.getImage() != null)
                .map(mpa -> {
                    Path path = Paths.get(rutaImagenes, mpa.getImage());
                    if (!Files.exists(path)) {
                        log.warn("Imagen no encontrada en disco imagenId={}", mpa.getIdImagen());
                        return null;
                    }
                    try {
                        ImagenProductoBase64 dto = new ImagenProductoBase64();
                        dto.setIdProducto(mpa.getIdProducto());
                        dto.setIdImagen(mpa.getIdImagen());
                        dto.setName(mpa.getName());
                        dto.setPrice(mpa.getPrice());
                        dto.setInventoryStatus(mpa.getInventoryStatus());
                        dto.setImage(Files.readAllBytes(path));
                        return dto;
                    } catch (IOException e) {
                        log.warn("Error leyendo imagen imagenId={}: {}", mpa.getIdImagen(), e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        PageableDto<List<ImagenProductoBase64>> resultado = new PageableDto<>();
        resultado.setList(conImagen);
        resultado.setTotalPaginas((int) paginaDB.getTotalPages());
        return resultado;
    }

    // RabbitMQ: NO aplica — lectura síncrona. Bytes del micro vía HTTP batch.
    @Override
    @Cacheable(value = "detalle-v2", key = "'id:' + #productoId + ':page:' + #page + ':size:' + #size")
    public PageableDto<List<ImagenProductoBase64>> findImagenPrincipalPorProductoIdsV2(Integer productoId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ImagenProductoDto> paginaDB = iImagenRepository.findImagenPrincipalPorProductoIds(productoId, pageable);
        if (paginaDB.isEmpty()) {
            PageableDto<List<ImagenProductoBase64>> vacio = new PageableDto<>();
            vacio.setList(List.of());
            vacio.setTotalPaginas(0);
            return vacio;
        }

        List<Long> idsEnPagina = paginaDB.getContent().stream().map(ImagenProductoDto::getIdImagen).toList();
        Map<Long, byte[]> bytesById;
        try {
            bytesById = imagenPort.getAll(idsEnPagina).stream()
                    .filter(dto -> dto.getImagen() != null)
                    .collect(Collectors.toMap(ImagenDto::getId, ImagenDto::getImagen));
        } catch (Exception e) {
            log.warn("Error al obtener imágenes del micro para productoId={}: {}", productoId, e.getMessage());
            bytesById = Map.of();
        }

        Map<Long, byte[]> bytesFinal = bytesById;
        List<ImagenProductoBase64> conImagen = paginaDB.getContent().stream()
                .filter(mpa -> bytesFinal.containsKey(mpa.getIdImagen()))
                .map(mpa -> {
                    ImagenProductoBase64 dto = new ImagenProductoBase64();
                    dto.setIdProducto(mpa.getIdProducto());
                    dto.setIdImagen(mpa.getIdImagen());
                    dto.setName(mpa.getName());
                    dto.setPrice(mpa.getPrice());
                    dto.setInventoryStatus(mpa.getInventoryStatus());
                    dto.setImage(bytesFinal.get(mpa.getIdImagen()));
                    return dto;
                })
                .toList();

        PageableDto<List<ImagenProductoBase64>> resultado = new PageableDto<>();
        resultado.setList(conImagen);
        resultado.setTotalPaginas((int) paginaDB.getTotalPages());
        return resultado;
    }

    @Deprecated
    @Override
    @CacheEvict(value = {"detalleImagen", "variantesImagenesCache", "variantesProductoCache", "detalle"}, allEntries = true)
    public void deleteById(Long id) {
        this.iImagenRepository.deleteById(id);
    }

    @Override
    public void deleteByIdV2(Long id) {
        try {
            imagenPort.delete(List.of(id));
        } catch (Exception e) {
            log.warn("No se pudo eliminar imagen del micro imagenId={}: {}", id, e.getMessage());
        }
        iImagenRepository.deleteById(id);
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
    }


    @Override
    public void deleteByIds(List<Long> ids) {
        this.iImagenRepository.deleteByIdIn(ids);
    }

    @Override
    public com.ventas.key.mis.productos.hexagonal.dominio.Imagen findByIdImg(Integer id) throws IOException {
        Optional<ImagenProductoResult> imfRes = this.iImagenRepository.findImagenByImg(id);
        Optional<Imagen> img = this.iImagenRepository.findById(imfRes.isPresent() ? imfRes.get().getImagenId() : 0L);
        if (img.isPresent()) {
            Path path = Paths.get(rutaImagenes, img.get().getBase64());
            log.info("info {}",path);
            com.ventas.key.mis.productos.hexagonal.dominio.Imagen devolverImagen = new com.ventas.key.mis.productos.hexagonal.dominio.Imagen();
            devolverImagen.setImagen(Files.readAllBytes(path));
            devolverImagen.setContentType(img.get().getExtension());
            return devolverImagen;
        }
        throw new IOException("No se encontro el imagen");
    }

    @Override
    public com.ventas.key.mis.productos.hexagonal.dominio.Imagen findByImagenId(Long imagenId) throws IOException {
        Optional<Imagen> img = this.iImagenRepository.findById(imagenId);
        if (img.isPresent()) {
            Path path = Paths.get(rutaImagenes, img.get().getBase64());
            com.ventas.key.mis.productos.hexagonal.dominio.Imagen devolverImagen = new com.ventas.key.mis.productos.hexagonal.dominio.Imagen();
            devolverImagen.setImagen(Files.readAllBytes(path));
            devolverImagen.setContentType(img.get().getExtension());
            return devolverImagen;
        }
        throw new IOException("No se encontro la imagen con id: " + imagenId);
    }
}
