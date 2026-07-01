package com.ventas.key.mis.productos.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class WhatsappService {

    @Value("${whatsapp.proveedor:ninguno}")
    private String proveedor;

    @Value("${whatsapp.callmebot-apikey:}")
    private String callmebotApiKey;

    @Value("${whatsapp.country-code:52}")
    private String countryCode;

    private final WebClient webClient = WebClient.create();

    /**
     * Envía un mensaje de WhatsApp al número indicado.
     * @param telefono Número del cliente (con o sin código de país).
     * @param texto    Texto plano del ticket.
     * @return true si el envío fue exitoso.
     */
    public boolean enviarMensaje(String telefono, String texto) {
        if (telefono == null || telefono.isBlank() || texto == null || texto.isBlank()) {
            log.warn("WhatsappService: teléfono o texto vacío, mensaje no enviado");
            return false;
        }

        String numero = normalizarTelefono(telefono);

        return switch (proveedor.toLowerCase()) {
            case "callmebot" -> enviarCallMeBot(numero, texto);
            default -> {
                log.info("WhatsappService: proveedor '{}' no configurado, mensaje omitido", proveedor);
                yield false;
            }
        };
    }

    private boolean enviarCallMeBot(String telefono, String texto) {
        if (callmebotApiKey == null || callmebotApiKey.isBlank()) {
            log.warn("WhatsappService CallMeBot: API key no configurada");
            return false;
        }
        try {
            String textoEncoded = URLEncoder.encode(texto, StandardCharsets.UTF_8);
            String url = String.format(
                "https://api.callmebot.com/whatsapp.php?phone=%s&text=%s&apikey=%s",
                telefono, textoEncoded, callmebotApiKey);

            String respuesta = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            boolean exito = respuesta != null && respuesta.contains("Message queued");
            if (exito) {
                log.info("WhatsApp enviado a: {}", telefono);
            } else {
                log.warn("WhatsApp CallMeBot respuesta inesperada para {}: {}", telefono, respuesta);
            }
            return exito;
        } catch (Exception e) {
            log.error("Error enviando WhatsApp a {}: {}", telefono, e.getMessage());
            return false;
        }
    }

    /** Asegura formato internacional sin '+' (ej. 7221234567 → 527221234567) */
    private String normalizarTelefono(String telefono) {
        String solo = telefono.replaceAll("[^0-9]", "");
        if (solo.startsWith(countryCode) && solo.length() > countryCode.length() + 8) {
            return solo;
        }
        return countryCode + solo;
    }
}
