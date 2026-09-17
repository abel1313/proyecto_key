package com.ventas.key.mis.productos.chatbot;

import com.ventas.key.mis.productos.entity.ChatMensaje;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Canal: chat en vivo (el de "Chat directo", donde antes SOLO contestaba el admin a mano).
//
// Es un canal aparte del widget publico (ChatbotSitioWebService) a proposito: aqui si hay una
// persona del otro lado a la cual escalar, asi que el prompt lleva una regla extra -- marcar
// ##HUMANO## cuando el cliente pide hablar con alguien de verdad. El widget publico se queda
// exactamente como estaba; ningun cambio de aqui lo toca.
@Service
@Slf4j
public class ChatbotChatVivoService extends ChatbotBase {

    // Lo escribe el modelo cuando el cliente pide hablar con una persona. El orquestador lo
    // detecta, manda el correo al dueno y pasa la conversacion a modo HUMANO.
    public static final String MARCA_HUMANO = "##HUMANO##";

    // Cuantos mensajes del historial se le pasan al modelo. Suficiente para que entienda el hilo
    // sin mandar conversaciones enteras a OpenAI en cada mensaje.
    private static final int MAX_HISTORIAL = 12;

    public ChatbotChatVivoService(IVarianteRepository varianteRepository,
                                 IPalabraClaveRepository palabraClaveRepository) {
        super(varianteRepository, palabraClaveRepository);
    }

    @Override
    protected String promptBase() {
        return super.promptBase()
                .replace(
                        "CATÁLOGO ACTUAL (variantes disponibles con stock):",
                        """
                        CUANDO EL CLIENTE QUIERE HABLAR CON UNA PERSONA:
                        - Este chat lo atiendes tú, pero SIEMPRE hay una persona del negocio que puede tomar
                          la conversación. Si el cliente la pide, se le pasa — nunca le digas que no se puede.
                        - Detecta la intención, no palabras exactas. Ejemplos de cuándo SÍ aplica:
                          * "quiero hablar con una persona", "me puedes comunicar con alguien"
                          * "hay alguien real?", "quiero hablar con el dueño", "necesito atención humana"
                          * "no me estás entendiendo, quiero hablar con alguien más"
                          * Un reclamo o problema serio con un pedido que tú no puedes resolver.
                        - Cuando aplique, haz exactamente esto:
                          1. Una línea avisando que ya le avisaste a una persona del negocio y que en un
                             momento le contesta por aquí mismo.
                          2. Escribe al final, sin espacios extra: ##HUMANO##
                        - NO uses ##HUMANO## solo porque no supiste un dato. Primero intenta contestar; úsalo
                          cuando el cliente pida a una persona o el asunto claramente necesite una.
                        - NUNCA uses ##FAREWELL## junto con ##HUMANO##: si pide una persona, se escala.

                        SI YA HAY UNA PERSONA EN LA CONVERSACIÓN:
                        - En el historial, los mensajes marcados como (persona del negocio) los escribió un
                          humano, no tú. Respeta lo que ya contestó: no lo contradigas ni repitas su mensaje.

                        CATÁLOGO ACTUAL (variantes disponibles con stock):""");
    }

    /**
     * Esta pantalla no dibuja tarjetas: el mensaje del bot se pinta como texto y nada más. Por eso
     * se reemplaza COMPLETA la sección de tarjetas de la base -- ejemplos y REGLA CRÍTICA
     * incluidos. Desactivarla a medias no funcionó: mientras abajo quedara el ejemplo "... 😊
     * ¿Quieres ver una foto?", el modelo lo copiaba tal cual y prometía una foto que nunca llega
     * (el cliente contestaba "sí" y se quedaba esperando). Los ejemplos de aquí empujan al lado
     * contrario, que es lo que el modelo sí sigue.
     */
    @Override
    protected String seccionMostrarProductos() {
        return """
                CÓMO HABLAR DE PRODUCTOS EN ESTE CHAT — SOLO TEXTO, NUNCA FOTOS:
                - Esta pantalla NO dibuja tarjetas ni imágenes. NUNCA uses ##BUSCAR##.
                - NUNCA ofrezcas mandar ni mostrar fotos o imágenes, ni le preguntes si las quiere
                  ver: aquí no se pueden dibujar y el cliente se queda esperando una que no llega.
                - Cuando encuentres el producto, dilo por texto: nombre, presentación y precio.
                  Si hay varios que le sirven, menciónale varios, no sólo uno.
                - Ejemplos:
                  * "tienes shorts?" → "¡Sí! Tenemos el Jeans Short Especial (short chico) a $250 MXN
                    y el Surprise SU8183 (short mezclilla) a $280 MXN 😊 ¿Te digo qué tallas hay?"
                  * "tienes bolsas Coach?" → "¡Sí! La Bolsa Coach Mini a $450 MXN. ¿Te digo los colores?"
                - Si el cliente pide fotos o imágenes, NO se las prometas. Dile que por aquí no se
                  pueden ver, e invítalo a la tienda en línea o ofrécele pasarlo con una persona.
                - Ejemplos:
                  * "me mandas foto?" → "Por aquí no puedo mandar fotos, pero lo puedes ver en la
                    tienda en línea 😊 ¿O quieres que te pase con una persona que te las mande?"
                  * "quiero ver imágenes" → "Por este chat no se ven las fotos; en la tienda en línea
                    están todas. ¿Te paso con alguien del negocio?"

                """;
    }

    /**
     * Contesta un mensaje del chat en vivo. El historial son los mensajes ya guardados de esa
     * conversación (chat_mensaje), no lo que traiga el navegador: así el bot ve también lo que
     * contestó el admin y no se pisa con él.
     */
    public Mono<String> responder(String mensaje, List<ChatMensaje> historial) {
        String categoria = detectarCategoriaEnConversacion(mensaje, textosDelCliente(historial));

        List<Map<String, String>> mensajes = new ArrayList<>();
        mensajes.add(Map.of("role", "system", "content", promptBase() + obtenerContextoVariantes(categoria)));
        mensajes.addAll(historialParaModelo(historial));
        mensajes.add(Map.of("role", "user", "content", mensaje));

        // Entre "el bot va a contestar" y la respuesta no había ninguna marca, y en medio va lo más
        // lento y lo más frágil: armar el catálogo (varias consultas) y salir a internet. Sin esta
        // línea el log no distingue "se atoró armando el prompt" de "OpenAI no contestó".
        log.info("Chat en vivo: prompt armado (categoría={}, {} mensajes al modelo), llamando a OpenAI",
                categoria, mensajes.size());
        return llamarOpenAI(mensajes);
    }

    private List<Map<String, String>> historialParaModelo(List<ChatMensaje> historial) {
        if (historial == null || historial.isEmpty()) return List.of();
        List<ChatMensaje> recientes = historial.size() > MAX_HISTORIAL
                ? historial.subList(historial.size() - MAX_HISTORIAL, historial.size())
                : historial;

        List<Map<String, String>> resultado = new ArrayList<>();
        for (ChatMensaje m : recientes) {
            if (m.getContenido() == null || m.getContenido().isBlank()) continue;
            if ("USUARIO".equals(m.getRemitente())) {
                resultado.add(Map.of("role", "user", "content", m.getContenido()));
            } else if ("ADMIN".equals(m.getRemitente())) {
                // Se marca para que el modelo sepa que eso lo contestó una persona y no lo repita
                // ni lo contradiga cuando retome la conversación.
                resultado.add(Map.of("role", "assistant",
                        "content", "(persona del negocio) " + m.getContenido()));
            } else {
                resultado.add(Map.of("role", "assistant", "content", m.getContenido()));
            }
        }
        return resultado;
    }

    private List<String> textosDelCliente(List<ChatMensaje> historial) {
        if (historial == null) return List.of();
        return historial.stream()
                .filter(m -> "USUARIO".equals(m.getRemitente()))
                .map(ChatMensaje::getContenido)
                .filter(c -> c != null && !c.isBlank())
                .toList();
    }
}
