package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.meta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PlantillaWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ResultadoEnvio;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.EnvioWhatsappPort;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manda mensajes con la API de WhatsApp (Cloud API) de Meta: {@code POST /{phone-number-id}/messages}.
 * Solo manda: no decide si se puede (eso lo hace {@code PoliticaDeEnvio} antes) y NUNCA reintenta;
 * un rechazo vuelve como {@link ResultadoEnvio.Rechazado} con el código de Meta (ej. 131047).
 *
 * <p>El token y el número se leen de variables de entorno ({@code WHATSAPP_CLOUD_TOKEN},
 * {@code WHATSAPP_PHONE_NUMBER_ID}); nunca van en el código ni en el log.</p>
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class WhatsappCloudClient implements EnvioWhatsappPort {

    private static final Logger log = LoggerFactory.getLogger(WhatsappCloudClient.class);
    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(20);

    @Value("${whatsapp.cloud.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.cloud.token:}")
    private String token;

    // Meta cambia la versión de la Graph API con el tiempo: se confirma contra la documentación oficial
    // vigente al desplegar (ver WHATSAPP_CLOUD_API.md).
    @Value("${whatsapp.cloud.api-version:v26.0}")
    private String apiVersion;

    private final ObjectMapper objectMapper;
    private WebClient webClient;

    public WhatsappCloudClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // OJO: WebClient.builder() nuevo a propósito, NO el WebClient.Builder inyectado por Spring: ese
    // lleva el filtro que agrega nuestro propio JWT a cada petición saliente (para los microservicios
    // internos) y Meta respondería "access token could not be decrypted" (mismo bug que FacebookGraphClient).
    @PostConstruct
    void init() {
        this.webClient = WebClient.builder().baseUrl("https://graph.facebook.com").build();
    }

    @Override
    public ResultadoEnvio enviarTexto(String telefono, String texto) {
        Map<String, Object> cuerpo = base(telefono, "text");
        // preview_url: WhatsApp muestra la vista previa del enlace de la tienda.
        cuerpo.put("text", Map.of("preview_url", true, "body", texto));
        return enviar(cuerpo);
    }

    @Override
    public ResultadoEnvio enviarImagen(String telefono, String urlImagen, String pie) {
        Map<String, Object> imagen = new LinkedHashMap<>();
        imagen.put("link", urlImagen);
        if (pie != null && !pie.isBlank()) {
            imagen.put("caption", pie);
        }
        Map<String, Object> cuerpo = base(telefono, "image");
        cuerpo.put("image", imagen);
        return enviar(cuerpo);
    }

    @Override
    public ResultadoEnvio enviarPlantilla(String telefono, PlantillaWhatsapp plantilla) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", plantilla.nombre());
        template.put("language", Map.of("code", plantilla.idioma()));
        if (!plantilla.parametros().isEmpty()) {
            List<Map<String, String>> parametros = new ArrayList<>();
            for (String valor : plantilla.parametros()) {
                parametros.add(Map.of("type", "text", "text", valor));
            }
            template.put("components", List.of(Map.of("type", "body", "parameters", parametros)));
        }
        Map<String, Object> cuerpo = base(telefono, "template");
        cuerpo.put("template", template);
        return enviar(cuerpo);
    }

    private static Map<String, Object> base(String telefono, String tipo) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("messaging_product", "whatsapp");
        cuerpo.put("recipient_type", "individual");
        cuerpo.put("to", telefono);
        cuerpo.put("type", tipo);
        return cuerpo;
    }

    private ResultadoEnvio enviar(Map<String, Object> cuerpo) {
        if (phoneNumberId.isBlank() || token.isBlank()) {
            return new ResultadoEnvio.Rechazado(null,
                    "WhatsApp no está configurado: faltan WHATSAPP_PHONE_NUMBER_ID o WHATSAPP_CLOUD_TOKEN");
        }
        try {
            JsonNode respuesta = webClient.post()
                    .uri("/{version}/{phoneNumberId}/messages", apiVersion, phoneNumberId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(cuerpo)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(ESPERA_MAXIMA)
                    .block();
            JsonNode id = respuesta == null ? null : respuesta.path("messages").path(0).path("id");
            if (id == null || id.isMissingNode() || id.asText("").isBlank()) {
                return new ResultadoEnvio.Rechazado(null, "Meta respondió sin id de mensaje");
            }
            return new ResultadoEnvio.Enviado(id.asText());
        } catch (WebClientResponseException e) {
            return rechazo(e);
        } catch (Exception e) {
            // Tiempo agotado o sin conexión: no se reintenta (el mensaje podría haber salido, y un
            // segundo envío lo duplicaría). Queda como fallido y una persona decide.
            log.warn("WhatsApp: no se pudo hablar con Meta ({})", e.getClass().getSimpleName());
            return new ResultadoEnvio.Rechazado(null, "No se pudo contactar a Meta (" + e.getClass().getSimpleName() + ")");
        }
    }

    /** Meta contesta {"error":{"code":131047,"message":"...","error_data":{"details":"..."}}}. */
    private ResultadoEnvio rechazo(WebClientResponseException e) {
        Integer codigo = null;
        String motivo = "Meta rechazó el mensaje (HTTP " + e.getStatusCode().value() + ")";
        try {
            JsonNode error = objectMapper.readTree(e.getResponseBodyAsString()).path("error");
            if (error.hasNonNull("code")) {
                codigo = error.path("code").asInt();
            }
            String detalle = error.path("error_data").path("details").asText(null);
            if (detalle == null || detalle.isBlank()) {
                detalle = error.path("message").asText(null);
            }
            if (detalle != null && !detalle.isBlank()) {
                motivo = (codigo != null ? "(" + codigo + ") " : "") + detalle;
            }
        } catch (Exception ignorado) {
            // El cuerpo no era JSON: se queda el motivo genérico con el HTTP.
        }
        return new ResultadoEnvio.Rechazado(codigo, motivo);
    }
}
