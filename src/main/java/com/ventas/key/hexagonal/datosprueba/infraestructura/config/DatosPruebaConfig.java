package com.ventas.key.hexagonal.datosprueba.infraestructura.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Un solo hilo para la corrida de datos de prueba: corre en segundo plano sin quitarle hilos a las
 * peticiones de la tienda, y nunca dos a la vez (R3 lo revisa antes; esto lo asegura).
 *
 * <p>No lleva el SecurityContext del que la arranco: la corrida no lo necesita (la venta directa
 * recibe el usuario por parametro) y asi no depende de un token que expira a los 15 minutos.
 */
@Configuration
public class DatosPruebaConfig {

    @Bean("datosPruebaExecutor")
    public Executor datosPruebaExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("datos-prueba-");
        executor.initialize();
        return executor;
    }
}
