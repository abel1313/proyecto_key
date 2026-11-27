package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.InformacionDelNegocio;

import java.util.Optional;

/**
 * Dirección, ubicación y redes del negocio, tal como están en Sistema → Negocio y contactos.
 * Solo se LEE (nunca crea ni cambia la configuración). Vacío si no se pudo leer. [Hexagonal: Driven Port]
 */
public interface InformacionDelNegocioPort {

    Optional<InformacionDelNegocio> actual();
}
