package com.ventas.key.mis.productos.chatbot;

import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Canal: WhatsApp (bot «NovedadesJade», repo whatsapp_bot). Vive en este paquete porque usa constantes de {@link ChatbotBase} que no son
 * publicas (REGLAS_DE_RESPETO). Ver {@code PLAN_WHATSAPP_PUBLICACIONES.md}, seccion 6.
 *
 * <p>Ahorra tokens: sin catalogo ni politicas largas (solo los datos del producto de la publicacion), respuesta de ~150 tokens como
 * maximo y sin historial. Las marcas que entiende el caso de uso son {@code ##ESCALAR##} y {@code ##GRACIAS##} (las explica
 * {@link ChatbotBase#instruccionesSobreProducto}).
 */
@Service
public class ChatbotWhatsappService extends ChatbotBase {

    public ChatbotWhatsappService(IVarianteRepository varianteRepository, IPalabraClaveRepository palabraClaveRepository) {
        super(varianteRepository, palabraClaveRepository);
    }

    @Override
    protected int maxTokens() {
        return 150;
    }

    /** Mensaje sobre una publicacion: contesta SOLO sobre ese producto; lo demas lo escala. */
    public Mono<String> responderSobreProductoWhatsapp(String texto, Variantes variante, boolean enGrupo) {
        List<Map<String, String>> mensajes = new ArrayList<>();
        mensajes.add(Map.of("role", "system", "content", identidad(enGrupo) + REGLAS_DE_RESPETO));
        mensajes.add(Map.of("role", "system", "content", instruccionesSobreProducto(variante, false)));
        mensajes.add(Map.of("role", "user", "content", texto));
        return llamarOpenAI(mensajes);
    }

    /** Chat privado sin publicacion de por medio: contesta con todo el catalogo (regla R3 de botredes). */
    public Mono<String> responderMensajeDirectoWhatsapp(String texto, boolean esPrimeraVez) {
        return responderMensajeDirecto(texto, esPrimeraVez);
    }

    private static String identidad(boolean enGrupo) {
        return """
                Eres NovedadesJade, el asistente automático de Novedades Jade, una tienda mexicana de bolsas, ropa y perfumes.
                Estás contestando por WHATSAPP sobre un producto que la tienda publicó.
                Responde en español, amable, breve (1 a 3 líneas) y con 1 emoji como máximo, como una vecina del pueblo
                atendiendo. Nunca inventes precios, tallas, colores ni existencias.
                """ + (enGrupo ? """
                Tu respuesta la leen todas las personas del grupo: nunca menciones datos de otras personas, domicilios ni teléfonos.
                """ : "") + "\n";
    }
}
