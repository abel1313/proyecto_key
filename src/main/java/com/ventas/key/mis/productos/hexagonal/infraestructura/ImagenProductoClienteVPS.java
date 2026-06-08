package com.ventas.key.mis.productos.hexagonal.infraestructura;

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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class ImagenProductoClienteVPS implements ImagenProductoPort {

    @Value("${api.imagenes}")
    private @NotNull String endpointImg;

    private WebClient webClient;
    private final WebClient.Builder builder;
    private final RabbitTemplate rabbitTemplate;

    public ImagenProductoClienteVPS(WebClient.Builder builder, RabbitTemplate rabbitTemplate) {
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
        log.info(" endpoint imagenes ImagenProductoClienteVPS {}", endpointImg);
    }

    @Override
    public ResponseGeneric<ProductoImagen> save(RequestProductoImagen requestProductoImagen) {
        return webClient.post()
                .uri("/imagenes") // ajusta la ruta de tu endpoint
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(requestProductoImagen), RequestProductoImagen.class)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ResponseGeneric<ProductoImagen>>() {})
                .timeout(Duration.ofSeconds(5))
                .block(); // aquí obtienes el resultado sincrónicamente
    }


    @Override
    public void saveAll(List<RequestProductoImagen> requestProductoImagen) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_IMAGENES,
                RabbitMQConfig.ROUTING_KEY_GUARDAR,
                requestProductoImagen
        );
        log.info("Publicadas {} relaciones producto-imagen a Rabbit (queue.guardar.imagenes)", requestProductoImagen.size());
    }

    @Override
    public ResponseGeneric<ProductoImagen> update(RequestProductoImagen requestProductoImagen) throws Exception {
        return webClient.put()
                .uri("/imagenes") // ajusta la ruta de tu endpoint
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(requestProductoImagen), RequestProductoImagen.class)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ResponseGeneric<ProductoImagen>>() {})
                .timeout(Duration.ofSeconds(5))
                .block(); // aquí obtienes el resultado sincrónicamente
    }

    @Override
    public ResponseGeneric<ProductoImagen> update(Integer id) throws Exception {
        throw new UnsupportedOperationException("update por HTTP no implementado — usar Rabbit");
    }

    @Override
    public ResponseGeneric<ProductoImagen> findById(Integer id) throws Exception {
        return webClient.get()
                .uri("/imagenes/", id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ResponseGeneric<ProductoImagen>>() {})
                .timeout(Duration.ofSeconds(5))
                .block(); // aquí obtienes el resultado sincrónicamente
    }

    @Override
    @Cacheable(value = "buscarImagenIdCache", key = "#id")
    public Imagen buscarImagenProducto(Integer id) {
        log.info("micro imagenes, buscar imagen por ID {}",id);
        return webClient.get()
                .uri("/producto-imagen/buscarImagenProducto/{id}", id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Imagen>() {})
                .timeout(Duration.ofSeconds(5))
                .block();
    }
}
