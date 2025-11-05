package com.ventas.key.mis.productos.service;


import com.ventas.key.mis.productos.dipomex.CodigoPostalResponse;
import com.ventas.key.mis.productos.service.api.IDopoMexService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static io.jsonwebtoken.Jwts.header;


@Service
public class DopoMexService implements IDopoMexService {

    private static final String API_KEY = "32512a7e5d843f4faa0b3e572a608c874d3076f7";
    private static final String API_KEY_COPO_MEX = "0be7515c-3fcc-404f-a201-6cc0404d7574";
    private WebClient webClient;
    private WebClient webClientCopaMex;

    public DopoMexService(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://api.tau.com.mx/dipomex/v1/").build();
        this.webClientCopaMex = builder.baseUrl("https://api.copomex.com/query/").build();
    }

    @Override
    public Mono<CodigoPostalResponse> getCodigoPostal(String codigoPostal) {
        //Mono<Object> obj = getCodigoPostal2(codigoPostal);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("codigo_postal")
                        .queryParam("cp", codigoPostal)
                        .build())
                .header("APIKEY", API_KEY)
                .retrieve()
                .bodyToMono(CodigoPostalResponse.class);
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
