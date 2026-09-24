package com.ventas.key.hexagonal.botredes.dominio.puerto.salida;

import com.ventas.key.hexagonal.botredes.dominio.modelo.Canal;
import com.ventas.key.hexagonal.botredes.dominio.modelo.ComentarioOriginal;
import com.ventas.key.hexagonal.botredes.dominio.modelo.Interaccion;

import java.util.Optional;

/**
 * Historial de lo que el bot recibió y contestó ({@code comentario_social} y
 * {@code mensaje_directo_social}).
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]</p>
 */
public interface RegistroInteraccionesPort {

    /** Meta a veces reenvía el mismo evento: si ya se procesó, no se contesta dos veces. */
    boolean yaProcesada(Canal canal, String id);

    /** Si alguna vez le contestamos a esta persona. Si no, es su primera vez y el bot se presenta. */
    boolean yaLeHabiamosEscrito(Canal canal, String autorId);

    void guardar(Interaccion interaccion, String respuesta, String idRespuesta);

    /** Si ese comentario o mensaje de la cuenta del negocio es la respuesta que publicó el bot. */
    boolean esRespuestaDelBot(Canal canal, String id);

    Optional<ComentarioOriginal> comentarioOriginal(String commentId);
}
