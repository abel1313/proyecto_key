package com.ventas.key.mis.productos.hexagonal.infraestructura;

import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.hexagonal.dominio.Imagen;
import com.ventas.key.mis.productos.hexagonal.dominio.ProductoImagen;
import com.ventas.key.mis.productos.hexagonal.dominio.mapper.RequestProductoImagen;
import com.ventas.key.mis.productos.hexagonal.dominio.port.out.ImagenProductoPort;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
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

    public ImagenProductoClienteAWS(WebClient.Builder builder) {
        this.builder = builder;
    }
    @PostConstruct
    public void init() {
        this.webClient = builder.baseUrl(endpointImg).build();
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String jwtToken = authentication.getCredentials().toString();
        return webClient.post()
                .uri("/producto-imagen/saveAll") // ajusta la ruta de tu endpoint
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, AuthenticationUtils.jwtBearerToken())
                .body(Mono.just(requestProductoImagen), RequestProductoImagen.class)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ResponseGeneric<ProductoImagen>>() {})
                .block(); // aquí obtienes el resultado sincrónicamente
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
    public Imagen buscarImagenProducto(Integer id) throws Exception {
        return webClient.get()
                .uri("/producto-imagen/buscarImagenProducto/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, AuthenticationUtils.jwtBearerToken())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Imagen>() {})
                .block(); // aquí obtienes el resultado sincrónicamente
    }
}
