package com.ventas.key.mis.productos.service;


import com.ventas.key.mis.productos.dipomex.CodigoPostalResponse;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionServicioExterno;
import com.ventas.key.mis.productos.service.api.IDopoMexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;


@Slf4j
@Service
public class DopoMexService implements IDopoMexService {

    /** Lo más que se espera al servicio de códigos postales antes de decirle al cliente que lo escriba a mano. */
    private static final Duration TIEMPO_MAXIMO = Duration.ofSeconds(8);

    private static final String API_KEY = "32512a7e5d843f4faa0b3e572a608c874d3076f7";
    private static final String API_KEY_COPO_MEX = "0be7515c-3fcc-404f-a201-6cc0404d7574";
    private WebClient webClient;
    private WebClient webClientCopaMex;

    public DopoMexService(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://api.tau.com.mx/dipomex/v1/").build();
        this.webClientCopaMex = builder.baseUrl("https://api.copomex.com/query/").build();
    }

    /**
     * Municipio y colonias de un código postal (Mis datos y ver cliente lo usan para llenar el municipio).
     *
     * QA 2026-10-14: si el servicio externo no contestaba o decía "no existe", aquí se regresaba
     * tal cual (o un 500 sin explicación) y la pantalla se quedaba sin llenar el municipio y sin
     * decir nada. Ahora: un código que no son 5 dígitos o que no existe → 404 con el motivo; el
     * servicio externo caído, lento o rechazando la llave → 502, y el log dice el código HTTP que
     * contestó (nunca la llave) para saber si hay que renovarla.
     */
    @Override
    public Mono<CodigoPostalResponse> getCodigoPostal(String codigoPostal) {
        if (codigoPostal == null || !codigoPostal.matches("\\d{5}")) {
            return Mono.error(new ExceptionDataNotFound("El código postal debe tener 5 dígitos"));
        }
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("codigo_postal")
                        .queryParam("cp", codigoPostal)
                        .build())
                .header("APIKEY", API_KEY)
                .retrieve()
                .bodyToMono(CodigoPostalResponse.class)
                .timeout(TIEMPO_MAXIMO)
                .flatMap(r -> {
                    if (r == null || r.isError() || r.getCodigo_postal() == null
                            || r.getCodigo_postal().getMunicipio() == null) {
                        return Mono.<CodigoPostalResponse>error(
                                new ExceptionDataNotFound("No encontramos el código postal " + codigoPostal));
                    }
                    return Mono.just(r);
                })
                .onErrorMap(e -> !(e instanceof ExceptionDataNotFound), e -> {
                    if (e instanceof WebClientResponseException w) {
                        log.warn("DIPOMEX contestó {} al buscar el código postal {}", w.getStatusCode().value(), codigoPostal);
                    } else {
                        log.warn("DIPOMEX no contestó al buscar el código postal {}: {}", codigoPostal, e.toString());
                    }
                    return new ExceptionServicioExterno(
                            "No pudimos buscar el código postal en este momento. Escribe el municipio a mano");
                });
    }


    public Mono<Object> getCodigoPostal2(String codigoPostal) {
        return webClientCopaMex.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/info_cp/" + codigoPostal)
                        .queryParam("token", API_KEY_COPO_MEX)
                        .build())
                .retrieve()
                .bodyToMono(Object.class); // Puedes reemplazar Object con tu clase modelo si ya la tienes

    }
}
