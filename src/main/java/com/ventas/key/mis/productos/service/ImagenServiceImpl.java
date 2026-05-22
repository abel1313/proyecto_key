package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenPort;
import com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto;
import com.ventas.key.mis.productos.models.*;
import com.ventas.key.mis.productos.repository.BaseRepository;
import com.ventas.key.mis.productos.repository.IImagenRepository;
import com.ventas.key.mis.productos.service.api.IImagenService;
import lombok.extern.slf4j.Slf4j;
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
        List<ImagenProductoDto> todas = iImagenRepository.findImagenPrincipalPorProductoIds55(id);

        List<ImagenProductoBase64> conImagen = todas.stream()
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

        return paginarEnMemoria(conImagen, page, size);
    }

    // RabbitMQ: NO aplica — lectura síncrona. Bytes del micro vía HTTP batch.
    @Override
    @Cacheable(value = "detalle-v2", key = "'id:' + #productoId + ':page:' + #page + ':size:' + #size")
    public PageableDto<List<ImagenProductoBase64>> findImagenPrincipalPorProductoIdsV2(Integer productoId, int page, int size) {
        List<ImagenProductoDto> todas = iImagenRepository.findImagenPrincipalPorProductoIds55(productoId);
        if (todas.isEmpty()) return paginarEnMemoria(List.of(), page, size);

        List<Long> todosIds = todas.stream().map(ImagenProductoDto::getIdImagen).toList();
        Map<Long, byte[]> bytesById;
        try {
            bytesById = imagenPort.getAll(todosIds).stream()
                    .filter(dto -> dto.getImagen() != null)
                    .collect(Collectors.toMap(ImagenDto::getId, ImagenDto::getImagen));
        } catch (Exception e) {
            log.warn("Error al obtener imágenes del micro para productoId={}: {}", productoId, e.getMessage());
            bytesById = Map.of();
        }

        Map<Long, byte[]> bytesFinal = bytesById;
        List<ImagenProductoBase64> conImagen = todas.stream()
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

        return paginarEnMemoria(conImagen, page, size);
    }

    @Deprecated
    @Override
    @CacheEvict(value = {"detalleImagen", "variantesImagenesCache", "variantesProductoCache", "detalle"}, allEntries = true)
    public void deleteById(Long id) {
        this.iImagenRepository.deleteById(id);
    }

    // TODO: RabbitMQ — candidato para publicar evento "imagen.eliminada" a exchange.imagenes
    //   para que el micro procese la eliminación del archivo de forma asíncrona.
    @Override
    @CacheEvict(value = {"detalleImagen", "variantesImagenesCache", "variantesProductoCache", "detalle"}, allEntries = true)
    public void deleteByIdV2(Long id) {
        // el micro necesita el registro en BD para encontrar el path del archivo en disco
        // por eso se borra primero el archivo y después el registro
        try {
            imagenPort.delete(List.of(id));
        } catch (Exception e) {
            log.warn("No se pudo eliminar imagen del micro imagenId={}: {}", id, e.getMessage());
        }
        iImagenRepository.deleteById(id);
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

    private PageableDto<List<ImagenProductoBase64>> paginarEnMemoria(List<ImagenProductoBase64> lista, int page, int size) {
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, lista.size());
        List<ImagenProductoBase64> paginado = fromIndex >= lista.size() ? List.of() : lista.subList(fromIndex, toIndex);
        int totalPaginas = size == 0 ? 0 : (int) Math.ceil((double) lista.size() / size);
        PageableDto<List<ImagenProductoBase64>> resultado = new PageableDto<>();
        resultado.setList(paginado);
        resultado.setTotalPaginas(totalPaginas);
        return resultado;
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
