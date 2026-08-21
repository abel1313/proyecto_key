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
            // Si el caller ya puso su propio Authorization (credenciales de una API externa, no
            // las nuestras), NO lo tocamos. ClientRequest.Builder.header() agrega en vez de
            // reemplazar, asi que sin este check el request salia con 2 headers Authorization y
            // la API externa lo rechazaba.
            if (request.headers().containsKey(HttpHeaders.AUTHORIZATION)) {
                return Mono.just(request);
            }
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
