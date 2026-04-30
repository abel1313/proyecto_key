package com.ventas.key.mis.productos.chatbot;

import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatbotService {

    @Value("${openai.api-key}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private final IVarianteRepository varianteRepository;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    public String chat(ChatbotRequest request) {
        String contexto = obtenerContextoVariantes();

        String sistemPrompt = """
                Eres el asistente virtual de Novedades Jade, una tienda en línea mexicana.
                Responde siempre en español, de manera amable y breve.
                Si el cliente pregunta por un producto que no está en el catálogo, díselo con amabilidad.
                No inventes precios ni productos que no estén en la lista.

                POLÍTICAS DE LA TIENDA:
                - Entregas en: Luvianos, el Estanco, Caja de Agua, Acatitlán, Tejupilco (Estado de México) y Zacazonapan.
                - Pagos: tarjeta de crédito, débito, transferencia y efectivo.

                MANEJO DE MENSAJES NO COMPRENSIBLES O FUERA DE CONTEXTO:
                - Si el mensaje NO tiene relación con productos, compras, precios, envíos o la tienda,
                  o si simplemente no entiendes lo que el cliente quiere decir, haz exactamente esto:
                  1. Indica en una línea breve que no pudiste entender su mensaje.
                  2. Menciona que puede contactarnos por Facebook o WhatsApp para recibir ayuda.
                  3. Da una despedida corta y amable (máximo 2 líneas en total).
                  4. Menciona que si se registra en la plataforma podrá ver más opciones de contacto
                     y hacer seguimiento de sus pedidos.
                  5. Escribe exactamente al final de tu respuesta, sin espacios extra: ##FAREWELL##
                  6. NO sigas preguntando ni intentando ayudar con ese mensaje.
                     Una sola respuesta de despedida, sin vueltas.

                CATÁLOGO ACTUAL (variantes disponibles con stock):
                """ + contexto;

        List<Map<String, String>> mensajes = new ArrayList<>();
        mensajes.add(Map.of("role", "system", "content", sistemPrompt));

        if (request.getHistorial() != null) {
            for (ChatbotRequest.MensajeHistorial h : request.getHistorial()) {
                if ("user".equals(h.getRol()) || "assistant".equals(h.getRol())) {
                    mensajes.add(Map.of("role", h.getRol(), "content", h.getContenido()));
                }
            }
        }

        mensajes.add(Map.of("role", "user", "content", request.getMensaje()));

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", mensajes,
                "max_tokens", 500,
                "temperature", 0.7
        );

        Map response = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new RuntimeException("Sin respuesta de OpenAI");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private String obtenerContextoVariantes() {
        try {
            List<Variantes> variantes = varianteRepository
                    .findByStockGreaterThanAndProductoHabilitado(0, '1', PageRequest.of(0, 100))
                    .getContent();

            if (variantes.isEmpty()) {
                return "No hay productos disponibles en este momento.";
            }

            StringBuilder sb = new StringBuilder();
            for (Variantes v : variantes) {
                sb.append("- ").append(v.getProducto().getNombre());

                if (v.getMarca() != null && !v.getMarca().isBlank()) {
                    sb.append(" (").append(v.getMarca()).append(")");
                }
                if (v.getTalla() != null && !v.getTalla().isBlank()) {
                    sb.append(", talla: ").append(v.getTalla());
                }
                if (v.getColor() != null && !v.getColor().isBlank()) {
                    sb.append(", color: ").append(v.getColor());
                }
                if (v.getPresentacion() != null && !v.getPresentacion().isBlank()) {
                    sb.append(", presentación: ").append(v.getPresentacion());
                }

                Double precioRebaja = v.getProducto().getPrecioRebaja();
                Double precioVenta = v.getProducto().getPrecioVenta();
//                if (precioRebaja != null && precioRebaja > 0) {
//                    sb.append(", precio: $").append(String.format("%.0f", precioRebaja))
//                      .append(" MXN (en oferta, antes $").append(String.format("%.0f", precioVenta)).append(")");
//                } else
//
                if (precioVenta != null) {
                    sb.append(", precio: $").append(String.format("%.0f", precioVenta)).append(" MXN");
                }

                sb.append(", stock: ").append(v.getStock()).append(" pzas");

                if (v.getDescripcion() != null && !v.getDescripcion().isBlank()) {
                    sb.append(". ").append(v.getDescripcion());
                }
                sb.append("\n");
            }
            return sb.toString();

        } catch (Exception e) {
            log.error("Error consultando variantes para chatbot", e);
            return "Catálogo temporalmente no disponible.";
        }
    }
}