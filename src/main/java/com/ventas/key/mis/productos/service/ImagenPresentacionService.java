package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.config.RabbitMQConfig;
import com.ventas.key.mis.productos.dto.negocio.ImagenPresentacionUpdateDto;
import com.ventas.key.mis.productos.entity.ImagenPresentacion;
import com.ventas.key.mis.productos.models.ImagenPresentacionDto;
import com.ventas.key.mis.productos.repository.IImagenPresentacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImagenPresentacionService {

    private final IImagenPresentacionRepository repo;
    private final RabbitTemplate rabbitTemplate;
    private final CacheService cacheService;

    @Value("${guardar-imagenes.ruta_imagenes}")
    private String rutaImagenes;

    /** @deprecated Usar getImagenesPorTipoV2 — retorna DTO con urlImagen apuntando al micro */
    @Deprecated
    public List<ImagenPresentacion> getImagenesPorTipo(String tipo) {
        return repo.findByTipoAndActivoOrderByOrden(tipo.toUpperCase(), true);
    }

    @Cacheable(value = "presentacion-imagenes", key = "#tipo.toUpperCase()")
    public List<ImagenPresentacionDto> getImagenesPorTipoV2(String tipo) {
        return repo.findByTipoAndActivoOrderByOrden(tipo.toUpperCase(), true)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private ImagenPresentacionDto toDto(ImagenPresentacion img) {
        ImagenPresentacionDto dto = new ImagenPresentacionDto();
        dto.setId(img.getId());
        dto.setTipo(img.getTipo());
        dto.setOrden(img.getOrden());
        dto.setExtension(img.getExtension());
        dto.setNombreOriginal(img.getNombreOriginal());
        dto.setDescripcion(img.getDescripcion());
        dto.setActivo(img.isActivo());
        dto.setActualizadoEn(img.getActualizadoEn());
        dto.setUrlImagen("/presentacion/v2/imagenes/" + img.getId() + "/imagen");
        return dto;
    }

    /** @deprecated Usar getTodasV2 — expone nombreArchivo (ruta interna de disco) */
    @Deprecated
    public List<ImagenPresentacion> getTodas() {
        return repo.findAll();
    }

    public List<ImagenPresentacionDto> getTodasV2() {
        return repo.findAll().stream().map(this::toDto).toList();
    }

    /**
     * @deprecated Usar actualizarV2 — no invalida caché y devuelve entidad con nombreArchivo interno
     */
    @Deprecated
    @Transactional
    public ImagenPresentacion actualizar(Integer id, ImagenPresentacionUpdateDto dto) {
        ImagenPresentacion imagen = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Imagen no encontrada: " + id));

        if (dto.getBase64() != null && dto.getBase64().length > 0) {
            eliminarArchivoEnDisco(imagen.getNombreArchivo());

            String nombreArchivo = UUID.randomUUID() + "_" + dto.getNombreImagen();
            Path path = Paths.get(rutaImagenes, nombreArchivo);
            try {
                File directorio = new File(rutaImagenes);
                if (!directorio.exists()) directorio.mkdirs();
                Files.write(path, dto.getBase64());
            } catch (IOException e) {
                throw new RuntimeException("Error al guardar imagen en disco", e);
            }
            imagen.setNombreArchivo(nombreArchivo);
            imagen.setExtension(dto.getExtension());
            imagen.setNombreOriginal(dto.getNombreImagen());
        }

        if (dto.getDescripcion() != null) imagen.setDescripcion(dto.getDescripcion());
        if (dto.getActivo() != null)      imagen.setActivo(dto.getActivo());
        imagen.setActualizadoEn(LocalDateTime.now());
        cacheService.evictAll();
        return repo.save(imagen);
    }

    @Transactional
    public ImagenPresentacionDto actualizarV2(Integer id, ImagenPresentacionUpdateDto dto) {
        ImagenPresentacion imagen = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Imagen no encontrada: " + id));

        if (dto.getBase64() != null && dto.getBase64().length > 0) {
            eliminarArchivoEnDisco(imagen.getNombreArchivo());
            String nombreArchivo = UUID.randomUUID() + "_" + dto.getNombreImagen();
            Path path = Paths.get(rutaImagenes, nombreArchivo);
            try {
                File directorio = new File(rutaImagenes);
                if (!directorio.exists()) directorio.mkdirs();
                Files.write(path, dto.getBase64());
            } catch (IOException e) {
                throw new RuntimeException("Error al guardar imagen en disco", e);
            }
            imagen.setNombreArchivo(nombreArchivo);
            imagen.setExtension(dto.getExtension());
            imagen.setNombreOriginal(dto.getNombreImagen());
        }

        if (dto.getDescripcion() != null) imagen.setDescripcion(dto.getDescripcion());
        if (dto.getActivo() != null)      imagen.setActivo(dto.getActivo());
        imagen.setActualizadoEn(LocalDateTime.now());
        ImagenPresentacionDto resultado = toDto(repo.save(imagen));
        cacheService.evictAll();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_IMAGENES, RabbitMQConfig.ROUTING_KEY_CACHE_EVICT_ALL, "evict");
        return resultado;
    }

    public byte[] getImagenBytes(Integer id) throws IOException {
        ImagenPresentacion imagen = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Imagen no encontrada: " + id));
        if (imagen.getNombreArchivo() == null || imagen.getNombreArchivo().isBlank()) {
            throw new IOException("La presentación id=" + id + " no tiene imagen en disco");
        }
        Path path = Paths.get(rutaImagenes, imagen.getNombreArchivo());
        return Files.readAllBytes(path);
    }

    public MediaType getMediaType(Integer id) {
        return repo.findById(id).map(img -> {
            if (img.getExtension() == null) return MediaType.APPLICATION_OCTET_STREAM;
            return switch (img.getExtension().toLowerCase()) {
                case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
                case "png"         -> MediaType.IMAGE_PNG;
                case "gif"         -> MediaType.IMAGE_GIF;
                default            -> MediaType.APPLICATION_OCTET_STREAM;
            };
        }).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private void eliminarArchivoEnDisco(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) return;
        try {
            Path path = Paths.get(rutaImagenes, nombreArchivo);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo anterior: {}", nombreArchivo);
        }
    }
}