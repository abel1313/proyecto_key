package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.chatbot;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.TurnoDeConversacion;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.CerebroWhatsappPort;
import com.ventas.key.mis.productos.chatbot.ChatbotWhatsappService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * El chatbot de siempre (OpenAI, con su respaldo), con las reglas propias de WhatsApp. Si falla o
 * tarda, lanza la excepción y el servicio pasa la conversación a una persona.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class CerebroWhatsappAdapter implements CerebroWhatsappPort {

    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(25);

    private final ChatbotWhatsappService chatbot;

    public CerebroWhatsappAdapter(ChatbotWhatsappService chatbot) {
        this.chatbot = chatbot;
    }

    @Override
    public String responder(String mensaje, List<TurnoDeConversacion> historial, boolean esPrimeraVez) {
        List<ChatbotWhatsappService.Turno> turnos = historial.stream()
                .map(t -> new ChatbotWhatsappService.Turno(
                        ChatbotWhatsappService.Turno.Rol.valueOf(t.rol().name()), t.texto()))
                .toList();
        String respuesta = chatbot.responder(mensaje, turnos, esPrimeraVez).block(ESPERA_MAXIMA);
        if (respuesta == null || respuesta.isBlank()) {
            throw new IllegalStateException("El chatbot no devolvió respuesta");
        }
        return respuesta;
    }
}
