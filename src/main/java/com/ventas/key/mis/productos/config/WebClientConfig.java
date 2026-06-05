package com.ventas.key.mis.productos.config;

import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .filter(jwtHeaderFilter());
    }

    private ExchangeFilterFunction jwtHeaderFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getCredentials() != null) {
                ClientRequest autenticado = ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, AuthenticationUtils.jwtBearerToken())
                        .build();
                return Mono.just(autenticado);
            }
            return Mono.just(request);
        });
    }
}
