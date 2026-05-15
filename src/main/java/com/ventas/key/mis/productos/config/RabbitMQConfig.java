package com.ventas.key.mis.productos.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Nombres como constantes para no escribirlos a mano en cada clase
    public static final String EXCHANGE_IMAGENES   = "exchange.imagenes";
    public static final String QUEUE_GUARDAR       = "queue.guardar.imagenes";
    public static final String ROUTING_KEY_GUARDAR = "guardar.imagen";

    // El Exchange: punto de entrada donde publicas mensajes.
    // DirectExchange = enruta por routing key exacta.
    @Bean
    public DirectExchange exchangeImagenes() {
        return new DirectExchange(EXCHANGE_IMAGENES);
    }

    // La Cola: buzón donde se acumulan los mensajes.
    // durable(true) = sobrevive si RabbitMQ se reinicia, los mensajes no se pierden.
    @Bean
    public Queue queueGuardarImagenes() {
        return QueueBuilder.durable(QUEUE_GUARDAR).build();
    }

    // El Binding: conecta el Exchange con la Cola usando la routing key.
    // Cuando llegue un mensaje con routing key "guardar.imagen" al exchange,
    // Rabbit lo manda a "queue.guardar.imagenes".
    @Bean
    public Binding bindingGuardarImagenes(DirectExchange exchangeImagenes, Queue queueGuardarImagenes) {
        return BindingBuilder
                .bind(queueGuardarImagenes)
                .to(exchangeImagenes)
                .with(ROUTING_KEY_GUARDAR);
    }

    // Convierte los objetos Java a JSON al publicar y de JSON a Java al consumir.
    // Sin esto Rabbit serializa en binario y no se puede leer en el panel web.
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Configura el RabbitTemplate (el objeto que usas para publicar mensajes)
    // para que use el converter JSON que definimos arriba.
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}