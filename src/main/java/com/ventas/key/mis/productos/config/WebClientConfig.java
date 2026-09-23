package com.ventas.key.mis.productos.config;

import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

// cambio de humo 2026-08-18: forzar rebuild/redeploy de QA para confirmar que el fix del
// Authorization duplicado (ver jwtHeaderFilter abajo) realmente llegue al pod.
@Configuration
public class WebClientConfig {

    /** Conectar a un servicio no deberia tardar mas que esto: o esta arriba, o no esta. */
    private static final int CONNECT_TIMEOUT_MS = 5_000;

    /**
     * Techo duro por request. Cubre la subida de imagenes, que es lo mas lento que se manda.
     * Un cliente que necesite mas puede definir su propio connector.
     */
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(35);

    /**
     * Los .timeout(...) de Reactor que ponga cada servicio NO alcanzan solos: si el otro extremo
     * acepta el socket y despues no contesta nunca, sin timeout a nivel HTTP el hilo del servlet
     * que hizo .block() se queda esperando para siempre. Como Tomcat tiene un pool finito de
     * hilos, unas cuantas peticiones asi dejan colgada la aplicacion entera -- no solo la
     * pantalla que pidio el dato. Esto le pone piso a todos los clientes a la vez.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(RESPONSE_TIMEOUT);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(jwtHeaderFilter());
    }

    private ExchangeFilterFunction jwtHeaderFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            // Si el caller ya puso su propio Authorization (FacebookGraphClient/InstagramGraphClient
            // usan "OAuth <pageAccessToken>", TikTokGraphClient usa "Bearer <accessToken>" -- son
            // credenciales de la API externa, no las nuestras), NO lo tocamos. ClientRequest.Builder
            // .header() agrega en vez de reemplazar, asi que sin este check el request salia con 2
            // headers Authorization y la API externa lo rechazaba (Facebook: "access token could not
            // be decrypted"; TikTok: 400 en su load balancer de borde antes de llegar a su API).
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
