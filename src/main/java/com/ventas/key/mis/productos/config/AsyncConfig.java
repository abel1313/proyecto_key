package com.ventas.key.mis.productos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

// Pool acotado para la carga rapida de imagenes (CargaImagenesServiceImpl.subirImagenAsync).
// @EnableAsync ya esta activo en MisProductosApplication; sin un Executor propio, Spring usa
// SimpleAsyncTaskExecutor por default, que abre un hilo nuevo SIN LIMITE por cada @Async — si
// el admin sube 50-100 fotos seguidas eso satura el servidor y bombardea al microservicio de
// imagenes con esa misma cantidad de requests simultaneos. Este bean limita cuantas subidas
// corren en paralelo; el resto se encola (hasta queueCapacity) en vez de disparar mas hilos.
@Configuration
public class AsyncConfig {

    @Bean("cargaImagenesExecutor")
    public Executor cargaImagenesExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(300);
        executor.setThreadNamePrefix("carga-imagenes-");
        // Si el pool y la cola de 300 se llenan (burst extremo), procesa en el hilo que
        // llamo en vez de tirar RejectedExecutionException — se vuelve sincrono para ese
        // caso limite pero no se pierde ninguna imagen.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        // El SecurityContext (y con el, el JWT que WebClientConfig propaga como Authorization
        // al microservicio de imagenes) es un ThreadLocal del hilo del request: los hilos de
        // este pool NO lo heredan solos, y sin el la subida llega sin token al micro y este
        // responde 403. Este wrapper captura el contexto al encolar la tarea (en el hilo del
        // request, donde si existe) y lo instala en el hilo del pool mientras corre.
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
