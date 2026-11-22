package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PublicacionWhatsapp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** [Hexagonal: Driven Port] [Clean: Interface Adapter] Lo que el dominio necesita para leer y guardar publicaciones. */
public interface PublicacionesWhatsappPort {

    /** Las PENDIENTE con hora vencida, con bloqueo de fila para que dos llamadas del bot no recojan la misma. */
    List<PublicacionWhatsapp> pendientesVencidasConBloqueo(LocalDateTime ahora, int maximo);

    Optional<PublicacionWhatsapp> buscarConBloqueo(int id);

    Optional<PublicacionWhatsapp> buscar(int id);

    PublicacionWhatsapp guardar(PublicacionWhatsapp publicacion);

    /** Inserta una publicacion nueva (aun sin codigo) y la devuelve con su id. */
    PublicacionWhatsapp crear(PublicacionWhatsapp publicacion);

    /** Por su codigo (J42). Vacio si no existe. */
    Optional<PublicacionWhatsapp> buscarPorCodigo(String codigoRef);

    /** Las PUBLICADA en ese grupo desde esa fecha (para saber de cual hablan cuando no citan ni ponen el codigo). */
    List<PublicacionWhatsapp> publicadasDelGrupoDesde(String grupo, LocalDateTime desde);

    /** Las mas recientes primero. */
    List<PublicacionWhatsapp> listar(int maximo);
}
