package com.ventas.key.mis.productos.chatbot;

import com.ventas.key.mis.productos.entity.PalabraClave;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Base comun de los "cerebros" del chatbot (2026-09-08, refactor pedido por el dueño): antes
// habia UNA sola clase (ChatbotService) con el prompt duplicado a mano entre sitio web y redes
// sociales -- cambiar uno se sentia siempre en riesgo de mover el otro sin querer, y duplicar
// texto entero para "separarlos" tampoco era mantenible (dos copias del mismo prompt que hay que
// acordarse de mantener iguales cuando SI deben cambiar juntos).
//
// Esta clase abstracta trae lo que TODOS los canales comparten de verdad (llamar a OpenAI,
// armar el catalogo de variantes, detectar la categoria mencionada en el mensaje) y cada canal
// concreto (ChatbotSitioWebService, ChatbotFacebookService, ChatbotInstagramService) hereda de
// aqui. `promptBase()` es el prompt por defecto -- si un canal necesita uno distinto, solo
// sobreescribe ESE metodo en su propia clase, sin tocar los demas canales ni esta base.
@Slf4j
@RequiredArgsConstructor
public abstract class ChatbotBase {

    @Value("${openai.api-key}")
    protected String openaiApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    protected String model;

    protected final IVarianteRepository varianteRepository;
    protected final IPalabraClaveRepository palabraClaveRepository;

    protected final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    /**
     * Prompt de sistema por defecto, compartido por todos los canales mientras ninguno pida algo
     * distinto. Un canal que SI necesite su propio texto sobreescribe este metodo en su propia
     * clase -- el resto (armado de mensajes, llamada a OpenAI, etc.) no cambia.
     */
    protected String promptBase() {
        return """
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
                - término — REGLA DE PRECISIÓN (muy importante, evita traer el producto equivocado):
                  Si el producto de esta conversación se identificó por un código de barras
                  (el cliente lo escribió o tú lo usaste para confirmarlo), usa EXACTAMENTE ese
                  código de barras como término — NUNCA el nombre en ese caso. El nombre puede
                  repetirse en varios productos distintos (ej. "Mochila Prada" y "Mochila para
                  mostrar" son productos DIFERENTES); el código de barras es único y trae el
                  producto exacto. Solo usa nombre o marca (máximo 2 palabras) cuando NO haya
                  código de barras conocido en la conversación.
                - offset: siempre 0.
                - El sistema muestra tarjetas con imagen automáticamente — NO listes productos en texto.
                - Ejemplos:
                  * Cliente preguntó "tienes cod1230981?" y luego dice "sí, muéstramela"
                    → "¡Aquí la tienes! 📸 ##BUSCAR[cod1230981,0]##"
                  * Cliente: "¿me puedes mostrar una foto?" (sin código de barras en la conversación) → "¡Claro! 📸 ##BUSCAR[Mochila,0]##"
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
                """;
    }

    // Responder un comentario o DM de red social. Compartido por los 3 canales de redes
    // (ChatbotFacebookService, ChatbotInstagramService) -- cada uno solo sobreescribe
    // promptBase() si necesita un texto propio, esta logica de armado no cambia entre ellos.
    // Sin historial (un comentario es una interaccion suelta, no una conversacion con hilo). Si
    // el post tiene una variante asociada, se agrega como contexto extra para que el bot priorice
    // ese producto. La respuesta puede seguir trayendo ##FAREWELL##/##BUSCAR## -- el caller decide
    // que hacer con eso (##BUSCAR## no aplica a un comentario de texto plano, se limpia sin usar).
    //
    // esPrimeraVez: si es el primer comentario de este autor (nunca antes le contestamos, ver
    // ComentarioSocial), el bot SIEMPRE debe contestar -- minimo un saludo de cortesia, aunque el
    // comentario no traiga una pregunta clara -- decision explicita del dueño. De ahi en adelante,
    // si no entiende, se queda callado (##FAREWELL##) como el resto de las interacciones.
    protected Mono<String> responderComentario(String comentario, Variantes varianteDelPost, boolean esPrimeraVez) {
        StringBuilder contextoExtra = new StringBuilder();
        if (varianteDelPost != null) {
            contextoExtra.append("Este comentario es sobre esta publicación específica, que es del producto: ")
                    .append(varianteDelPost.getProducto().getNombre());
            if (varianteDelPost.getDescripcion() != null && !varianteDelPost.getDescripcion().isBlank()) {
                contextoExtra.append(". ").append(varianteDelPost.getDescripcion());
            }
            contextoExtra.append(". Prioriza este producto en tu respuesta si el comentario pregunta por él o por su precio.\n");
        }
        contextoExtra.append("""
                Si el cliente pregunta algo ESPECÍFICO sobre un producto (precio, stock, tallas, \
                colores, disponibilidad, etc.) y NO tienes ese dato exacto en el catálogo de arriba \
                (por ejemplo, el precio aparece vacío o el producto no está en el catálogo), NO \
                inventes ni supongas una respuesta. En ese caso responde ÚNICAMENTE con ##ESCALAR## \
                (sin nada más de texto) para que un administrador lo conteste directamente.
                """);
        if (esPrimeraVez) {
            contextoExtra.append("""
                    Este es el PRIMER comentario de esta persona -- nunca le hemos contestado antes. \
                    SIEMPRE debes responder con al menos un saludo cordial de bienvenida, aunque su \
                    comentario no sea una pregunta clara o no tenga relación con la tienda. Si además \
                    pregunta algo entendible sobre un producto, contesta la pregunta junto con el \
                    saludo. NUNCA uses ##FAREWELL## en este caso -- siempre hay que darle la bienvenida. \
                    Esto NO aplica a ##ESCALAR## -- si pregunta un dato específico que no tienes, sigue \
                    usando ##ESCALAR## aunque sea su primer comentario.
                    """);
        }

        String categoria = detectarCategoriaEnMensaje(comentario);
        String sistemPrompt = promptBase() + obtenerContextoVariantes(categoria);

        List<Map<String, String>> mensajes = new java.util.ArrayList<>();
        mensajes.add(Map.of("role", "system", "content", sistemPrompt));
        mensajes.add(Map.of("role", "system", "content", contextoExtra.toString()));
        mensajes.add(Map.of("role", "user", "content", comentario));
        return llamarOpenAI(mensajes);
    }

    protected Mono<String> llamarOpenAI(List<Map<String, String>> mensajes) {
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
            p.put("descripcion", v.getDescripcion());
            if (v.getProducto().getCodigoBarras() != null) {
                p.put("codigoBarras", v.getProducto().getCodigoBarras().getCodigoBarras());
            }
            return p;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("productos", productos);
        result.put("hayMas", page.hasNext());
        result.put("busquedaQuery", query.trim());
        result.put("busquedaOffset", offset + page.getNumberOfElements());
        return result;
    }

    // Categoria (palabra_clave) mencionada en el mensaje del cliente -- ej. "tienes bolsas?"
    // matchea la categoria "bolsa". Se detecta a partir del ULTIMO mensaje del cliente
    // unicamente (no revisa historial): si un mensaje siguiente ya no repite el nombre de la
    // categoria (ej. "las quiero grandes" despues de "tienes bolsas?"), ese turno vuelve a
    // mandar el catalogo completo -- el historial de la conversacion sigue ahi para que el
    // modelo no pierda el hilo, pero el catalogo puntual de ESE turno no queda acotado.
    protected String detectarCategoriaEnMensaje(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) return null;
        List<PalabraClave> categorias = palabraClaveRepository.findAll();
        String mensajeNorm = normalizar(mensaje);
        List<String> palabrasMensaje = Arrays.asList(mensajeNorm.split("\\s+"));
        for (PalabraClave categoria : categorias) {
            String categoriaNorm = normalizar(categoria.getNombre());
            if (categoriaNorm.isBlank()) continue;
            for (String palabra : palabrasMensaje) {
                if (coincideConCategoria(palabra, categoriaNorm)) {
                    return categoria.getNombre();
                }
            }
        }
        return null;
    }

    // Compara una palabra del mensaje del cliente contra el nombre de una categoria, tolerando
    // plurales simples en español ("bolsa"/"bolsas", "pantalon"/"pantalones") -- quita una "s" o
    // "es" final de CUALQUIERA de los dos lados antes de comparar.
    private boolean coincideConCategoria(String palabra, String categoriaNorm) {
        if (palabra.equals(categoriaNorm)) return true;
        String palabraSing = quitarPluralSimple(palabra);
        String categoriaSing = quitarPluralSimple(categoriaNorm);
        return palabraSing.equals(categoriaSing);
    }

    private String quitarPluralSimple(String s) {
        if (s.endsWith("es") && s.length() > 3) return s.substring(0, s.length() - 2);
        if (s.endsWith("s") && s.length() > 2) return s.substring(0, s.length() - 1);
        return s;
    }

    // minusculas + sin acentos, para que "Bolsón"/"bolson" y "PANTALÓN"/"pantalon" comparen igual.
    private String normalizar(String s) {
        return Normalizer.normalize(s.toLowerCase().trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    // Tope subido de 100 a 1000 (2026-09-08): con mas de 100 variantes con stock, las que caian
    // despues del corte (sin ORDER BY, orden arbitrario de la BD) nunca llegaban al catalogo que
    // se le manda al modelo -- el bot terminaba diciendo "no tenemos bolsas disponibles" con
    // bolsas reales en stock, simplemente porque no entraron en las primeras 100 filas.
    private static final int MAX_VARIANTES_CONTEXTO_CHATBOT = 1000;

    // Pedido explicito del dueño (2026-09-08): cuando el mensaje del cliente menciona una
    // categoria dada de alta (palabra_clave, ej. "bolsa"), el catalogo que se le manda al modelo
    // se filtra a SOLO esa categoria -- en vez de mandarle siempre el catalogo entero para que la
    // IA adivine. Con el catalogo ya acotado, la IA sigue afinando dentro de ese subconjunto
    // usando los demas campos ya estructurados de cada variante (color, talla, marca, precio,
    // descripcion) -- esa parte sigue siendo criterio de la IA, no cambia.
    // categoria == null (no se detecto ninguna en el mensaje) -> catalogo completo, igual que antes.
    protected String obtenerContextoVariantes(String categoria) {
        try {
            List<Variantes> variantes = (categoria != null)
                    ? varianteRepository.findByStockGreaterThanAndProducto_HabilitadoAndPalabraClave_NombreIgnoreCase(
                            0, '1', categoria, PageRequest.of(0, MAX_VARIANTES_CONTEXTO_CHATBOT)).getContent()
                    : varianteRepository.findByStockGreaterThanAndProductoHabilitado(
                            0, '1', PageRequest.of(0, MAX_VARIANTES_CONTEXTO_CHATBOT)).getContent();

            if (variantes.isEmpty()) {
                return categoria != null
                        ? "No hay productos disponibles en este momento en la categoría \"" + categoria + "\"."
                        : "No hay productos disponibles en este momento.";
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

                Double precioVenta = v.getProducto().getPrecioVenta();
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
