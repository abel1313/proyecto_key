package com.ventas.key.hexagonal.busquedapedido.infraestructura.salida.reloj;

import com.ventas.key.hexagonal.busquedapedido.dominio.puerto.salida.HoyPort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 *
 * <p>El dia de hoy en Mexico, aunque el contenedor corra en UTC.
 */
@Component
public class HoyEnLaTiendaAdapter implements HoyPort {

    static final ZoneId ZONA_DE_LA_TIENDA = ZoneId.of("America/Mexico_City");

    @Override
    public LocalDate hoy() {
        return LocalDate.now(ZONA_DE_LA_TIENDA);
    }
}
