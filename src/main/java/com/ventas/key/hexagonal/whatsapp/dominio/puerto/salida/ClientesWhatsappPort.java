package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ClienteWhatsapp;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.Pagina;

import java.time.Instant;
import java.util.Optional;

/**
 * Los clientes (números) y la marca de su último mensaje entrante.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]</p>
 */
public interface ClientesWhatsappPort {

    ClienteWhatsapp buscarOCrear(String telefono, String nombre, Instant ahora);

    Optional<ClienteWhatsapp> porId(long id);

    /** Actualiza la marca del último mensaje entrante (nunca hacia atrás) y la última actividad. */
    void registrarEntrante(long id, Instant enviadoEn, Instant ahora, String resumen);

    /** Actualiza la última actividad y el último mensaje cuando nosotros escribimos. */
    void registrarSaliente(long id, String resumen, Instant ahora);

    /** Una persona contestó (o el bot escaló): el bot se aparta un rato (regla R10). */
    void pausarBot(long id, Instant desde);

    /** Una persona tomó la conversación a mano: el bot no contesta hasta que se la devuelvan. */
    void tomarConversacion(long id, Instant desde);

    void reanudarBot(long id);

    void marcarAtencion(long id, boolean requiere);

    Pagina<ClienteWhatsapp> listar(int pagina, int size);
}
