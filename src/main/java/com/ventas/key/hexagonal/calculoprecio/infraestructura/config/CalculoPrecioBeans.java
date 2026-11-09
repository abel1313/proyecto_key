package com.ventas.key.hexagonal.calculoprecio.infraestructura.config;

import com.ventas.key.hexagonal.calculoprecio.aplicacion.servicio.CalculoPrecioService;
import com.ventas.key.hexagonal.calculoprecio.dominio.puerto.entrada.CalculoPrecioCasoUso;
import com.ventas.key.hexagonal.calculoprecio.dominio.puerto.salida.CalculosPrecioPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Arma el caso de uso con su adaptador. Su propio reloj: producción no tiene un bean Clock global. */
@Configuration
public class CalculoPrecioBeans {
    @Bean
    public CalculoPrecioCasoUso calculoPrecioCasoUso(CalculosPrecioPort calculos) {
        return new CalculoPrecioService(calculos, Clock.systemDefaultZone());
    }
}
