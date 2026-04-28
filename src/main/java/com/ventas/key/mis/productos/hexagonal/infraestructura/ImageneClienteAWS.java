package com.ventas.key.mis.productos.hexagonal.infraestructura;

import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.hexagonal.dominio.Imagen;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenPort;
import com.ventas.key.mis.productos.hexagonal.infraestructura.dto.ImagenDto;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
@Service
@Slf4j
public class ImageneClienteAWS implements ImagenPort {

    @Value("${api.imagenes}")
    private @NotNull String endpointImg;

    private WebClient webClient;
    private final WebClient.Builder builder;

    public ImageneClienteAWS(WebClient.Builder builder) {
        this.builder = builder;
    }
    @PostConstruct
    public void init() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(config -> config.defaultCodecs().maxInMemorySize(40 * 1024 * 1024))
                .build();
        this.webClient = builder.baseUrl(endpointImg).exchangeStrategies(strategies).build();
        log.info(" endpoint imagenes ImageneClienteAWS {}", endpointImg);
    }


    @Override
    public List<ImagenDto> save(MultiValueMap<String, ?> multipartData) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String jwtToken = authentication.getCredentials().toString();
        return webClient.post()
                .uri("/imagenes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ".concat(jwtToken))
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
                }).toList())).block();
    }

    @Override
    public List<ImagenDto> getAll(List<Long> ids) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/imagenes")
                        .queryParam("ids", ids.toArray())
                        .build())
                .header(HttpHeaders.AUTHORIZATION, AuthenticationUtils.jwtBearerToken())
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
                .block();
    }

    @Override
    public ImagenDto getOne(Long id) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/imagenes")
                        .queryParam("ids", id)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, AuthenticationUtils.jwtBearerToken())
                .retrieve()
                .bodyToMono(Imagen.class).flatMap(mpa -> {
                    ImagenDto imagenDto = new ImagenDto();
                    imagenDto.setId(mpa.getId());
                    imagenDto.setImagen(mpa.getImagen());
                    return Mono.just(imagenDto);
                }).block();
    }
}
