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
        String categoria = detectarCategoriaEnMensaje(request.getMensaje());
        String sistemPrompt = promptBase() + obtenerContextoVariantes(categoria);

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
}
