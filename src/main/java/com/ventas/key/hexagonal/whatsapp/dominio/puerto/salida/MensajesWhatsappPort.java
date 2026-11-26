package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoDeEntrega;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.EstadoMensaje;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeEntrante;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.OrigenMensaje;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.Pagina;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * El historial de mensajes, separado por cliente.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]</p>
 */
public interface MensajesWhatsappPort {

    /**
     * Guarda un mensaje entrante. Devuelve {@code false} si ya estaba guardado (Meta reenvía): el id
     * de Meta es único en la base, así que también protege contra dos reenvíos a la vez.
     */
    boolean guardarEntrante(long clienteId, MensajeEntrante mensaje);

    MensajeWhatsapp guardarSaliente(long clienteId, OrigenMensaje origen, String tipo, String contenido,
                                    EstadoMensaje estado, String waMessageId, String motivo,
                                    Integer codigoError, String plantilla, Instant en);

    /** Aplica el aviso de Meta al mensaje y devuelve de qué cliente es (vacío si no es un mensaje nuestro). */
    Optional<Long> actualizarEstadoPorWaId(EstadoDeEntrega estado);

    /** Lo más nuevo primero. */
    Pagina<MensajeWhatsapp> historial(long clienteId, int pagina, int size);

    /** Los últimos {@code cuantos} mensajes, del más viejo al más nuevo. */
    List<MensajeWhatsapp> ultimos(long clienteId, int cuantos);

    /** Mensajes del cliente después de nuestra última respuesta que sí salió. */
    long contarSinResponder(long clienteId);

    /** ¿Ya le mandamos algo antes a este cliente? (para presentarse solo la primera vez, regla R9). */
    boolean yaLeHabiamosEscrito(long clienteId);
}
