package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import java.util.Optional;

/** [Hexagonal: Driven Port] [Clean: Interface Adapter] El enlace de WhatsApp del negocio (wa.me/...), si esta configurado. */
public interface ContactoNegocioPort {

    Optional<String> whatsappUrl();
}
