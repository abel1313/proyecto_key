package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Publica fotos en la cuenta de Instagram profesional vinculada a la pagina, via Graph API
 * ({@code /{ig-user-id}/media} + {@code /{ig-user-id}/media_publish}). Reusa el mismo Page
 * Access Token que Facebook -- una cuenta de Instagram Business/Creator solo puede publicarse
 * a traves de la pagina de Facebook a la que esta vinculada, no tiene credenciales propias.
 *
 * Requiere que la cuenta de Instagram ya este vinculada a la pagina en Meta Business Suite y
 * que la app tenga los permisos instagram_basic/instagram_content_publish aprobados -- sin eso
 * la Graph API responde error, no hay forma de evitarlo desde el codigo.
 *
 * A diferencia de Facebook, la Content Publishing API de Instagram SIEMPRE publica de inmediato
 * -- no soporta scheduled_publish_time. Programar quedaria pendiente de un scheduler propio si
 * algun dia se pide.
 */
@Service
@Slf4j
public class InstagramGraphClient {

    @Value("${instagram.account-id:}")
    private String igUserId;

    @Value("${facebook.page-access-token:}")
    private String pageAccessToken;

    @Value("${facebook.api-version:v21.0}")
    private String apiVersion;

    private final WebClient.Builder builder;
    private WebClient webClient;
    // rupload.facebook.com es el host de subida binaria de Meta, distinto del de la Graph API
    // normal -- mismo mecanismo que usa Facebook para /video_reels.
    private WebClient uploadClient;

    public InstagramGraphClient(WebClient.Builder builder) {
        this.builder = builder;
    }

    @PostConstruct
    void init() {
        this.webClient = builder.baseUrl("https://graph.facebook.com").build();
        this.uploadClient = builder.clone().baseUrl("https://rupload.facebook.com").build();
    }

    public String publicarFoto(String imagenUrlPublica, String caption) {
        if (igUserId.isBlank() || pageAccessToken.isBlank()) {
            throw new ExceptionErrorInesperado("Instagram no esta configurado: falta INSTAGRAM_ACCOUNT_ID o "
                    + "FACEBOOK_PAGE_ACCESS_TOKEN (la cuenta de Instagram publica a traves de la misma pagina)");
        }
        if (imagenUrlPublica == null || imagenUrlPublica.isBlank()) {
            throw new ExceptionErrorInesperado("No hay una URL publica de imagen disponible para publicar");
        }

        String creationId = crearContenedor(imagenUrlPublica, caption);
        String mediaId = publicarContenedor(creationId);
        log.info("Publicado en Instagram, id={}", mediaId);
        return mediaId;
    }

    private String crearContenedor(String imagenUrlPublica, String caption) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("image_url", imagenUrlPublica);
        form.add("caption", caption);
        form.add("access_token", pageAccessToken);

        Map<?, ?> response = webClient.post()
                .uri("/{version}/{igUserId}/media", apiVersion, igUserId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(err -> Mono.error(new ExceptionErrorInesperado(
                                "Instagram rechazo la imagen: " + err))))
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        if (response == null || response.get("id") == null) {
            throw new ExceptionErrorInesperado("Instagram no devolvio id de contenedor al crear la publicacion: " + response);
        }
        return String.valueOf(response.get("id"));
    }

    private String publicarContenedor(String creationId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("creation_id", creationId);
        form.add("access_token", pageAccessToken);

        Map<?, ?> response = webClient.post()
                .uri("/{version}/{igUserId}/media_publish", apiVersion, igUserId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(err -> Mono.error(new ExceptionErrorInesperado(
                                "Instagram rechazo publicar el contenedor: " + err))))
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        if (response == null || response.get("id") == null) {
            throw new ExceptionErrorInesperado("Instagram no devolvio id de publicacion: " + response);
        }
        return String.valueOf(response.get("id"));
    }

    // Reel -- a diferencia de la foto, el video no se manda por URL: se sube directo con "subida
    // reanudable" (mismo mecanismo que usa Facebook para /video_reels), asi el admin no necesita
    // que el archivo exista en ningun lado publico de antemano. 4 pasos: crear el contenedor en
    // modo resumable, subir los bytes a rupload.facebook.com, esperar a que Meta termine de
    // procesar el video (puede tardar), y publicar el contenedor -- este ultimo paso reusa
    // publicarContenedor(), es identico al de la foto.
    public String publicarReel(byte[] video, String contentType, String caption) {
        if (igUserId.isBlank() || pageAccessToken.isBlank()) {
            throw new ExceptionErrorInesperado("Instagram no esta configurado: falta INSTAGRAM_ACCOUNT_ID o "
                    + "FACEBOOK_PAGE_ACCESS_TOKEN (la cuenta de Instagram publica a traves de la misma pagina)");
        }
        if (video == null || video.length == 0) {
            throw new ExceptionErrorInesperado("No hay video disponible para publicar");
        }

        String containerId = crearContenedorReelResumable(caption);
        subirVideoResumable(containerId, video, contentType);
        esperarProcesado(containerId);
        String mediaId = publicarContenedor(containerId);
        log.info("Reel publicado en Instagram, id={}", mediaId);
        return mediaId;
    }

    private String crearContenedorReelResumable(String caption) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("media_type", "REELS");
        form.add("upload_type", "resumable");
        form.add("caption", caption);
        form.add("access_token", pageAccessToken);

        Map<?, ?> response = webClient.post()
                .uri("/{version}/{igUserId}/media", apiVersion, igUserId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(err -> Mono.error(new ExceptionErrorInesperado(
                                "Instagram rechazo crear el contenedor del Reel: " + err))))
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        if (response == null || response.get("id") == null) {
            throw new ExceptionErrorInesperado("Instagram no devolvio id de contenedor para el Reel: " + response);
        }
        return String.valueOf(response.get("id"));
    }

    private void subirVideoResumable(String containerId, byte[] video, String contentType) {
        Map<?, ?> response = uploadClient.post()
                .uri("/ig-api-upload/{version}/{containerId}", apiVersion, containerId)
                .header("Authorization", "OAuth " + pageAccessToken)
                .header("offset", "0")
                .header("file_size", String.valueOf(video.length))
                .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                .body(BodyInserters.fromResource(new ByteArrayResource(video)))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(err -> Mono.error(new ExceptionErrorInesperado(
                                "Instagram rechazo la subida del video del Reel: " + err))))
                .bodyToMono(Map.class)
                .timeout(Duration.ofMinutes(5))
                .block();

        if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
            throw new ExceptionErrorInesperado("Instagram no confirmo la subida del video del Reel: " + response);
        }
    }

    // Meta procesa el video de forma asincrona despues de subirlo -- sin esperar a que termine,
    // media_publish falla porque el contenedor todavia no tiene un video utilizable. Se consulta
    // cada 3 segundos hasta un maximo de 60 intentos (~3 minutos); si tarda mas que eso se avisa
    // con un mensaje claro en vez de dejar la peticion colgada indefinidamente.
    private void esperarProcesado(String containerId) {
        for (int intento = 0; intento < 60; intento++) {
            Map<?, ?> estado = webClient.get()
                    .uri("/{version}/{containerId}?fields=status_code", apiVersion, containerId)
                    .header("Authorization", "Bearer " + pageAccessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(err -> Mono.error(new ExceptionErrorInesperado(
                                    "Instagram rechazo consultar el estado del Reel: " + err))))
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            String statusCode = estado != null && estado.get("status_code") != null
                    ? String.valueOf(estado.get("status_code")) : null;

            if ("FINISHED".equals(statusCode)) {
                return;
            }
            if ("ERROR".equals(statusCode) || "EXPIRED".equals(statusCode)) {
                throw new ExceptionErrorInesperado("Instagram no pudo procesar el video del Reel (status: " + statusCode + ")");
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ExceptionErrorInesperado("Se interrumpio la espera del procesamiento del Reel");
            }
        }
        throw new ExceptionErrorInesperado("Instagram sigue procesando el video del Reel despues de 3 minutos -- "
                + "intenta publicarlo de nuevo en unos minutos");
    }
}
