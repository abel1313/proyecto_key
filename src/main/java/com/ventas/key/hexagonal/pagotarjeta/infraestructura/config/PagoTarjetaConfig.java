package com.ventas.key.hexagonal.pagotarjeta.infraestructura.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** El reloj del cobro (UTC), inyectable para que las pruebas lo controlen. */
@Configuration
public class PagoTarjetaConfig {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock relojDelCobro() {
        return Clock.systemUTC();
    }
}
