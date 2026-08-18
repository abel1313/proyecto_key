package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Publica fotos en la Página de Facebook vía Graph API (POST /{page-id}/photos).
 * Requiere que la app tenga permisos pages_manage_posts aprobados por Meta para
 * publicar en una página que no sea de prueba/admin de la app.
 */
@Service
@Slf4j
public class FacebookGraphClient {

    @Value("${facebook.page-id:}")
    private String pageId;

    @Value("${facebook.page-access-token:}")
    private String pageAccessToken;

    @Value("${facebook.api-version:v21.0}")
    private String apiVersion;

    private final WebClient.Builder builder;
    private WebClient webClient;
    // Host de subida binaria de Meta para /video_reels -- la URL exacta la devuelve el paso
    // "start" (uploadUrl), pero siempre cae en este host, asi que se deja armado un WebClient
    // aparte igual que en InstagramGraphClient.
    private WebClient uploadClient;

    public FacebookGraphClient(WebClient.Builder builder) {
        this.builder = builder;
    }

    @PostConstruct
    void init() {
        this.webClient = builder.baseUrl("https://graph.facebook.com").build();
        this.uploadClient = builder.clone().build();
    }

    public String publicarFoto(byte[] imagen, String contentType, String mensaje, Long scheduledEpochSeconds) {
        if (pageId.isBlank() || pageAccessToken.isBlank()) {
            throw new ExceptionErrorInesperado("Facebook no está configurado: faltan FACEBOOK_PAGE_ID o FACEBOOK_PAGE_ACCESS_TOKEN");
        }
        if (imagen == null || imagen.length == 0) {
            throw new ExceptionErrorInesperado("No hay imagen disponible para publicar");
        }

        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("source", new ByteArrayResource(imagen) {
                    @Override
                    public String getFilename() {
                        return "publicacion." + extensionDe(contentType);
                    }
                })
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "image/jpeg"));
        body.part("message", mensaje);
        body.part("access_token", pageAccessToken);
        if (scheduledEpochSeconds != null) {
            body.part("published", "false");
            body.part("scheduled_publish_time", String.valueOf(scheduledEpochSeconds));
        }

        Map<?, ?> response = webClient.post()
                .uri("/{version}/{pageId}/photos", apiVersion, pageId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body.build()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(err -> Mono.error(new ExceptionErrorInesperado(
                                "Facebook rechazó la publicación: " + err))))
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        if (response == null) {
            throw new ExceptionErrorInesperado("Facebook no devolvió respuesta al publicar");
        }

        Object postId = response.containsKey("post_id") ? response.get("post_id") : response.get("id");
        if (postId == null) {
            throw new ExceptionErrorInesperado("Facebook respondió sin id de publicación: " + response);
        }
        log.info("Publicado en Facebook, id={}", postId);
        return String.valueOf(postId);
    }

    /**
     * Publica un video en el feed normal de la página (POST /{page-id}/videos). A diferencia de
     * /photos, el caption va en "description" (no "message") y la respuesta no trae post_id,
     * solo el id del video. Timeout más largo porque los archivos son mucho más pesados.
     */
    public String publicarVideo(byte[] video, String contentType, String descripcion, Long scheduledEpochSeconds) {
        if (pageId.isBlank() || pageAccessToken.isBlank()) {
            throw new ExceptionErrorInesperado("Facebook no está configurado: faltan FACEBOOK_PAGE_ID o FACEBOOK_PAGE_ACCESS_TOKEN");
        }
        if (video == null || video.length == 0) {
            throw new ExceptionErrorInesperado("No hay video disponible para publicar");
        }

        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("source", new ByteArrayResource(video) {
                    @Override
                    public String getFilename() {
                        return "publicacion." + extensionVideoDe(contentType);
                    }
                })
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "video/mp4"));
        body.part("description", descripcion);
        body.part("access_token", pageAccessToken);
        if (scheduledEpochSeconds != null) {
            body.part("published", "false");
            body.part("scheduled_publish_time", String.valueOf(scheduledEpochSeconds));
        }

        Map<?, ?> response = webClient.post()
                .uri("/{version}/{pageId}/videos", apiVersion, pageId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body.build()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(err -> Mono.error(new ExceptionErrorInesperado(
                                "Facebook rechazó el video: " + err))))
                .bodyToMono(Map.class)
                .timeout(Duration.ofMinutes(5))
                .block();

        if (response == null) {
            throw new ExceptionErrorInesperado("Facebook no devolvió respuesta al publicar el video");
        }

        Object videoId = response.get("id");
        if (videoId == null) {
            throw new ExceptionErrorInesperado("Facebook respondió sin id de video: " + response);
        }
        log.info("Video publicado en Facebook, id={}", videoId);
        return String.valueOf(videoId);
    }

    // Reel de la pagina (POST /{page-id}/video_reels) -- flujo de "subida reanudable" en 3 pasos,
    // distinto al de /videos (feed normal). El video se sube tal cual, sin validar
    // duracion/proporcion, igual que el resto de publicaciones de este cliente.
    public String publicarReel(byte[] video, String contentType, String descripcion) {
        if (pageId.isBlank() || pageAccessToken.isBlank()) {
            throw new ExceptionErrorInesperado("Facebook no está configurado: faltan FACEBOOK_PAGE_ID o FACEBOOK_PAGE_ACCESS_TOKEN");
        }
        if (video == null || video.length == 0) {
            throw new ExceptionErrorInesperado("No hay video disponible para publicar");
        }

        Map<String, String> inicio = iniciarReel();
        String videoId = inicio.get("video_id");
        String uploadUrl = inicio.get("upload_url");
        if (videoId == null || uploadUrl == null) {
            throw new ExceptionErrorInesperado("Facebook no devolvió video_id/upload_url al iniciar el Reel: " + inicio);
        }

        subirVideoReel(uploadUrl, video, contentType);
        finalizarReel(videoId, descripcion);

        log.info("Reel publicado en Facebook, video_id={}", videoId);
        return videoId;
    }

    private Map<String, String> iniciarReel() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("upload_phase", "start");
        form.add("access_token", pageAccessToken);

        Map<?, ?> response = webClient.post()
                .uri("/{version}/{pageId}/video_reels", apiVersion, pageId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(err -> Mono.error(new ExceptionErrorInesperado(
                                "Facebook rechazó iniciar el Reel: " + err))))
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        if (response == null) {
            throw new ExceptionErrorInesperado("Facebook no devolvió respuesta al iniciar el Reel");
        }
        Map<String, String> resultado = new java.util.HashMap<>();
        Object videoId = response.get("video_id");
        Object uploadUrl = response.get("upload_url");
        resultado.put("video_id", videoId != null ? String.valueOf(videoId) : null);
        resultado.put("upload_url", uploadUrl != null ? String.valueOf(uploadUrl) : null);
        return resultado;
    }

    private void subirVideoReel(String uploadUrl, byte[] video, String contentType) {
        Map<?, ?> response = uploadClient.post()
                .uri(java.net.URI.create(uploadUrl))
                .header("Authorization", "OAuth " + pageAccessToken)
                .header("offset", "0")
                .header("file_size", String.valueOf(video.length))
                .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                .body(BodyInserters.fromResource(new ByteArrayResource(video)))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(err -> Mono.error(new ExceptionErrorInesperado(
                                "Facebook rechazó la subida del video del Reel: " + err))))
                .bodyToMono(Map.class)
                .timeout(Duration.ofMinutes(5))
                .block();

        if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
            throw new ExceptionErrorInesperado("Facebook no confirmó la subida del video del Reel: " + response);
        }
    }

    private void finalizarReel(String videoId, String descripcion) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("upload_phase", "finish");
        form.add("video_id", videoId);
        form.add("video_state", "PUBLISHED");
        form.add("description", descripcion);
        form.add("access_token", pageAccessToken);

        Map<?, ?> response = webClient.post()
                .uri("/{version}/{pageId}/video_reels", apiVersion, pageId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(err -> Mono.error(new ExceptionErrorInesperado(
                                "Facebook rechazó publicar el Reel: " + err))))
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
            throw new ExceptionErrorInesperado("Facebook no confirmó la publicación del Reel: " + response);
        }
    }

    private String extensionDe(String contentType) {
        if (contentType == null) return "jpg";
        if (contentType.contains("png")) return "png";
        if (contentType.contains("webp")) return "webp";
        return "jpg";
    }

    private String extensionVideoDe(String contentType) {
        if (contentType == null) return "mp4";
        if (contentType.contains("quicktime") || contentType.contains("mov")) return "mov";
        if (contentType.contains("webm")) return "webm";
        return "mp4";
    }
}
