package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.pedido;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.BorradorDePedido;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.BorradoresDePedidoPort;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda en memoria el resumen que espera el «sí» del cliente. Decisión consciente (regla R16): dura minutos, solo
 * hay un pod (R8) y no hay dinero de por medio. Si el servidor se reinicia a media conversación el resumen se pierde
 * y el cliente solo tiene que repetir su confirmación (el bot vuelve a proponer el pedido). Se evita así una tabla y
 * una migración nuevas. Caduca solo: los que pasan de 2 h se limpian al guardar uno nuevo.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class BorradoresDePedidoMemoriaAdapter implements BorradoresDePedidoPort {

    private static final Duration LIMPIAR_DESPUES_DE = Duration.ofHours(2);

    private final ConcurrentHashMap<Long, BorradorDePedido> porCliente = new ConcurrentHashMap<>();

    @Override
    public void guardar(long clienteId, BorradorDePedido borrador) {
        Instant limite = Instant.now().minus(LIMPIAR_DESPUES_DE);
        porCliente.values().removeIf(b -> b.creadoEn() == null || b.creadoEn().isBefore(limite));
        porCliente.put(clienteId, borrador);
    }

    @Override
    public Optional<BorradorDePedido> porCliente(long clienteId) {
        return Optional.ofNullable(porCliente.get(clienteId));
    }

    @Override
    public void descartar(long clienteId) {
        porCliente.remove(clienteId);
    }
}
