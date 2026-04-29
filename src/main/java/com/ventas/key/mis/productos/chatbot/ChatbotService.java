package com.ventas.key.mis.productos.chatbot;

import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.repository.IProductosRepository;
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

    private final IProductosRepository productosRepository;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    public String chat(ChatbotRequest request) {
        String contextoProductos = obtenerContextoProductos();

        String sistemPrompt = """
                Eres el asistente virtual de Novedades Jade, una tienda en línea mexicana.
                Responde siempre en español, de manera amable y breve.
                Si el cliente pregunta por un producto que no está en el catálogo, díselo con amabilidad.
                No inventes precios ni productos que no estén en la lista.

                POLÍTICAS DE LA TIENDA:
                - Envíos a toda la república en 3-5 días hábiles
                - Devoluciones hasta 7 días después de recibir el pedido
                - Pagos: tarjeta, transferencia y MercadoPago
                - Envío gratis en compras mayores a $600 MXN

                CATÁLOGO ACTUAL (productos disponibles con stock):
                """ + contextoProductos;

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

    private String obtenerContextoProductos() {
        try {
            // Trae hasta 80 productos habilitados con stock disponible
            List<Producto> productos = productosRepository
                    .findDistinctByStockGreaterThanAndHabilitado(0, '1', PageRequest.of(0, 80))
                    .getContent();

            if (productos.isEmpty()) {
                return "No hay productos disponibles en este momento.";
            }

            StringBuilder sb = new StringBuilder();
            for (Producto p : productos) {
                sb.append("- ").append(p.getNombre());
                if (p.getMarca() != null && !p.getMarca().isBlank()) {
                    sb.append(" (").append(p.getMarca()).append(")");
                }
                if (p.getColor() != null && !p.getColor().isBlank()) {
                    sb.append(", color: ").append(p.getColor());
                }
                if (p.getPrecioRebaja() != null && p.getPrecioRebaja() > 0) {
                    sb.append(", precio: $").append(String.format("%.0f", p.getPrecioRebaja()))
                      .append(" MXN (en oferta, antes $").append(String.format("%.0f", p.getPrecioVenta())).append(")");
                } else if (p.getPrecioVenta() != null) {
                    sb.append(", precio: $").append(String.format("%.0f", p.getPrecioVenta())).append(" MXN");
                }
                if (p.getStock() != null) {
                    sb.append(", stock: ").append(p.getStock()).append(" pzas");
                }
                if (p.getDescripcion() != null && !p.getDescripcion().isBlank()) {
                    sb.append(". ").append(p.getDescripcion());
                }
                sb.append("\n");
            }
            return sb.toString();

        } catch (Exception e) {
            log.error("Error consultando productos para chatbot", e);
            return "Catálogo temporalmente no disponible.";
        }
    }
}