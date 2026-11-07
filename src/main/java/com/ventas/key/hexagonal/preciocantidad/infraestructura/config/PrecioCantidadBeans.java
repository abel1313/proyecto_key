package com.ventas.key.hexagonal.preciocantidad.infraestructura.config;

import com.ventas.key.hexagonal.preciocantidad.aplicacion.servicio.AdministrarGruposService;
import com.ventas.key.hexagonal.preciocantidad.aplicacion.servicio.CalcularPreciosPorCantidadService;
import com.ventas.key.hexagonal.preciocantidad.dominio.puerto.entrada.AdministrarGruposCasoUso;
import com.ventas.key.hexagonal.preciocantidad.dominio.puerto.entrada.CalcularPreciosPorCantidadCasoUso;
import com.ventas.key.hexagonal.preciocantidad.dominio.puerto.salida.GruposDePrecioPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Arma los casos de uso con su adaptador. Los servicios son Java puro. Su propio reloj: producción no tiene un bean Clock global. */
@Configuration
public class PrecioCantidadBeans {

    @Bean
    public CalcularPreciosPorCantidadCasoUso calcularPreciosPorCantidadCasoUso(GruposDePrecioPort grupos) {
        return new CalcularPreciosPorCantidadService(grupos, Clock.systemDefaultZone());
    }

    @Bean
    public AdministrarGruposCasoUso administrarGruposCasoUso(GruposDePrecioPort grupos) {
        return new AdministrarGruposService(grupos);
    }
}
