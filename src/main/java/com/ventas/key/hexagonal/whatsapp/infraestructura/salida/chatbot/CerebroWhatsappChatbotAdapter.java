package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.chatbot;

import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.CerebroWhatsappPort;
import com.ventas.key.mis.productos.chatbot.ChatbotWhatsappService;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * [Hexagonal: Driven Adapter] [Clean: Gateway]
 *
 * <p>El chatbot de siempre (OpenAI) con el canal de WhatsApp. Si falla o tarda, lanza la excepcion y el caso de uso escala.
 */
@Component
public class CerebroWhatsappChatbotAdapter implements CerebroWhatsappPort {

    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(25);

    private final ChatbotWhatsappService chatbot;
    private final IVarianteRepository variantes;

    public CerebroWhatsappChatbotAdapter(ChatbotWhatsappService chatbot, IVarianteRepository variantes) {
        this.chatbot = chatbot;
        this.variantes = variantes;
    }

    @Override
    public String responderSobreProducto(String texto, int varianteId, boolean enGrupo) {
        Variantes variante = variantes.findById(varianteId)
                .orElseThrow(() -> new IllegalStateException("El artículo " + varianteId + " ya no existe"));
        return chatbot.responderSobreProductoWhatsapp(texto, variante, enGrupo).block(ESPERA_MAXIMA);
    }

    @Override
    public String responderMensajeDirecto(String texto, boolean esPrimeraVez) {
        return chatbot.responderMensajeDirectoWhatsapp(texto, esPrimeraVez).block(ESPERA_MAXIMA);
    }
}
