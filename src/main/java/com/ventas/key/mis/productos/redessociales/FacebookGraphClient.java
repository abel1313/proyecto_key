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

    public FacebookGraphClient(WebClient.Builder builder) {
        this.builder = builder;
    }

    @PostConstruct
    void init() {
        this.webClient = builder.baseUrl("https://graph.facebook.com").build();
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

    private String extensionDe(String contentType) {
        if (contentType == null) return "jpg";
        if (contentType.contains("png")) return "png";
        if (contentType.contains("webp")) return "webp";
        return "jpg";
    }
}
