package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.entity.productoVariantes.VarianteImagen;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenPort;
import com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PublicacionSocialService {

    private final IVarianteRepository varianteRepository;
    private final IVarianteImagenRepository varianteImagenRepository;
    private final IPublicacionSocialRepository publicacionSocialRepository;
    private final ImagenPort imagenPort;
    private final FacebookGraphClient facebookGraphClient;

    @Transactional
    public PublicacionSocialDto publicarEnFacebook(PublicarFacebookRequest request, MultipartFile imagenNueva) {
        Variantes variante = varianteRepository.findById(request.getVarianteId())
                .orElseThrow(() -> new ExceptionDataNotFound(
                        "No existe la variante con id " + request.getVarianteId()));

        byte[] bytesImagen;
        String contentType;
        Long imagenId = null;

        if (imagenNueva != null && !imagenNueva.isEmpty()) {
            // Imagen ad-hoc: se sube tal cual llegó del disco/cámara del admin, directo a
            // Facebook. NO se guarda en el microservicio de imágenes ni se vincula a la
            // variante -- es exclusiva de esta publicación, a máxima calidad, sin pasar por
            // el redimensionado que sí se aplica a los thumbnails de las listas.
            try {
                bytesImagen = imagenNueva.getBytes();
            } catch (IOException e) {
                throw new ExceptionErrorInesperado("No se pudo leer la imagen enviada: " + e.getMessage());
            }
            contentType = imagenNueva.getContentType();
            log.info("Publicando en Facebook con imagen ad-hoc (sin guardar en galería), varianteId={}, bytes={}",
                    variante.getId(), bytesImagen.length);
        } else {
            imagenId = request.getImagenId() != null ? request.getImagenId() : imagenPrincipalDe(variante.getId());
            ImagenDto imagen = imagenPort.getOne(imagenId);
            if (imagen == null || imagen.getImagen() == null) {
                throw new ExceptionErrorInesperado(
                        "La variante " + variante.getId() + " no tiene una imagen disponible para publicar");
            }
            bytesImagen = imagen.getImagen();
            contentType = imagen.getContentType();
        }

        Long scheduledEpoch = null;
        if (request.getScheduledPublishTime() != null) {
            validarVentanaProgramacion(request.getScheduledPublishTime());
            scheduledEpoch = request.getScheduledPublishTime()
                    .atZone(ZoneId.systemDefault())
                    .toEpochSecond();
        }

        String postId = facebookGraphClient.publicarFoto(
                bytesImagen, contentType, request.getDescripcion(), scheduledEpoch);

        PublicacionSocial publicacion = new PublicacionSocial();
        publicacion.setVariante(variante);
        publicacion.setPlataforma("facebook");
        publicacion.setTipoPublicacion("foto");
        publicacion.setDescripcionPublicada(request.getDescripcion());
        publicacion.setImagenId(imagenId);
        publicacion.setPostIdFacebook(postId);
        publicacion.setScheduledPublishTime(request.getScheduledPublishTime());
        publicacion.setFechaPublicacion(LocalDateTime.now());
        publicacion.setEstado(scheduledEpoch != null ? "PROGRAMADA" : "PUBLICADA");

        publicacion = publicacionSocialRepository.save(publicacion);
        log.info("Publicación en Facebook creada: varianteId={}, postId={}, estado={}",
                variante.getId(), postId, publicacion.getEstado());

        return PublicacionSocialDto.from(publicacion);
    }

    private Long imagenPrincipalDe(Integer varianteId) {
        List<VarianteImagen> imagenes = varianteImagenRepository.findByVarianteIdIn(List.of(varianteId));
        if (imagenes.isEmpty()) {
            throw new ExceptionErrorInesperado(
                    "La variante " + varianteId + " no tiene ninguna imagen guardada");
        }
        // La query ya ordena principal=true primero.
        return imagenes.get(0).getImagen().getId();
    }

    private void validarVentanaProgramacion(LocalDateTime scheduledPublishTime) {
        LocalDateTime ahora = LocalDateTime.now();
        if (scheduledPublishTime.isBefore(ahora.plusMinutes(10))) {
            throw new ExceptionErrorInesperado(
                    "Facebook exige programar la publicación con al menos 10 minutos de anticipación");
        }
        if (scheduledPublishTime.isAfter(ahora.plusMonths(6))) {
            throw new ExceptionErrorInesperado(
                    "Facebook no permite programar publicaciones a más de 6 meses en el futuro");
        }
    }
}
