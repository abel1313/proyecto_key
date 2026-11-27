package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.DatosConocidos;

import java.util.Optional;

/**
 * Lo que ESE número de WhatsApp (tal como lo manda Meta) dio en su último pedido hecho por el bot. Solo pedidos hechos
 * desde ese mismo número: un teléfono que alguien más escribió en su propio pedido no cuenta (regla R17). Nunca lanza
 * excepción. [Hexagonal: Driven Port]
 */
public interface DatosConocidosPort {

    Optional<DatosConocidos> porNumeroDeWhatsapp(String numeroDeWhatsapp);
}
