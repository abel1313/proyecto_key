package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.dto.negocio.LogoUploadDto;
import com.ventas.key.mis.productos.entity.Logo;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.LogoDto;
import com.ventas.key.mis.productos.repository.ILogoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Gestión de logos subidos por el admin -- pedido 2026-08-28: agregar el logo real de la marca
 * (hasta ahora el encabezado de los correos solo tenía ícono+texto, ver EmailService) y permitir
 * varios logos guardados, con uno marcado como "el que se usa en los correos".
 *
 * Almacenamiento: disco local (misma ruta ya usada para producto/presentación,
 * guardar-imagenes.ruta_imagenes) -- no depende del microservicio externo de imágenes para algo
 * tan simple como el logo de la marca, y ya está resuelto/probado en este proyecto.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogoService {

    private final ILogoRepository repo;

    @Value("${guardar-imagenes.ruta_imagenes}")
    private String rutaImagenes;

    @Cacheable("logosCache")
    public List<LogoDto> listar() {
        return repo.findAllByOrderByCreadoEnDesc().stream().map(this::toDto).toList();
    }

    /** Público -- el que hoy está activo, si hay alguno (usado por EmailService y por el front). */
    @Cacheable("logoActivoCache")
    public Optional<LogoDto> obtenerActivo() {
        return repo.findByActivoTrue().map(this::toDto);
    }

    @Transactional
    @CacheEvict(value = {"logosCache", "logoActivoCache"}, allEntries = true)
    public LogoDto subir(LogoUploadDto dto) {
        String nombreArchivo = UUID.randomUUID() + "_" + dto.getNombreImagen();
        Path path = Paths.get(rutaImagenes, nombreArchivo);
        try {
            File directorio = new File(rutaImagenes);
            if (!directorio.exists()) directorio.mkdirs();
            Files.write(path, dto.getBase64());
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el logo en disco", e);
        }

        Logo logo = new Logo();
        logo.setNombreArchivo(nombreArchivo);
        logo.setExtension(dto.getExtension());
        logo.setNombreOriginal(dto.getNombreImagen());
        // El primer logo que se sube queda activo automáticamente -- si no, habría que subir Y
        // activar a mano para que el correo deje de verse con el ícono genérico.
        logo.setActivo(repo.count() == 0);
        logo.setCreadoEn(LocalDateTime.now());
        return toDto(repo.save(logo));
    }

    /** Marca este logo como el usado en correos y desactiva cualquier otro (selección única). */
    @Transactional
    @CacheEvict(value = {"logosCache", "logoActivoCache"}, allEntries = true)
    public LogoDto activar(Integer id) {
        Logo logo = repo.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Logo no encontrado: " + id));
        repo.findByActivoTrue().ifPresent(actual -> {
            if (!actual.getId().equals(id)) {
                actual.setActivo(false);
                repo.save(actual);
            }
        });
        logo.setActivo(true);
        return toDto(repo.save(logo));
    }

    @Transactional
    @CacheEvict(value = {"logosCache", "logoActivoCache"}, allEntries = true)
    public void eliminar(Integer id) {
        Logo logo = repo.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Logo no encontrado: " + id));
        try {
            Files.deleteIfExists(Paths.get(rutaImagenes, logo.getNombreArchivo()));
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo del logo id={}: {}", id, e.getMessage());
        }
        repo.delete(logo);
    }

    public byte[] obtenerBytes(Integer id) throws IOException {
        Logo logo = repo.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Logo no encontrado: " + id));
        return Files.readAllBytes(Paths.get(rutaImagenes, logo.getNombreArchivo()));
    }

    public MediaType obtenerMediaType(Integer id) {
        return repo.findById(id).map(logo -> {
            if (logo.getExtension() == null) return MediaType.APPLICATION_OCTET_STREAM;
            return switch (logo.getExtension().toLowerCase()) {
                case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
                case "png"         -> MediaType.IMAGE_PNG;
                case "gif"         -> MediaType.IMAGE_GIF;
                default            -> MediaType.APPLICATION_OCTET_STREAM;
            };
        }).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private LogoDto toDto(Logo logo) {
        return new LogoDto(
                logo.getId(),
                logo.getExtension(),
                logo.getNombreOriginal(),
                logo.isActivo(),
                logo.getCreadoEn(),
                "/logos/" + logo.getId() + "/imagen"
        );
    }
}
