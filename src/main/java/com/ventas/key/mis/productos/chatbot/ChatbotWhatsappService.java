package com.ventas.key.mis.productos.chatbot;

import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Canal: WhatsApp (API de Meta). Conversación uno a uno con el cliente, con hilo.
//
// Hereda de ChatbotBase todo lo común (catálogo, políticas de la tienda, REGLAS_DE_RESPETO, la
// llamada a OpenAI y su respaldo) y solo cambia lo propio de WhatsApp, igual que ChatbotChatVivoService:
// las reglas de ESTE canal viven aquí, en promptBase() y seccionMostrarProductos(), sin tocar el
// sitio web, el chat en vivo, Facebook ni Instagram. Para agregar o quitar una regla de WhatsApp,
// se edita REGLAS_WHATSAPP (decisión del dueño: opción A, reglas en el código).
@Service
@Slf4j
public class ChatbotWhatsappService extends ChatbotBase {

    // Lo escribe el modelo cuando el cliente pide a una persona o falta un dato que él no tiene.
    // PoliticaDeRespuestaWhatsapp lo detecta, saluda y pasa la conversación a una persona.
    public static final String MARCA_HUMANO = "##HUMANO##";

    // Cuántos mensajes del hilo se le pasan al modelo: suficiente para el contexto, sin mandar
    // conversaciones enteras a OpenAI en cada mensaje.
    private static final int MAX_HISTORIAL = 12;

    private static final String ANCLA_CATALOGO = "CATÁLOGO ACTUAL (artículos disponibles con stock):";

    /** Un mensaje del hilo. */
    public record Turno(Rol rol, String texto) {
        public enum Rol {
            CLIENTE,
            BOT,
            PERSONA
        }
    }

    private static final String REGLAS_WHATSAPP = """
            CANAL: WHATSAPP — conversación directa con el cliente:
            - Escribe como en WhatsApp: mensajes cortos (1 a 4 líneas), cálidos y claros.
            - WhatsApp no entiende Markdown. Para resaltar algo usa un solo asterisco a cada lado
              (*así*). Nunca uses #, ** ni tablas. Si das varias opciones, una por línea.
            - NUNCA uses ##BUSCAR## ni ##FAREWELL## en este canal.

            CUANDO HAGA FALTA UNA PERSONA:
            - Hay una persona del negocio que puede tomar esta conversación. Si el cliente pide hablar
              con alguien ("quiero hablar con una persona", "¿hay alguien real?", un reclamo serio),
              o te pregunta algo que no puedes contestar con el catálogo y estas instrucciones (otra
              talla o color que no aparece, medidas, apartados, mayoreo, devoluciones, envíos fuera de
              las zonas), haz exactamente esto:
              1. Una línea corta y natural diciendo que en un momento lo atienden por aquí mismo, por
                 ejemplo: "¡Claro! En un momento te atendemos por aquí mismo 💖". No digas que avisaste,
                 mandaste ni notificaste a nadie.
              2. Escribe al final, sin espacios extra: ##HUMANO##
            - Primero intenta contestar con lo que sí sabes; usa ##HUMANO## cuando de verdad haga falta.

            SI YA HAY UNA PERSONA EN LA CONVERSACIÓN:
            - En el historial, los mensajes marcados como (persona del negocio) los escribió un humano,
              no tú. Respeta lo que ya contestó: no lo contradigas ni lo repitas.

            """;

    private static final String PRIMER_MENSAJE = """
            Este es el PRIMER mensaje de esta persona: nunca le hemos escrito. Empieza con un saludo
            cordial y di que eres el asistente automático de Novedades Jade (por ejemplo: "¡Hola! Soy el
            asistente automático de Novedades Jade 💖"), para que sepa que le contesta un asistente y no
            una persona. Después contesta lo que preguntó. Si necesitas usar ##HUMANO##, igual haz la
            presentación en una línea.
            """;

    public ChatbotWhatsappService(IVarianteRepository varianteRepository,
                                  IPalabraClaveRepository palabraClaveRepository) {
        super(varianteRepository, palabraClaveRepository);
    }

    @Override
    protected String promptBase() {
        String base = super.promptBase();
        if (!base.contains(ANCLA_CATALOGO)) {
            // Si alguien cambia el texto de la base, las reglas de WhatsApp no se pueden perder en silencio.
            log.warn("ChatbotWhatsappService: no se encontró el ancla del catálogo en promptBase(); "
                    + "las reglas de WhatsApp se agregan al final");
            return base + REGLAS_WHATSAPP;
        }
        return base.replace(ANCLA_CATALOGO, REGLAS_WHATSAPP + ANCLA_CATALOGO);
    }

    /**
     * Primera fase: WhatsApp contesta solo con texto. Las fotos y los botones se suman después; hasta
     * entonces el bot no promete mandar imágenes (el cliente se quedaría esperando una que no llega,
     * el mismo problema que tuvo el chat en vivo el 2026-09-17) y manda al cliente a la tienda en línea.
     */
    @Override
    protected String seccionMostrarProductos() {
        return """
                CÓMO HABLAR DE PRODUCTOS EN WHATSAPP — SOLO TEXTO POR AHORA:
                - Cuando encuentres el producto, dilo por texto: nombre, presentación y precio. Si hay
                  varios que le sirven, menciónale varios, no solo uno.
                - NUNCA ofrezcas mandar fotos ni imágenes por este chat ni le preguntes si quiere verlas.
                - Si pide fotos, no se las prometas: invítalo a verlas en la tienda en línea
                  https://shop.novedades-jade.com.mx/ o ofrécele que una persona del negocio se las mande
                  (en ese caso termina con ##HUMANO##).
                - Ejemplos:
                  * "tienes shorts?" → "¡Sí! Tenemos el Jeans Short Especial (short chico) a $250 MXN y el
                    Surprise SU8183 (short mezclilla) a $280 MXN 😊 ¿Te digo qué tallas hay?"
                  * "me mandas foto?" → "Por aquí no te puedo mandar fotos, pero las puedes ver en la tienda
                    en línea: https://shop.novedades-jade.com.mx/ 😊 ¿O prefieres que una persona del negocio
                    te las mande?"

                """;
    }

    /**
     * Contesta un mensaje de WhatsApp. El historial es el hilo ya guardado (incluye lo que contestó una
     * persona), no lo que traiga el cliente: así el bot no se pisa con ella.
     */
    public Mono<String> responder(String mensaje, List<Turno> historial, boolean esPrimeraVez) {
        List<Turno> hilo = historial == null ? List.of() : recientes(historial);
        String categoria = detectarCategoriaEnConversacion(mensaje, textosDelCliente(hilo));

        List<Map<String, String>> mensajes = new ArrayList<>();
        mensajes.add(Map.of("role", "system", "content", promptBase() + obtenerContextoVariantes(categoria)));
        if (esPrimeraVez) {
            mensajes.add(Map.of("role", "system", "content", PRIMER_MENSAJE));
        }
        mensajes.addAll(historialParaModelo(hilo));
        mensajes.add(Map.of("role", "user", "content", mensaje));

        log.info("WhatsApp: prompt armado (categoría={}, {} mensajes al modelo), llamando a la IA",
                categoria, mensajes.size());
        return llamarOpenAIConversacion(mensajes);
    }

    private static List<Turno> recientes(List<Turno> historial) {
        return historial.size() > MAX_HISTORIAL
                ? historial.subList(historial.size() - MAX_HISTORIAL, historial.size())
                : historial;
    }

    private static List<Map<String, String>> historialParaModelo(List<Turno> hilo) {
        List<Map<String, String>> resultado = new ArrayList<>();
        for (Turno t : hilo) {
            if (t.texto() == null || t.texto().isBlank()) {
                continue;
            }
            switch (t.rol()) {
                case CLIENTE -> resultado.add(Map.of("role", "user", "content", t.texto()));
                // Se marca para que el modelo sepa que eso lo contestó una persona y no lo repita ni lo contradiga.
                case PERSONA -> resultado.add(Map.of("role", "assistant", "content", "(persona del negocio) " + t.texto()));
                case BOT -> resultado.add(Map.of("role", "assistant", "content", t.texto()));
            }
        }
        return resultado;
    }

    private static List<String> textosDelCliente(List<Turno> hilo) {
        return hilo.stream()
                .filter(t -> t.rol() == Turno.Rol.CLIENTE)
                .map(Turno::texto)
                .filter(t -> t != null && !t.isBlank())
                .toList();
    }
}
