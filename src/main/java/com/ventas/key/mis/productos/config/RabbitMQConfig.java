package com.ventas.key.mis.productos.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_IMAGENES   = "exchange.imagenes";
    public static final String QUEUE_GUARDAR       = "queue.guardar.imagenes";
    public static final String ROUTING_KEY_GUARDAR = "guardar.imagen";

    // Solo el exchange — la cola y el binding los declara el consumidor (micro_imagenes)
    @Bean
    public DirectExchange exchangeImagenes() {
        return new DirectExchange(EXCHANGE_IMAGENES);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}