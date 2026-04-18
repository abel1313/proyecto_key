package com.ventas.key.mis.productos.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;



@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class ConfigSocket implements WebSocketMessageBrokerConfigurer {

    @Value("api.cors_angular")
    private String corsAngular;
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("Se ejecutan los cors desde cofig socket {}",corsAngular);
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(corsAngular) // 🔥 Usa la URL específica de Angular
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // Canal donde los clientes recibirán mensajes
        registry.setApplicationDestinationPrefixes("/app"); // Prefijo para los mensajes de clientes
    }



}
