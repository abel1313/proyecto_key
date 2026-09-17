package com.ventas.key.mis.productos.hexagonal.infraestructura;

import com.ventas.key.mis.productos.config.RabbitMQConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.hexagonal.dominio.Imagen;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenPort;
import com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import io.netty.channel.ChannelOption;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
@Service
@Slf4j
public class ImageneClienteDisco implements ImagenPort {

    @Value("${api.imagenes}")
    private @NotNull String endpointImg;

    private WebClient webClient;
    private final WebClient.Builder builder;
    private final RabbitTemplate rabbitTemplate;

    public ImageneClienteDisco(WebClient.Builder builder, RabbitTemplate rabbitTemplate) {
        this.builder = builder;
        this.rabbitTemplate = rabbitTemplate;
    }
    /** Conectar al micro no deberia tardar mas que esto: o esta arriba, o no esta. */
    private static final int CONNECT_TIMEOUT_MS = 5_000;

    /**
     * Techo duro por request. Subir imagenes manda archivos, asi que se toma el mas largo de los
     * timeouts de Reactor que usan los metodos de abajo (30 s en save) y se deja ese como limite.
     */
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(35);

    @PostConstruct
    public void init() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(config -> config.defaultCodecs().maxInMemorySize(40 * 1024 * 1024))
                .build();

        // Los .timeout(...) de Reactor de cada metodo NO alcanzan solos: si el micro acepta el
        // socket y despues no contesta nunca, sin timeout a nivel HTTP el hilo del servlet que
        // hizo .block() se queda esperando para siempre, y como Tomcat tiene un pool finito de
        // hilos, unas cuantas peticiones asi dejan colgada toda la aplicacion -- no solo la
        // pantalla que pidio la imagen. Esto le pone piso a eso.
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(RESPONSE_TIMEOUT);

        this.webClient = builder
                .baseUrl(endpointImg)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
        log.info(" endpoint imagenes ImageneClienteDisco {}", endpointImg);
    }


    @Override
    public List<ImagenDto> save(MultiValueMap<String, ?> multipartData) {
        try {
            return webClient.post()
                    .uri("/v1/imagenes")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(multipartData))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Imagen>>() {
                    }).flatMap(flat-> Mono.just(flat.stream().map(mpa->{
                        ImagenDto imagenDto = new ImagenDto();
                        imagenDto.setId(mpa.getId());
                        imagenDto.setNombreImagen(mpa.getNombreImagen());
                        imagenDto.setContentType(mpa.getContentType());
                        imagenDto.setImagen(mpa.getImagen());
                        return imagenDto;
                    }).toList())).timeout(Duration.ofSeconds(30)).block();
        } catch (WebClientResponseException e) {
            // Sin esto el admin veia "400 Bad Request from POST .../v1/imagenes" y nada mas: el
            // motivo real lo manda el micro en el body (su validador de subida explica si la
            // extension no coincide con los bytes, si pesa de mas, etc.) y se perdia aqui.
            String detalle = mensajeDelMicro(e.getResponseBodyAsString());
            log.error("El micro de imagenes rechazo la subida ({}): {}", e.getStatusCode(), detalle);
            throw new ExceptionErrorInesperado(detalle);
        }
    }

    /** Saca el campo message del MensajeError del micro; si no se puede, devuelve el body crudo. */
    private String mensajeDelMicro(String body) {
        if (body == null || body.isBlank()) {
            return "El servicio de imagenes rechazo la subida y no dio detalle.";
        }
        try {
            JsonNode mensaje = new ObjectMapper().readTree(body).get("message");
            if (mensaje != null && !mensaje.asText().isBlank()) {
                return mensaje.asText();
            }
        } catch (Exception ignored) {
            // body que no es el JSON esperado: se devuelve tal cual, es mejor que nada
        }
        return body;
    }

    @Override
    public List<ImagenDto> getAll(List<Long> ids) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/imagenes")
                        .queryParam("ids", ids.toArray())
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Imagen>>() {
                }).flatMap(flat -> Mono.just(flat.stream().map(mpa -> {
                    ImagenDto imagenDto = new ImagenDto();
                    imagenDto.setId(mpa.getId());
                    imagenDto.setImagen(mpa.getImagen());
                    return imagenDto;
                }).toList()))
                .doOnError(e -> log.warn("Error obteniendo imágenes del microservicio para ids=[{}]: {}", ids.toArray(), e.getMessage(), e))
                .onErrorReturn(List.of())
                .timeout(Duration.ofSeconds(5))
                .block();
    }

    @Override
    public void delete(List<Long> ids) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_IMAGENES,
                RabbitMQConfig.ROUTING_KEY_ELIMINAR,
                ids
        );
        log.info("Publicados {} IDs a eliminar a Rabbit (queue.eliminar.imagenes)", ids.size());
    }

    @Override
    public List<Long> verificarExistentes(List<Long> ids) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/imagenes/verificar")
                        .queryParam("ids", ids.toArray())
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Long>>() {})
                // El .timeout() va ANTES del .onErrorReturn(): al reves, la TimeoutException se
                // levantaba despues del fallback y salia como excepcion en vez de degradar a
                // lista vacia, que es justo lo que este metodo promete a quien lo llama.
                // Un 204 del micro deja el Mono vacio y .block() devolveria null, de ahi el
                // defaultIfEmpty: esto nunca devuelve null ni lanza.
                .timeout(Duration.ofSeconds(5))
                .doOnError(e -> log.warn("Error verificando imágenes ids=[{}]: {}", ids, e.getMessage()))
                .onErrorReturn(List.of())
                .defaultIfEmpty(List.of())
                .block();
    }

    @Override
    public void deleteInagenesDisco(List<String> ids) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_IMAGENES,
                RabbitMQConfig.ROUTING_KEY_ELIMINAR_DISCO,
                ids
        );
        log.info("Publicados {} nombres a eliminar del disco a Rabbit (queue.eliminar.imagenes.disco)", ids.size());
    }

    @Override
    public ImagenDto getOne(Long id) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/imagenes")
                        .queryParam("ids", id)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Imagen>>() {})
                .flatMap(list -> {
                    if (list == null || list.isEmpty()) return Mono.empty();
                    Imagen mpa = list.get(0);
                    ImagenDto imagenDto = new ImagenDto();
                    imagenDto.setId(mpa.getId());
                    imagenDto.setImagen(mpa.getImagen());
                    return Mono.just(imagenDto);
                }).timeout(Duration.ofSeconds(5)).block();
    }
}
