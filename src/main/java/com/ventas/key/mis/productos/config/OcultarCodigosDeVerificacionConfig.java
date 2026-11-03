package com.ventas.key.mis.productos.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ventas.key.mis.productos.entity.Cliente;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * El codigo de verificacion del correo del cliente NO sale en ninguna respuesta HTTP. Antes viajaba dentro del JSON de
 * {@code buscarPorIdCliente} / {@code getOne}: el propio cliente (o cualquiera con acceso a esos datos) leia su codigo y "verificaba" un
 * correo que no era suyo, saltandose el correo de confirmacion.
 *
 * <p>Se oculta con un mixin SOLO en el serializador de las respuestas HTTP: la cache de Redis usa su propio ObjectMapper y sigue guardando
 * el objeto completo, asi que nada del flujo de verificacion cambia.
 */
@Configuration
public class OcultarCodigosDeVerificacionConfig {

    abstract static class ClienteSinCodigos {
        @JsonIgnore abstract String getCodigoVerificacion();
        @JsonIgnore abstract LocalDateTime getCodigoVerificacionExpira();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer ocultarCodigosDeVerificacion() {
        return builder -> builder.mixIn(Cliente.class, ClienteSinCodigos.class);
    }
}
