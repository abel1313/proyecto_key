package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.abuso;

import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ControlDeAbusoWhatsappPort;
import com.ventas.key.mis.productos.chatbot.ChatbotBlockService;
import org.springframework.stereotype.Component;

/**
 * Usa los mismos bloqueos, cooldown y límite por hora que el chat del sitio y los bots de redes,
 * con la clave "whatsapp:" + número.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class ControlDeAbusoWhatsappAdapter implements ControlDeAbusoWhatsappPort {

    private final ChatbotBlockService bloqueos;

    public ControlDeAbusoWhatsappAdapter(ChatbotBlockService bloqueos) {
        this.bloqueos = bloqueos;
    }

    @Override
    public boolean bloqueado(String clave) {
        return bloqueos.estaBloqueado(clave) || bloqueos.estaCooldown(clave);
    }

    @Override
    public boolean limiteExcedido(String clave) {
        return bloqueos.limiteMensajesExcedido(clave);
    }

    @Override
    public void registrarMensaje(String clave) {
        bloqueos.registrarMensaje(clave);
    }

    @Override
    public void registrarNoEntendido(String clave) {
        bloqueos.registrarFarewell(clave);
    }

    @Override
    public void registrarEntendido(String clave) {
        bloqueos.registrarMensajeNormal(clave);
    }
}
