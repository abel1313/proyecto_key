package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.dipomex.CodigoPostalResponse;
import reactor.core.publisher.Mono;

public interface IDopoMexService {
    Mono<CodigoPostalResponse> getCodigoPostal(String codigoPostal);
}
