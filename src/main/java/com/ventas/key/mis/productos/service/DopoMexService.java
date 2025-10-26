package com.ventas.key.mis.productos.service;


import com.ventas.key.mis.productos.service.api.IDopoMexService;
import org.springframework.stereotype.Service;


@Service
public class DopoMexService implements IDopoMexService {

    private static final String API_KEY = "32512a7e5d843f4faa0b3e572a608c874d3076f7";
   // private WebClient webClient;

//    public DopoMexService(WebClient.Builder builder) {
//       // this.webClient = builder.baseUrl("https://api.tau.com.mx/dipomex/v1/").build(); // cambia la URL real
//    }

//    @Override
//    public Mono<CodigoPostalResponse> getCodigoPostal(String codigoPostal) {
////        return webClient.get()
////                .uri("/codigos-postales/{cp}", codigoPostal)
////                .header("APIKEY", API_KEY)
////                .retrieve()
////                .bodyToMono(CodigoPostalResponse.class);
//
//
//        return null;
//    }
}
