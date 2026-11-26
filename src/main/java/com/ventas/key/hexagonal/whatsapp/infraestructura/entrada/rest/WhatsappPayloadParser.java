package com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoDeEntrega;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoMensaje;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeEntrante;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Traduce el payload del webhook de WhatsApp (object "whatsapp_business_account", campo "messages")
 * a mensajes entrantes y avisos de estado. Solo toma los eventos de NUESTRO número
 * ({@code WHATSAPP_PHONE_NUMBER_ID}). Las reacciones y los avisos del sistema se ignoran: contestarlas
 * sería ruido.
 */
@Component
class WhatsappPayloadParser {

    private static final Logger log = LoggerFactory.getLogger(WhatsappPayloadParser.class);

    private static final Set<String> TIPOS_QUE_SE_IGNORAN = Set.of("reaction", "system");

    private final ObjectMapper objectMapper;

    @Value("${whatsapp.cloud.phone-number-id:}")
    private String phoneNumberId;

    WhatsappPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    PayloadWhatsapp parsear(byte[] cuerpo) throws IOException {
        List<MensajeEntrante> mensajes = new ArrayList<>();
        List<EstadoDeEntrega> estados = new ArrayList<>();

        JsonNode raiz = objectMapper.readTree(cuerpo);
        if (!"whatsapp_business_account".equals(raiz.path("object").asText())) {
            log.info("WhatsApp webhook: object '{}' no es de WhatsApp, se ignora", raiz.path("object").asText());
            return new PayloadWhatsapp(mensajes, estados);
        }
        for (JsonNode entry : raiz.path("entry")) {
            for (JsonNode cambio : entry.path("changes")) {
                if (!"messages".equals(cambio.path("field").asText())) {
                    continue;
                }
                JsonNode valor = cambio.path("value");
                if (!esNuestroNumero(valor.path("metadata").path("phone_number_id").asText(""))) {
                    log.info("WhatsApp webhook: evento de otro número, se ignora");
                    continue;
                }
                leerMensajes(valor, mensajes);
                leerEstados(valor, estados);
            }
        }
        return new PayloadWhatsapp(mensajes, estados);
    }

    private boolean esNuestroNumero(String delEvento) {
        // Sin número configurado no se filtra (la configuración incompleta se nota al enviar).
        return phoneNumberId.isBlank() || phoneNumberId.equals(delEvento);
    }

    private void leerMensajes(JsonNode valor, List<MensajeEntrante> salida) {
        Map<String, String> nombres = new HashMap<>();
        for (JsonNode contacto : valor.path("contacts")) {
            nombres.put(contacto.path("wa_id").asText(""), contacto.path("profile").path("name").asText(null));
        }
        for (JsonNode m : valor.path("messages")) {
            String tipo = m.path("type").asText("");
            if (TIPOS_QUE_SE_IGNORAN.contains(tipo)) {
                continue;
            }
            String telefono = m.path("from").asText("");
            salida.add(new MensajeEntrante(m.path("id").asText(null), telefono, nombres.get(telefono),
                    tipo.isEmpty() ? "unsupported" : tipo, textoDe(m, tipo), instante(m.path("timestamp").asText(""))));
        }
    }

    private static String textoDe(JsonNode m, String tipo) {
        return switch (tipo) {
            case "text" -> m.path("text").path("body").asText(null);
            // Respuesta a un botón de una plantilla.
            case "button" -> m.path("button").path("text").asText(null);
            // Respuesta a un botón o a una lista interactiva: se lee como si el cliente la hubiera escrito.
            case "interactive" -> {
                JsonNode interactivo = m.path("interactive");
                String boton = interactivo.path("button_reply").path("title").asText(null);
                yield boton != null ? boton : interactivo.path("list_reply").path("title").asText(null);
            }
            // El pie de foto de una imagen, video o documento.
            case "image", "video", "document" -> m.path(tipo).path("caption").asText(null);
            default -> null;
        };
    }

    private void leerEstados(JsonNode valor, List<EstadoDeEntrega> salida) {
        for (JsonNode s : valor.path("statuses")) {
            EstadoMensaje estado = EstadoMensaje.deMeta(s.path("status").asText("")).orElse(null);
            if (estado == null) {
                continue;
            }
            Integer codigo = null;
            String motivo = null;
            JsonNode error = s.path("errors").path(0);
            if (!error.isMissingNode()) {
                codigo = error.hasNonNull("code") ? error.path("code").asInt() : null;
                String detalle = error.path("error_data").path("details").asText(null);
                if (detalle == null || detalle.isBlank()) {
                    detalle = error.path("message").asText(error.path("title").asText(""));
                }
                motivo = (codigo != null ? "(" + codigo + ") " : "") + detalle;
            }
            salida.add(new EstadoDeEntrega(s.path("id").asText(null), estado, codigo, motivo,
                    instante(s.path("timestamp").asText(""))));
        }
    }

    /** Meta manda los segundos desde 1970 como texto. */
    private static Instant instante(String segundos) {
        try {
            return Instant.ofEpochSecond(Long.parseLong(segundos));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
