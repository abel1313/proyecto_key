package com.ventas.key.mis.productos.hexagonal.infraestructura;

import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.config.RabbitMQConfig;
import com.ventas.key.mis.productos.hexagonal.dominio.Imagen;
import com.ventas.key.mis.productos.hexagonal.dominio.ProductoImagen;
import com.ventas.key.mis.productos.hexagonal.dominio.mapper.RequestProductoImagen;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenProductoPort;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class ImagenProductoClienteMicro implements ImagenProductoPort {

    @Value("${api.imagenes}")
    private @NotNull String endpointImg;

    private WebClient webClient;
    private final WebClient.Builder builder;
    private final RabbitTemplate rabbitTemplate;

    public ImagenProductoClienteMicro(WebClient.Builder builder, RabbitTemplate rabbitTemplate) {
        this.builder = builder;
        this.rabbitTemplate = rabbitTemplate;
    }
    @PostConstruct
    public void init() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        String baseUrl = endpointImg.endsWith("/") ? endpointImg : endpointImg + "/";
        this.webClient = builder.baseUrl(baseUrl).exchangeStrategies(strategies).build();
        log.info(" endpoint imagenes ImagenProductoClienteMicro {}", endpointImg);
    }

    @Override
    public ResponseGeneric<ProductoImagen> save(RequestProductoImagen requestProductoImagen) {
        return webClient.post()
                .uri("/imagenes") // ajusta la ruta de tu endpoint
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(requestProductoImagen), RequestProductoImagen.class)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ResponseGeneric<ProductoImagen>>() {})
                .block(); // aquí obtienes el resultado sincrónicamente
    }


    @Override
    public void saveAll(List<RequestProductoImagen> requestProductoImagen) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_IMAGENES,
                RabbitMQConfig.ROUTING_KEY_GUARDAR,
                requestProductoImagen);
        log.info("Relaciones producto-imagen publicadas a Rabbit — {} relaciones", requestProductoImagen.size());
    }

    @Override
    public ResponseGeneric<ProductoImagen> update(RequestProductoImagen requestProductoImagen) throws Exception {
        return webClient.put()
                .uri("/imagenes") // ajusta la ruta de tu endpoint
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(requestProductoImagen), RequestProductoImagen.class)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ResponseGeneric<ProductoImagen>>() {})
                .block(); // aquí obtienes el resultado sincrónicamente
    }

    // BUG CONOCIDO: .uri("/imagenes/", id) no interpola el parámetro — el DELETE iría a /imagenes/ sin ID.
    // Debe corregirse a .uri("/imagenes/{id}", id) antes de activar este método.
    // Sin callers activos al 2026-05-23 — no ejecutar hasta corregir.
    @Override
    public ResponseGeneric<ProductoImagen> update(Integer id) throws Exception {
        return webClient.delete()
                .uri("/imagenes/", id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ResponseGeneric<ProductoImagen>>() {})
                .block();
    }

    @Override
    public ResponseGeneric<ProductoImagen> findById(Integer id) throws Exception {
        return webClient.get()
                .uri("/imagenes/", id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ResponseGeneric<ProductoImagen>>() {})
                .block(); // aquí obtienes el resultado sincrónicamente
    }

    @Override
    @Cacheable(value = "buscarImagenIdCache", key = "#id")
    public Imagen buscarImagenProducto(Integer id) {
        log.info("micro imagenes, buscar imagen por ID {}",id);
        return webClient.get()
                .uri("/producto-imagen/buscarImagenProducto/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, AuthenticationUtils.jwtBearerToken())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Imagen>() {})
                .block();
    }
}
