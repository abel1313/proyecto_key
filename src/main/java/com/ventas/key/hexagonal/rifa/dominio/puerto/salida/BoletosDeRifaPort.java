package com.ventas.key.hexagonal.rifa.dominio.puerto.salida;

import com.ventas.key.hexagonal.rifa.dominio.modelo.GrupoDeBoletos;
import com.ventas.key.hexagonal.rifa.dominio.modelo.Participacion;
import com.ventas.key.hexagonal.rifa.dominio.modelo.PerfilEnRed;

import java.util.List;
import java.util.Optional;

/**
 * Lo que el dominio necesita de la base para manejar boletos agrupados.
 *
 * <p>Ojo con {@link #crearParticipacion}: crea <b>una fila</b> en {@code boletos_rifa} por
 * cada participacion (decision D4). El sorteo elige filas, asi que una fila con tres URLs
 * adentro seria una sola chance, no tres.</p>
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]</p>
 */
public interface BoletosDeRifaPort {

    /** Todos los boletos de la rifa, ya agrupados por (plataforma + perfil). */
    List<GrupoDeBoletos> gruposDeLaRifa(Integer rifaId);

    /** El grupo de ese perfil dentro de la rifa, si ya existe. */
    Optional<GrupoDeBoletos> buscarGrupo(Integer rifaId, PerfilEnRed perfil);

    /**
     * Busca esa URL de participacion en toda la rifa, sin importar de que perfil sea.
     * Sirve para el modo {@code UNICA}, que rechaza la URL aunque el duplicado sea de otro
     * cliente.
     */
    Optional<DuenoDeLaUrl> buscarUrlEnLaRifa(Integer rifaId, String urlParticipacion);

    /** Crea la fila del boleto y devuelve la participacion ya con su id. */
    Participacion crearParticipacion(Integer concursanteId, PerfilEnRed perfil, Participacion nueva);

    /** Borra la fila de esa participacion. */
    void borrarParticipacion(Integer boletoId);

    /** El nombre del concursante, para los mensajes de error. */
    Optional<String> nombreDelConcursante(Integer concursanteId);

    /** Quien tiene ya cargada una URL: lo minimo para armar el mensaje del duplicado. */
    record DuenoDeLaUrl(Integer boletoId, Integer concursanteId, String nombre) {
    }
}
