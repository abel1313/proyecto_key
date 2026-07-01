package com.ventas.key.mis.productos.chatbot;

import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public Mono<String> chat(ChatbotRequest request) {
        List<Map<String, String>> mensajes = construirMensajes(request);
        mensajes.add(Map.of("role", "user", "content", request.getMensaje()));
        return llamarOpenAI(mensajes);
    }

    /**
     * Reintento cuando el cliente pidió ver una imagen pero el modelo no usó ##BUSCAR##
     * (p. ej. respondió "no tenemos imágenes" pese a la instrucción del prompt). Se reenvía
     * el mismo contexto más la respuesta fallida y un recordatorio explícito, para que el
     * modelo se autocorrija usando el producto que ya se venía discutiendo.
     */
    public Mono<String> forzarMostrarImagen(ChatbotRequest request, String respuestaFallida) {
        List<Map<String, String>> mensajes = construirMensajes(request);
        mensajes.add(Map.of("role", "user", "content", request.getMensaje()));
        mensajes.add(Map.of("role", "assistant", "content", respuestaFallida));
        mensajes.add(Map.of("role", "system", "content", """
                El cliente está pidiendo ver una imagen del producto del que ya se habló en la
                conversación. Tu respuesta anterior no debió decir que no hay imágenes — SIEMPRE
                hay forma de mostrarlas. Responde ahora con una frase muy breve seguida de
                ##BUSCAR[término,0]##, usando como término el nombre o marca del producto que se
                estaba discutiendo. No repitas que no hay imágenes disponibles.
                """));
        return llamarOpenAI(mensajes);
    }

    private List<Map<String, String>> construirMensajes(ChatbotRequest request) {
        String contexto = obtenerContextoVariantes();

        String sistemPrompt = """
                Eres el asistente virtual de Novedades Jade, una tienda en línea mexicana.
                Responde siempre en español, de manera amable, breve y clara.
                No inventes precios ni productos que no estén en el catálogo.

                POLÍTICAS DE LA TIENDA:
                - Entregas en: Luvianos, el Estanco, Caja de Agua, Acatitlán, Tejupilco (Estado de México) y Zacazonapan.
                - Pagos: tarjeta de crédito, débito, transferencia y efectivo.

                TONO:
                - Amable, cercano y sencillo. Como si fuera una vecina del pueblo atendiendo.
                - Respuestas cortas y directas, sin rodeos.
                - Puedes usar 1 o 2 emojis por mensaje para ser más expresivo, sin exagerar.

                MOSTRAR PRODUCTOS EN TARJETAS — DOS CASOS:

                CASO 1 — El bot encuentra o confirma un producto:
                - Responde brevemente con nombre y precio, luego pregunta "¿Quieres ver una foto?"
                - NO uses ##BUSCAR## todavía — espera a que el cliente diga que sí.
                - Ejemplos:
                  * "tienes cod1230981?" → "¡Sí! Tenemos la Mochila para mostrar a $300 MXN 😊 ¿Quieres ver una foto?"
                  * "tienes bolsas Coach?" → "¡Sí tenemos bolsas Coach! ¿Quieres que te muestre las opciones disponibles?"

                CASO 2 — El cliente pide ver foto/imagen O confirma que sí quiere verla:
                - Usa ##BUSCAR[término,offset]## al FINAL de tu respuesta, sin más explicación.
                - término: el nombre del producto o marca. Máximo 2 palabras.
                  Si encontraste el producto por código de barras, usa el NOMBRE del producto.
                - offset: siempre 0.
                - El sistema muestra tarjetas con imagen automáticamente — NO listes productos en texto.
                - Ejemplos:
                  * Cliente: "sí, muéstramela" → "¡Aquí la tienes! 📸 ##BUSCAR[Mochila,0]##"
                  * Cliente: "¿me puedes mostrar una foto?" → "¡Claro! 📸 ##BUSCAR[Mochila,0]##"
                  * Cliente: "¿tienes alguna imagen?" → "¡Claro! 📸 ##BUSCAR[Mochila,0]##"
                  * Cliente: "¿tienes foto de eso?" → "¡Sí! 📸 ##BUSCAR[Mochila,0]##"
                  * Cliente: "quiero ver las opciones" → "¡Aquí van! 😊 ##BUSCAR[Coach,0]##"
                  * Cliente: "muéstrame bolsas" → "¡Claro! 👜 ##BUSCAR[bolsa,0]##"

                REGLA CRÍTICA — cualquier mensaje del cliente que mencione las palabras "imagen",
                "imágenes", "foto" o "fotos" sobre un producto YA identificado en la conversación
                SIEMPRE es CASO 2, sin importar si está en forma de pregunta ("¿tienes imagen?")
                u orden ("muéstrame la imagen"). NUNCA respondas que no hay imágenes o que no
                puedes mostrarlas — siempre puedes mostrarlas con ##BUSCAR##.
                NUNCA uses ##BUSCAR## sin que el cliente haya pedido ver el producto o confirmado que sí quiere verlo.

                MANEJO DE MENSAJES NO COMPRENSIBLES O FUERA DE CONTEXTO:
                - USA ##FAREWELL## ÚNICAMENTE si el mensaje es basura, incomprensible,
                  o no tiene NINGUNA relación con la tienda (productos, precios, envíos, pagos, pedidos).
                - Ejemplos de cuándo SÍ usar ##FAREWELL##:
                  * "asdjklasdjl", "jajajaja", "¿qué hora es?", "¿cómo está el clima?", insultos, spam.
                - Ejemplos de cuándo NO usar ##FAREWELL##:
                  * Cualquier pregunta de tienda, aunque la respuesta sea "no tenemos eso".
                - Cuando SÍ aplique ##FAREWELL##, haz exactamente esto:
                  1. Indica brevemente que no pudiste entender su mensaje.
                  2. Menciona que puede contactarnos por Facebook o WhatsApp.
                  3. Despedida corta y amable (máximo 2 líneas).
                  4. Escribe al final, sin espacios extra: ##FAREWELL##

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

        return mensajes;
    }

    private Mono<String> llamarOpenAI(List<Map<String, String>> mensajes) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", mensajes,
                "max_tokens", 500,
                "temperature", 0.7
        );

        return webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(20))
                .map(response -> {
                    if (response == null) throw new RuntimeException("Sin respuesta de OpenAI");
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                });
    }

    private static final int CHATBOT_PAGE_SIZE = 2;

    public Map<String, Object> buscarProductos(String query, int offset) {
        int pagina = offset / CHATBOT_PAGE_SIZE;
        Page<Variantes> page = varianteRepository.buscarParaChatbot(
                query.trim(), PageRequest.of(pagina, CHATBOT_PAGE_SIZE));

        List<Map<String, Object>> productos = page.getContent().stream().map(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("varianteId", v.getId());
            p.put("nombre", v.getProducto().getNombre());
            p.put("marca", v.getMarca());
            p.put("talla", v.getTalla());
            p.put("color", v.getColor());
            p.put("precio", v.getProducto().getPrecioVenta());
            p.put("stock", v.getStock());
            return p;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("productos", productos);
        result.put("hayMas", page.hasNext());
        result.put("busquedaQuery", query.trim());
        result.put("busquedaOffset", offset + page.getNumberOfElements());
        return result;
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

                if (v.getProducto().getCodigoBarras() != null
                        && v.getProducto().getCodigoBarras().getCodigoBarras() != null
                        && !v.getProducto().getCodigoBarras().getCodigoBarras().isBlank()) {
                    sb.append(" [cód. barras: ").append(v.getProducto().getCodigoBarras().getCodigoBarras()).append("]");
                }
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