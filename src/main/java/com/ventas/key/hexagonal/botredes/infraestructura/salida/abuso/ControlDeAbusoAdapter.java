package com.ventas.key.hexagonal.botredes.infraestructura.salida.abuso;

import com.ventas.key.hexagonal.botredes.dominio.puerto.salida.ControlDeAbusoPort;
import com.ventas.key.mis.productos.chatbot.ChatbotBlockService;
import org.springframework.stereotype.Component;

/**
 * Usa los mismos bloqueos, cooldown y límite por hora que el chat del sitio.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class ControlDeAbusoAdapter implements ControlDeAbusoPort {

    private final ChatbotBlockService bloqueos;

    public ControlDeAbusoAdapter(ChatbotBlockService bloqueos) {
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
