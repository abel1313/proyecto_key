package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.chatbot;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.HorarioDeAtencion;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.TurnoDeConversacion;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.CerebroWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.HorarioDeAtencionPort;
import com.ventas.key.mis.productos.chatbot.ChatbotWhatsappService;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
    private final HorarioDeAtencionPort horario;
    private final Clock reloj;

    @org.springframework.beans.factory.annotation.Autowired
    public CerebroWhatsappAdapter(ChatbotWhatsappService chatbot, HorarioDeAtencionPort horario) {
        this(chatbot, horario, Clock.systemUTC());
    }

    /** Con el reloj inyectado (hallazgo M7): la hora con la que se decide si hay una persona sale del mismo reloj que el resto. */
    CerebroWhatsappAdapter(ChatbotWhatsappService chatbot, HorarioDeAtencionPort horario, Clock reloj) {
        this.chatbot = chatbot;
        this.horario = horario;
        this.reloj = reloj;
    }

    @Override
    public String responder(String mensaje, List<TurnoDeConversacion> historial, boolean esPrimeraVez,
                            String contextoDePedido) {
        List<ChatbotWhatsappService.Turno> turnos = historial.stream()
                .map(t -> new ChatbotWhatsappService.Turno(
                        ChatbotWhatsappService.Turno.Rol.valueOf(t.rol().name()), t.texto()))
                .toList();
        String horarioTexto = null;
        boolean hayPersonaAhora = true;
        try {
            Optional<HorarioDeAtencion> h = horario.actual();
            horarioTexto = h.map(HorarioDeAtencion::aTexto).orElse(null);
            hayPersonaAhora = h.map(x -> x.abiertoEn(reloj.instant())).orElse(true);
        } catch (RuntimeException e) {
            // sin horario el bot no da ninguno y se trata como abierto
        }
        String respuesta = chatbot.responder(mensaje, turnos, esPrimeraVez, horarioTexto, hayPersonaAhora, contextoDePedido).block(ESPERA_MAXIMA);
        if (respuesta == null || respuesta.isBlank()) {
            throw new IllegalStateException("El chatbot no devolvió respuesta");
        }
        return respuesta;
    }
}
