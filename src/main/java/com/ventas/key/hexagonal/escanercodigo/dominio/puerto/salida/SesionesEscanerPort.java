package com.ventas.key.hexagonal.escanercodigo.dominio.puerto.salida;

import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.SesionEscaner;

import java.time.Instant;
import java.util.Optional;

/**
 * Dónde viven las conexiones abiertas.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 *
 * <p>Hoy están en memoria del servidor (una sola copia de la aplicación). Si algún día hay varias, otro adaptador las
 * guarda en Redis sin tocar el dominio.
 */
public interface SesionesEscanerPort {

    void guardar(SesionEscaner sesion);

    Optional<SesionEscaner> buscar(String id);

    void eliminar(String id);

    /** Conexiones vigentes en total. */
    int vigentes(Instant ahora);

    /** La conexión vigente de ese origen exacto, si hay (para el canal de una cuenta: a lo más una). */
    Optional<SesionEscaner> buscarVigentePorOrigen(String origen, Instant ahora);

    /** Conexiones vigentes cuyo origen NO empieza con ninguno de {@code prefijosDeCuenta} (las del personal con sesión no cuentan). */
    int vigentesAnonimas(Instant ahora, String... prefijosDeCuenta);

    /** Conexiones vigentes abiertas desde ese origen (la IP de la PC). */
    int vigentesDe(String origen, Instant ahora);

    /** Quita las vencidas o cerradas (limpieza). */
    int limpiar(Instant ahora);
}
