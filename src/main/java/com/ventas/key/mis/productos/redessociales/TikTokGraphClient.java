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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Publica videos en la cuenta de TikTok del negocio via Content Posting API (Direct Post).
 * A diferencia de Facebook/Instagram, el token expira cada ~24h y el refresh token rota en cada
 * uso -- este cliente maneja todo eso solo, leyendo/actualizando {@link TikTokToken} (fila unica,
 * id=1) antes de cada publicacion. Ver TIKTOK_SETUP.md para el tramite completo con Meta... con
 * TikTok, y ver el comentario de {@link #publicarVideo} sobre la restriccion de Sandbox.
 *
 * Escrito siguiendo la documentacion oficial de TikTok for Developers (Content Posting API v2),
 * sin verificar contra la API real -- primera vez que este proyecto integra TikTok. Si algun
 * campo/endpoint respondiera distinto a lo esperado, el mensaje de error de TikTok viaja tal cual
 * en la excepcion.
 */
@Service
@Slf4j
public class TikTokGraphClient {

    @Value("${tiktok.client-key:}")
    private String clientKey;

    @Value("${tiktok.client-secret:}")
    private String clientSecret;

    private final WebClient.Builder builder;
    private final ITikTokTokenRepository tokenRepository;
    private WebClient webClient;

    public TikTokGraphClient(WebClient.Builder builder, ITikTokTokenRepository tokenRepository) {
        this.builder = builder;
        this.tokenRepository = tokenRepository;
    }

    @PostConstruct
    void init() {
        this.webClient = builder.clone().baseUrl("https://open.tiktokapis.com").build();
    }

    // Paso unico, manual: despues de que el admin autoriza la cuenta en el navegador
    // (TIKTOK_SETUP.md paso 4) y llega el "code" en la URL de retorno, se manda aqui para
    // cambiarlo por el primer access_token/refresh_token y guardarlos. De ahi en adelante todo
    // se refresca solo, esto no se vuelve a llamar salvo que se revoque el acceso.
    public void autorizarConCode(String code, String redirectUri) {
        if (clientKey.isBlank() || clientSecret.isBlank()) {
            throw new ExceptionErrorInesperado("TikTok no está configurado: faltan TIKTOK_CLIENT_KEY o TIKTOK_CLIENT_SECRET");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_key", clientKey);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", redirectUri);

        Map<?, ?> response = intercambiarToken(form);
        guardarToken(response);
        log.info("TikTok autorizado por primera vez, open_id={}", response.get("open_id"));
    }

    private void refrescarToken(TikTokToken actual) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_key", clientKey);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", actual.getRefreshToken());

        Map<?, ?> response = intercambiarToken(form);
        guardarToken(response);
        log.info("Token de TikTok refrescado, open_id={}", response.get("open_id"));
    }

    private Map<?, ?> intercambiarToken(MultiValueMap<String, String> form) {
        Map<?, ?> response = webClient.post()
                .uri("/v2/oauth/token/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(err -> Mono.error(new ExceptionErrorInesperado(
                                "TikTok rechazó el intercambio de token: " + err))))
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        if (response == null || response.get("access_token") == null) {
            throw new ExceptionErrorInesperado("TikTok no devolvió access_token: " + response);
        }
        return response;
    }

    private void guardarToken(Map<?, ?> response) {
        TikTokToken token = tokenRepository.findById(1).orElseGet(TikTokToken::new);
        token.setId(1);
        token.setAccessToken(String.valueOf(response.get("access_token")));
        token.setRefreshToken(String.valueOf(response.get("refresh_token")));
        token.setOpenId(String.valueOf(response.get("open_id")));
        Object expiresInRaw = response.get("expires_in");
        Object refreshExpiresInRaw = response.get("refresh_expires_in");
        long expiresIn = expiresInRaw != null ? ((Number) expiresInRaw).longValue() : 86400L;
        long refreshExpiresIn = refreshExpiresInRaw != null ? ((Number) refreshExpiresInRaw).longValue() : 31536000L;
        LocalDateTime ahora = LocalDateTime.now();
        token.setAccessTokenExpiraEn(ahora.plusSeconds(expiresIn));
        token.setRefreshTokenExpiraEn(ahora.plusSeconds(refreshExpiresIn));
        token.setActualizadoEn(ahora);
        tokenRepository.save(token);
    }

    // Margen de seguridad de 5 minutos antes de la expiracion real, para no arriesgarse a que el
    // token venza a medio proceso de publicacion (crear contenedor con un token, subir el video
    // con otro ya vencido).
    private String obtenerAccessTokenValido() {
        TikTokToken token = tokenRepository.findById(1)
                .orElseThrow(() -> new ExceptionErrorInesperado(
                        "TikTok no está autorizado todavía -- hace falta hacer el login OAuth primero "
                                + "(POST /v1/redes-sociales/tiktok/autorizar, ver TIKTOK_SETUP.md)"));
        if (token.getAccessTokenExpiraEn() == null || LocalDateTime.now().isAfter(token.getAccessTokenExpiraEn().minusMinutes(5))) {
            refrescarToken(token);
            token = tokenRepository.findById(1).orElseThrow();
        }
        return token.getAccessToken();
    }

    /**
     * Publica un video via Direct Post. Sin validar duracion/proporcion/peso, mismo criterio que
     * el resto de publicaciones de este proyecto.
     *
     * ⚠️ Mientras la app no pase la auditoria de TikTok ("Content Posting API audit"), esta
     * llamada SOLO funciona con las cuentas agregadas como Target User en el Sandbox del portal
     * -- para cualquier otra cuenta, TikTok la rechaza. Ademas, el video sale forzado a
     * visibilidad SELF_ONLY (privado) sin importar el privacy_level pedido -- restriccion de la
     * plataforma, no de este codigo. Ver TIKTOK_SETUP.md.
     */
    public String publicarVideo(byte[] video, String contentType, String caption) {
        if (video == null || video.length == 0) {
            throw new ExceptionErrorInesperado("No hay video disponible para publicar");
        }
        String accessToken = obtenerAccessTokenValido();

        String privacyLevel = consultarPrivacyLevelDisponible(accessToken);
        String publishId = iniciarPublicacion(accessToken, video.length, caption, privacyLevel);
        subirVideo(publishId, accessToken, video, contentType);
        esperarPublicado(publishId, accessToken);

        log.info("Video publicado en TikTok, publish_id={}", publishId);
        return publishId;
    }

    // TikTok exige consultar que niveles de privacidad puede usar esta cuenta antes de publicar
    // -- mientras la app no este auditada, la unica opcion real es SELF_ONLY (privado). Se toma
    // ese si esta disponible; si no, el primero que la cuenta permita.
    @SuppressWarnings("unchecked")
    private String consultarPrivacyLevelDisponible(String accessToken) {
        Map<?, ?> response = webClient.post()
                .uri("/v2/post/publish/creator_info/query/")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(err -> Mono.error(new ExceptionErrorInesperado(
                                "TikTok rechazó consultar la info del creador: " + err))))
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(20))
                .block();

        Map<?, ?> data = response != null ? (Map<?, ?>) response.get("data") : null;
        List<String> opciones = data != null ? (List<String>) data.get("privacy_level_options") : null;
        if (opciones == null || opciones.isEmpty()) {
            throw new ExceptionErrorInesperado("TikTok no devolvió privacy_level_options para esta cuenta: " + response);
        }
        return opciones.contains("SELF_ONLY") ? "SELF_ONLY" : opciones.get(0);
    }

    @SuppressWarnings("unchecked")
    private String iniciarPublicacion(String accessToken, int videoSize, String caption, String privacyLevel) {
        Map<String, Object> postInfo = Map.of(
                "title", caption != null ? caption : "",
                "privacy_level", privacyLevel,
                "disable_duet", false,
                "disable_comment", false,
                "disable_stitch", false
        );
        Map<String, Object> sourceInfo = Map.of(
                "source", "FILE_UPLOAD",
                "video_size", videoSize,
                "chunk_size", videoSize,
                "total_chunk_count", 1
        );
        Map<String, Object> body = Map.of("post_info", postInfo, "source_info", sourceInfo);

        Map<?, ?> response = webClient.post()
                .uri("/v2/post/publish/video/init/")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(err -> Mono.error(new ExceptionErrorInesperado(
                                "TikTok rechazó iniciar la publicación: " + err))))
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        Map<?, ?> data = response != null ? (Map<?, ?>) response.get("data") : null;
        String publishId = data != null && data.get("publish_id") != null ? String.valueOf(data.get("publish_id")) : null;
        String uploadUrl = data != null && data.get("upload_url") != null ? String.valueOf(data.get("upload_url")) : null;
        if (publishId == null || uploadUrl == null) {
            throw new ExceptionErrorInesperado("TikTok no devolvió publish_id/upload_url al iniciar: " + response);
        }
        this.ultimaUploadUrl = uploadUrl;
        return publishId;
    }

    // Guardado temporal entre iniciarPublicacion() y subirVideo() -- ambos siempre se llaman en
    // secuencia dentro de publicarVideo(), nunca en paralelo, asi que un campo de instancia
    // alcanza sin necesitar pasar la URL como parametro extra por toda la cadena.
    private String ultimaUploadUrl;

    private void subirVideo(String publishId, String accessToken, byte[] video, String contentType) {
        webClient.put()
                .uri(java.net.URI.create(ultimaUploadUrl))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Range", "bytes 0-" + (video.length - 1) + "/" + video.length)
                .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.valueOf("video/mp4"))
                .body(BodyInserters.fromResource(new ByteArrayResource(video)))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(err -> Mono.error(new ExceptionErrorInesperado(
                                "TikTok rechazó la subida del video (publish_id=" + publishId + "): " + err))))
                .toBodilessEntity()
                .timeout(Duration.ofMinutes(5))
                .block();
    }

    // Igual que Instagram: TikTok procesa el video en segundo plano despues de subirlo. Se
    // consulta cada 3 segundos hasta 60 intentos (~3 min) antes de avisar que sigue procesando.
    @SuppressWarnings("unchecked")
    private void esperarPublicado(String publishId, String accessToken) {
        for (int intento = 0; intento < 60; intento++) {
            Map<?, ?> response = webClient.post()
                    .uri("/v2/post/publish/status/fetch/")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(Map.of("publish_id", publishId)))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(err -> Mono.error(new ExceptionErrorInesperado(
                                    "TikTok rechazó consultar el estado de la publicación: " + err))))
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            Map<?, ?> data = response != null ? (Map<?, ?>) response.get("data") : null;
            String status = data != null && data.get("status") != null ? String.valueOf(data.get("status")) : null;

            if ("PUBLISH_COMPLETE".equals(status) || "SEND_TO_USER_INBOX".equals(status)) {
                return;
            }
            if ("FAILED".equals(status)) {
                throw new ExceptionErrorInesperado("TikTok no pudo publicar el video (publish_id=" + publishId
                        + "): " + (data != null ? data.get("fail_reason") : response));
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ExceptionErrorInesperado("Se interrumpió la espera del procesamiento en TikTok");
            }
        }
        throw new ExceptionErrorInesperado("TikTok sigue procesando el video (publish_id=" + publishId
                + ") después de 3 minutos -- puede que ya se haya publicado, revisa en la app antes de reintentar");
    }
}
