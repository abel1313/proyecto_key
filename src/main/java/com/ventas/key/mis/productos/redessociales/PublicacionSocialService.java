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
import java.util.List;

// Las 3 redes (Facebook, Instagram, TikTok) programan su publicacion de la MISMA forma: se
// guarda aqui con estado "PROGRAMADA" y PublicacionSocialScheduler la dispara a la hora exacta.
// Decision explicita del dueno (2026-08-18): aunque Facebook si tiene scheduler nativo de Meta
// (video_state=SCHEDULED), NO se usa -- Instagram y TikTok nunca tuvieron esa opcion (limite real
// de esas APIs, no nuestro), asi que si Facebook fuera nativo, las 3 podrian desincronizarse
// (Facebook sale aunque el servidor este caido a esa hora, las otras dos no). Con las 3 por el
// mismo job, siempre salen juntas o ninguna.
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

        byte[] bytesImagen = null;
        String contentType = null;
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
        }

        PublicacionSocial nueva = nuevaPublicacion(variante, "facebook", "foto", request.getDescripcion(),
                imagenId, bytesImagen, contentType);

        if (request.getScheduledPublishTime() != null) {
            return programar(nueva, request.getScheduledPublishTime());
        }
        return ejecutarYGuardar(nueva);
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

        byte[] bytesVideo = leerBytes(video, "video");

        PublicacionSocial nueva = nuevaPublicacion(variante, "facebook", "video", descripcion,
                null, bytesVideo, video.getContentType());

        if (scheduledPublishTime != null) {
            return programar(nueva, scheduledPublishTime);
        }
        return ejecutarYGuardar(nueva);
    }

    /** Reel de la página -- mismo criterio que el video: archivo siempre en el request. */
    @Transactional
    public PublicacionSocialDto publicarReelEnFacebook(Integer varianteId, String descripcion,
                                                         LocalDateTime scheduledPublishTime, MultipartFile video) {
        Variantes variante = varianteRepository.findById(varianteId)
                .orElseThrow(() -> new ExceptionDataNotFound("No existe la variante con id " + varianteId));

        byte[] bytesVideo = leerBytes(video, "video");

        PublicacionSocial nueva = nuevaPublicacion(variante, "facebook", "reel", descripcion,
                null, bytesVideo, video.getContentType());

        if (scheduledPublishTime != null) {
            return programar(nueva, scheduledPublishTime);
        }
        return ejecutarYGuardar(nueva);
    }

    // Primera version de Instagram, alcance recortado a proposito: solo imagen ya guardada en el
    // catalogo (imagenId o la principal de la variante), nunca un archivo ad-hoc -- Instagram
    // necesita una URL publica para la imagen, no acepta bytes subidos directo como Facebook, y
    // un archivo recien llegado del admin todavia no tiene esa URL sin antes guardarlo en el
    // microservicio de imagenes (paso que se deja para una version futura si hace falta).
    @Transactional
    public PublicacionSocialDto publicarEnInstagram(PublicarInstagramRequest request) {
        Variantes variante = varianteRepository.findById(request.getVarianteId())
                .orElseThrow(() -> new ExceptionDataNotFound(
                        "No existe la variante con id " + request.getVarianteId()));

        Long imagenId = request.getImagenId() != null ? request.getImagenId() : imagenPrincipalDe(variante.getId());

        PublicacionSocial nueva = nuevaPublicacion(variante, "instagram", "foto", request.getDescripcion(),
                imagenId, null, null);

        if (request.getScheduledPublishTime() != null) {
            return programar(nueva, request.getScheduledPublishTime());
        }
        return ejecutarYGuardar(nueva);
    }

    // Reel de Instagram -- el video SIEMPRE viene en el request (igual que el de Facebook): el
    // catalogo no guarda video de variantes, no hay "principal" a la cual caer.
    @Transactional
    public PublicacionSocialDto publicarReelEnInstagram(Integer varianteId, String descripcion,
                                                          LocalDateTime scheduledPublishTime, MultipartFile video) {
        Variantes variante = varianteRepository.findById(varianteId)
                .orElseThrow(() -> new ExceptionDataNotFound("No existe la variante con id " + varianteId));

        byte[] bytesVideo = leerBytes(video, "video");

        PublicacionSocial nueva = nuevaPublicacion(variante, "instagram", "reel", descripcion,
                null, bytesVideo, video.getContentType());

        if (scheduledPublishTime != null) {
            return programar(nueva, scheduledPublishTime);
        }
        return ejecutarYGuardar(nueva);
    }

    /**
     * Publica un video en TikTok (Direct Post). Mismo criterio que el resto: el archivo siempre
     * viene en el request, nunca se persiste en el microservicio de imágenes. Mientras la app no
     * esté auditada por TikTok, solo funciona con cuentas agregadas como Target User en Sandbox,
     * y el video sale forzado a privado -- ver TikTokGraphClient.
     */
    @Transactional
    public PublicacionSocialDto publicarEnTikTok(Integer varianteId, String descripcion,
                                                  LocalDateTime scheduledPublishTime, MultipartFile video) {
        Variantes variante = varianteRepository.findById(varianteId)
                .orElseThrow(() -> new ExceptionDataNotFound("No existe la variante con id " + varianteId));

        byte[] bytesVideo = leerBytes(video, "video");

        PublicacionSocial nueva = nuevaPublicacion(variante, "tiktok", "video", descripcion,
                null, bytesVideo, video.getContentType());

        if (scheduledPublishTime != null) {
            return programar(nueva, scheduledPublishTime);
        }
        return ejecutarYGuardar(nueva);
    }

    private byte[] leerBytes(MultipartFile video, String etiqueta) {
        if (video == null || video.isEmpty()) {
            throw new ExceptionErrorInesperado("Falta el archivo de " + etiqueta + " a publicar");
        }
        try {
            return video.getBytes();
        } catch (IOException e) {
            throw new ExceptionErrorInesperado("No se pudo leer el " + etiqueta + " enviado: " + e.getMessage());
        }
    }

    private PublicacionSocial nuevaPublicacion(Variantes variante, String plataforma, String tipoPublicacion,
                                                String descripcion, Long imagenId, byte[] contenidoBytes, String contentType) {
        PublicacionSocial p = new PublicacionSocial();
        p.setVariante(variante);
        p.setPlataforma(plataforma);
        p.setTipoPublicacion(tipoPublicacion);
        p.setDescripcionPublicada(descripcion);
        p.setImagenId(imagenId);
        p.setContenidoBytes(contenidoBytes);
        p.setContentType(contentType);
        p.setIntentos(0);
        return p;
    }

    // Min 10 minutos de anticipacion (mismo criterio operativo de siempre), sin el limite de 29
    // dias/6 meses que imponia cada red -- ya no aplica, es nuestro propio job, no su API.
    private PublicacionSocialDto programar(PublicacionSocial p, LocalDateTime scheduledPublishTime) {
        LocalDateTime ahora = LocalDateTime.now();
        if (scheduledPublishTime.isBefore(ahora.plusMinutes(10))) {
            throw new ExceptionErrorInesperado("Hay que programar la publicación con al menos 10 minutos de anticipación");
        }
        p.setScheduledPublishTime(scheduledPublishTime);
        p.setEstado("PROGRAMADA");
        PublicacionSocial guardada = publicacionSocialRepository.save(p);
        log.info("Publicación programada: plataforma={}, tipo={}, varianteId={}, para={}",
                p.getPlataforma(), p.getTipoPublicacion(), p.getVariante().getId(), scheduledPublishTime);
        return PublicacionSocialDto.from(guardada);
    }

    private PublicacionSocialDto ejecutarYGuardar(PublicacionSocial p) {
        ejecutar(p);
        PublicacionSocial guardada = publicacionSocialRepository.save(p);
        return PublicacionSocialDto.from(guardada);
    }

    // Llamado por PublicacionSocialScheduler para cada fila "PROGRAMADA" que ya llegó a su hora.
    // A diferencia de la publicación inmediata, aquí un error NO se propaga como excepción al
    // caller -- se guarda el fallo (intentos/ultimoError) para reintentar en la siguiente pasada
    // del job, hasta 3 veces, y ahí sí se marca "FALLIDA" en definitiva.
    @Transactional
    public void ejecutarProgramada(PublicacionSocial p) {
        try {
            ejecutar(p);
        } catch (Exception e) {
            int intentos = (p.getIntentos() == null ? 0 : p.getIntentos()) + 1;
            p.setIntentos(intentos);
            p.setUltimoError(e.getMessage());
            log.warn("Fallo al ejecutar publicación programada id={} (intento {}): {}", p.getId(), intentos, e.getMessage());
            if (intentos >= 3) {
                p.setEstado("FALLIDA");
                p.setContenidoBytes(null);
                log.error("Publicación programada id={} marcada FALLIDA tras 3 intentos", p.getId());
            }
        }
        publicacionSocialRepository.save(p);
    }

    // Deja p listo para guardar (postIdFacebook, estado, fechaPublicacion) -- el caller decide
    // cuándo hacer el save.
    private void ejecutar(PublicacionSocial p) {
        String resultId;
        if ("facebook".equals(p.getPlataforma())) {
            resultId = ejecutarFacebook(p);
        } else if ("instagram".equals(p.getPlataforma())) {
            resultId = ejecutarInstagram(p);
        } else if ("tiktok".equals(p.getPlataforma())) {
            resultId = tikTokGraphClient.publicarVideo(p.getContenidoBytes(), p.getContentType(), p.getDescripcionPublicada());
        } else {
            throw new ExceptionErrorInesperado("Plataforma desconocida: " + p.getPlataforma());
        }

        p.setPostIdFacebook(resultId);
        p.setEstado("PUBLICADA");
        p.setFechaPublicacion(LocalDateTime.now());
        p.setContenidoBytes(null); // ya no hace falta, no dejar el blob ocupando espacio
        log.info("Publicación ejecutada: plataforma={}, tipo={}, varianteId={}, id={}",
                p.getPlataforma(), p.getTipoPublicacion(), p.getVariante().getId(), resultId);
    }

    private String ejecutarFacebook(PublicacionSocial p) {
        if ("foto".equals(p.getTipoPublicacion())) {
            byte[] bytes = p.getContenidoBytes();
            String contentType = p.getContentType();
            if (bytes == null) {
                ImagenDto imagen = imagenPort.getOne(p.getImagenId());
                if (imagen == null || imagen.getImagen() == null) {
                    throw new ExceptionErrorInesperado(
                            "La variante " + p.getVariante().getId() + " no tiene una imagen disponible para publicar");
                }
                bytes = imagen.getImagen();
                contentType = imagen.getContentType();
            }
            return facebookGraphClient.publicarFoto(bytes, contentType, p.getDescripcionPublicada(), null);
        }
        if ("video".equals(p.getTipoPublicacion())) {
            return facebookGraphClient.publicarVideo(p.getContenidoBytes(), p.getContentType(), p.getDescripcionPublicada(), null);
        }
        if ("reel".equals(p.getTipoPublicacion())) {
            return facebookGraphClient.publicarReel(p.getContenidoBytes(), p.getContentType(), p.getDescripcionPublicada());
        }
        throw new ExceptionErrorInesperado("Tipo de publicación de Facebook desconocido: " + p.getTipoPublicacion());
    }

    private String ejecutarInstagram(PublicacionSocial p) {
        if ("foto".equals(p.getTipoPublicacion())) {
            if (endpointImagenes == null || endpointImagenes.isBlank()) {
                throw new ExceptionErrorInesperado("No se pudo construir la URL pública de la imagen: falta configurar api.imagenes");
            }
            String urlImagen = endpointImagenes + "v1/imagenes/file/" + p.getImagenId();
            return instagramGraphClient.publicarFoto(urlImagen, p.getDescripcionPublicada());
        }
        if ("reel".equals(p.getTipoPublicacion())) {
            return instagramGraphClient.publicarReel(p.getContenidoBytes(), p.getContentType(), p.getDescripcionPublicada());
        }
        throw new ExceptionErrorInesperado("Tipo de publicación de Instagram desconocido: " + p.getTipoPublicacion());
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
}
