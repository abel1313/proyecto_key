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
public class ImagenProductoClienteAWS implements ImagenProductoPort {

    @Value("${api.imagenes}")
    private @NotNull String endpointImg;

    private WebClient webClient;
    private final WebClient.Builder builder;
    private final RabbitTemplate rabbitTemplate;

    public ImagenProductoClienteAWS(WebClient.Builder builder, RabbitTemplate rabbitTemplate) {
        this.builder = builder;
        this.rabbitTemplate = rabbitTemplate;
    }
    @PostConstruct
    public void init() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.webClient = builder.baseUrl(endpointImg).exchangeStrategies(strategies).build();
        log.info(" endpoint imagenes ImagenProductoClienteAWS {}", endpointImg);
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
    public ResponseGeneric<ProductoImagen> saveAll(List<RequestProductoImagen> requestProductoImagen) {
        // Publica el mensaje a RabbitMQ en vez de llamar HTTP directamente.
        // Si el micro de imágenes está caído, el mensaje queda en la cola y
        // se procesa cuando levante. No se pierde la referencia.
        log.info("Publicando {} imagenes a Rabbit cola={}", requestProductoImagen.size(), RabbitMQConfig.QUEUE_GUARDAR);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_IMAGENES,    // a qué exchange
                RabbitMQConfig.ROUTING_KEY_GUARDAR,  // con qué routing key
                requestProductoImagen                // el objeto (se serializa a JSON)
        );
        return null;
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

    @Override
    public ResponseGeneric<ProductoImagen> update(Integer id) throws Exception {
        return webClient.delete()
                .uri("/imagenes/", id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ResponseGeneric<ProductoImagen>>() {})
                .block(); // aquí obtienes el resultado sincrónicamente
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
