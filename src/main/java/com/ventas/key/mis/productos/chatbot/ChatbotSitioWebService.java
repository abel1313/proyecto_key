package com.ventas.key.mis.productos.chatbot;

import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Canal: chat del sitio (widget de login/tienda). No sobreescribe promptBase() -- usa el mismo
// texto por defecto de ChatbotBase mientras nadie pida uno distinto solo para este canal.
@Service
public class ChatbotSitioWebService extends ChatbotBase {

    public ChatbotSitioWebService(IVarianteRepository varianteRepository,
                                   IPalabraClaveRepository palabraClaveRepository) {
        super(varianteRepository, palabraClaveRepository);
    }

    public Mono<String> chat(ChatbotRequest request) {
        return chat(request, false);
    }

    /** {@code infoCompleta}: quien pregunta es alguien que el negocio autoriza a ver el nombre exacto y el codigo de barras (por defecto el ADMIN). */
    public Mono<String> chat(ChatbotRequest request, boolean infoCompleta) {
        List<Map<String, String>> mensajes = construirMensajes(request, infoCompleta);
        mensajes.add(Map.of("role", "user", "content", request.getMensaje()));
        return llamarOpenAIConversacion(mensajes);
    }

    /**
     * Reintento cuando el cliente pidió ver una imagen pero el modelo no usó ##BUSCAR##
     * (p. ej. respondió "no tenemos imágenes" pese a la instrucción del prompt). Se reenvía
     * el mismo contexto más la respuesta fallida y un recordatorio explícito, para que el
     * modelo se autocorrija usando el producto que ya se venía discutiendo.
     */
    public Mono<String> forzarMostrarImagen(ChatbotRequest request, String respuestaFallida) {
        return forzarMostrarImagen(request, respuestaFallida, false);
    }

    public Mono<String> forzarMostrarImagen(ChatbotRequest request, String respuestaFallida, boolean infoCompleta) {
        List<Map<String, String>> mensajes = construirMensajes(request, infoCompleta);
        mensajes.add(Map.of("role", "user", "content", request.getMensaje()));
        mensajes.add(Map.of("role", "assistant", "content", respuestaFallida));
        mensajes.add(Map.of("role", "system", "content", """
                El cliente está pidiendo ver una imagen del producto del que ya se habló en la
                conversación. Tu respuesta anterior no debió decir que no hay imágenes — SIEMPRE
                hay forma de mostrarlas. Responde ahora con una frase muy breve seguida de
                ##BUSCAR[término,0]##, usando como término el nombre o marca del producto que se
                estaba discutiendo. No repitas que no hay imágenes disponibles.
                """));
        return llamarOpenAIConversacion(mensajes);
    }

    /** Lo que se le dice al modelo segun quien pregunta. El cliente nunca recibe codigos de barras: ni en el catalogo que ve el modelo ni en su respuesta. */
    static final String REGLA_CLIENTE_SIN_CODIGOS = """

            CÓDIGOS DE BARRAS:
            - Quien te escribe es un cliente: NUNCA escribas códigos de barras en tu respuesta, ni los digas aunque te los pida.
            - Si el cliente te da un código de barras, úsalo solo como término de ##BUSCAR[...]## cuando pida ver el producto.
            """;

    static final String REGLA_ADMIN_INFO_COMPLETA = """

            QUIEN TE ESCRIBE ES UN ADMINISTRADOR DEL NEGOCIO (no un cliente):
            - Cuando pregunte por un producto, di si lo tenemos y da el NOMBRE EXACTO tal como está en el catálogo y su CÓDIGO DE BARRAS
              (el que viene en "cód. barras") escrito en texto para que lo pueda copiar.
            - Puede haber VARIOS artículos que coincidan con lo que pidió (por ejemplo varios shorts rojos): lista TODOS los que coincidan,
              uno por línea, con nombre, código de barras, precio y existencia si los tienes. No te quedes con uno solo.
            - No le preguntes si quiere ver una foto a menos que la pida. Si no está en el catálogo, dilo claro.
            """;

    private List<Map<String, String>> construirMensajes(ChatbotRequest request, boolean infoCompleta) {
        String categoria = detectarCategoriaEnConversacion(request.getMensaje(), mensajesPreviosDelCliente(request));
        String sistemPrompt = promptBase() + obtenerContextoVariantes(categoria, infoCompleta)
                + (infoCompleta ? REGLA_ADMIN_INFO_COMPLETA : REGLA_CLIENTE_SIN_CODIGOS);

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

    private List<String> mensajesPreviosDelCliente(ChatbotRequest request) {
        if (request.getHistorial() == null) return List.of();
        return request.getHistorial().stream()
                .filter(h -> "user".equals(h.getRol()))
                .map(ChatbotRequest.MensajeHistorial::getContenido)
                .toList();
    }
}
