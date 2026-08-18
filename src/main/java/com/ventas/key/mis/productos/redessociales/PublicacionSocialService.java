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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
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
    private final InstagramGraphClient instagramGraphClient;
    private final TikTokGraphClient tikTokGraphClient;

    @Value("${api.imagenes:}")
    private String endpointImagenes;

    @PostConstruct
    void init() {
        if (endpointImagenes != null && !endpointImagenes.isBlank() && !endpointImagenes.endsWith("/")) {
            endpointImagenes = endpointImagenes + "/";
        }
    }

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

        return guardarPublicacion(variante, "foto", request.getDescripcion(), imagenId,
                postId, request.getScheduledPublishTime(), scheduledEpoch);
    }

    /**
     * Publica un video en el feed de la página. A diferencia de la foto, no hay "video principal
     * de la variante" al cual caer -- el catálogo no guarda video de variantes, así que el
     * archivo siempre viene en el request, nunca se persiste en el microservicio de imágenes.
     */
    @Transactional
    public PublicacionSocialDto publicarVideoEnFacebook(Integer varianteId, String descripcion,
                                                          LocalDateTime scheduledPublishTime, MultipartFile video) {
        Variantes variante = varianteRepository.findById(varianteId)
                .orElseThrow(() -> new ExceptionDataNotFound("No existe la variante con id " + varianteId));

        if (video == null || video.isEmpty()) {
            throw new ExceptionErrorInesperado("Falta el archivo de video a publicar");
        }

        byte[] bytesVideo;
        try {
            bytesVideo = video.getBytes();
        } catch (IOException e) {
            throw new ExceptionErrorInesperado("No se pudo leer el video enviado: " + e.getMessage());
        }

        Long scheduledEpoch = null;
        if (scheduledPublishTime != null) {
            validarVentanaProgramacion(scheduledPublishTime);
            scheduledEpoch = scheduledPublishTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        }

        log.info("Publicando video en Facebook, varianteId={}, bytes={}", variante.getId(), bytesVideo.length);
        String videoId = facebookGraphClient.publicarVideo(
                bytesVideo, video.getContentType(), descripcion, scheduledEpoch);

        return guardarPublicacion(variante, "video", descripcion, null,
                videoId, scheduledPublishTime, scheduledEpoch);
    }

    /**
     * Reel de la página. Sin programar -- video_reels no lo soporta igual que /videos, siempre
     * publica de inmediato. Mismo criterio que el video normal: el archivo siempre viene en el
     * request, nunca se persiste.
     */
    @Transactional
    public PublicacionSocialDto publicarReelEnFacebook(Integer varianteId, String descripcion, MultipartFile video) {
        Variantes variante = varianteRepository.findById(varianteId)
                .orElseThrow(() -> new ExceptionDataNotFound("No existe la variante con id " + varianteId));

        if (video == null || video.isEmpty()) {
            throw new ExceptionErrorInesperado("Falta el archivo de video a publicar");
        }

        byte[] bytesVideo;
        try {
            bytesVideo = video.getBytes();
        } catch (IOException e) {
            throw new ExceptionErrorInesperado("No se pudo leer el video enviado: " + e.getMessage());
        }

        log.info("Publicando Reel en Facebook, varianteId={}, bytes={}", variante.getId(), bytesVideo.length);
        String videoId = facebookGraphClient.publicarReel(bytesVideo, video.getContentType(), descripcion);

        return guardarPublicacion(variante, "reel", descripcion, null, videoId, null, null);
    }

    // Primera version de Instagram, alcance recortado a proposito: solo imagen ya guardada en el
    // catalogo (imagenId o la principal de la variante), nunca un archivo ad-hoc -- Instagram
    // necesita una URL publica para la imagen, no acepta bytes subidos directo como Facebook, y
    // un archivo recien llegado del admin todavia no tiene esa URL sin antes guardarlo en el
    // microservicio de imagenes (paso que se deja para una version futura si hace falta). Tampoco
    // soporta programar -- la Content Publishing API de Instagram siempre publica de inmediato.
    @Transactional
    public PublicacionSocialDto publicarEnInstagram(PublicarInstagramRequest request) {
        Variantes variante = varianteRepository.findById(request.getVarianteId())
                .orElseThrow(() -> new ExceptionDataNotFound(
                        "No existe la variante con id " + request.getVarianteId()));

        Long imagenId = request.getImagenId() != null ? request.getImagenId() : imagenPrincipalDe(variante.getId());
        if (endpointImagenes == null || endpointImagenes.isBlank()) {
            throw new ExceptionErrorInesperado("No se pudo construir la URL publica de la imagen: falta configurar api.imagenes");
        }
        String urlImagen = endpointImagenes + "v1/imagenes/file/" + imagenId;

        String mediaId = instagramGraphClient.publicarFoto(urlImagen, request.getDescripcion());

        return guardarPublicacionInstagram(variante, request.getDescripcion(), imagenId, mediaId);
    }

    private PublicacionSocialDto guardarPublicacionInstagram(Variantes variante, String descripcion,
                                                                Long imagenId, String mediaId) {
        return guardarPublicacionInstagram(variante, descripcion, imagenId, mediaId, "foto");
    }

    private PublicacionSocialDto guardarPublicacionInstagram(Variantes variante, String descripcion,
                                                                Long imagenId, String mediaId, String tipoPublicacion) {
        PublicacionSocial publicacion = new PublicacionSocial();
        publicacion.setVariante(variante);
        publicacion.setPlataforma("instagram");
        publicacion.setTipoPublicacion(tipoPublicacion);
        publicacion.setDescripcionPublicada(descripcion);
        publicacion.setImagenId(imagenId);
        publicacion.setPostIdFacebook(mediaId);
        publicacion.setFechaPublicacion(LocalDateTime.now());
        publicacion.setEstado("PUBLICADA");

        publicacion = publicacionSocialRepository.save(publicacion);
        log.info("Publicación en Instagram creada: tipo={}, varianteId={}, mediaId={}", tipoPublicacion, variante.getId(), mediaId);

        return PublicacionSocialDto.from(publicacion);
    }

    // Reel de Instagram -- a diferencia de la foto, el video SIEMPRE viene en el request (igual
    // que el video de Facebook): el catalogo no guarda video de variantes, no hay "principal" a
    // la cual caer. InstagramGraphClient absorbe toda la complejidad de la subida reanudable, el
    // front sigue mandando un multipart normal como con Facebook.
    @Transactional
    public PublicacionSocialDto publicarReelEnInstagram(Integer varianteId, String descripcion, MultipartFile video) {
        Variantes variante = varianteRepository.findById(varianteId)
                .orElseThrow(() -> new ExceptionDataNotFound("No existe la variante con id " + varianteId));

        if (video == null || video.isEmpty()) {
            throw new ExceptionErrorInesperado("Falta el archivo de video a publicar");
        }

        byte[] bytesVideo;
        try {
            bytesVideo = video.getBytes();
        } catch (IOException e) {
            throw new ExceptionErrorInesperado("No se pudo leer el video enviado: " + e.getMessage());
        }

        log.info("Publicando Reel en Instagram, varianteId={}, bytes={}", variante.getId(), bytesVideo.length);
        String mediaId = instagramGraphClient.publicarReel(bytesVideo, video.getContentType(), descripcion);

        return guardarPublicacionInstagram(variante, descripcion, null, mediaId, "reel");
    }

    /**
     * Publica un video en TikTok (Direct Post). Mismo criterio que el resto: el archivo siempre
     * viene en el request, nunca se persiste en el microservicio de imágenes. Mientras la app no
     * esté auditada por TikTok, solo funciona con cuentas agregadas como Target User en Sandbox,
     * y el video sale forzado a privado -- ver TikTokGraphClient.
     */
    @Transactional
    public PublicacionSocialDto publicarEnTikTok(Integer varianteId, String descripcion, MultipartFile video) {
        Variantes variante = varianteRepository.findById(varianteId)
                .orElseThrow(() -> new ExceptionDataNotFound("No existe la variante con id " + varianteId));

        if (video == null || video.isEmpty()) {
            throw new ExceptionErrorInesperado("Falta el archivo de video a publicar");
        }

        byte[] bytesVideo;
        try {
            bytesVideo = video.getBytes();
        } catch (IOException e) {
            throw new ExceptionErrorInesperado("No se pudo leer el video enviado: " + e.getMessage());
        }

        log.info("Publicando en TikTok, varianteId={}, bytes={}", variante.getId(), bytesVideo.length);
        String publishId = tikTokGraphClient.publicarVideo(bytesVideo, video.getContentType(), descripcion);

        PublicacionSocial publicacion = new PublicacionSocial();
        publicacion.setVariante(variante);
        publicacion.setPlataforma("tiktok");
        publicacion.setTipoPublicacion("video");
        publicacion.setDescripcionPublicada(descripcion);
        publicacion.setPostIdFacebook(publishId);
        publicacion.setFechaPublicacion(LocalDateTime.now());
        publicacion.setEstado("PUBLICADA");
        publicacion = publicacionSocialRepository.save(publicacion);

        return PublicacionSocialDto.from(publicacion);
    }

    private PublicacionSocialDto guardarPublicacion(Variantes variante, String tipoPublicacion, String descripcion,
                                                      Long imagenId, String postId,
                                                      LocalDateTime scheduledPublishTime, Long scheduledEpoch) {
        PublicacionSocial publicacion = new PublicacionSocial();
        publicacion.setVariante(variante);
        publicacion.setPlataforma("facebook");
        publicacion.setTipoPublicacion(tipoPublicacion);
        publicacion.setDescripcionPublicada(descripcion);
        publicacion.setImagenId(imagenId);
        publicacion.setPostIdFacebook(postId);
        publicacion.setScheduledPublishTime(scheduledPublishTime);
        publicacion.setFechaPublicacion(LocalDateTime.now());
        publicacion.setEstado(scheduledEpoch != null ? "PROGRAMADA" : "PUBLICADA");

        publicacion = publicacionSocialRepository.save(publicacion);
        log.info("Publicación en Facebook creada: tipo={}, varianteId={}, postId={}, estado={}",
                tipoPublicacion, variante.getId(), postId, publicacion.getEstado());

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
