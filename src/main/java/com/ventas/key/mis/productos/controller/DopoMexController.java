package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.dipomex.CodigoPostalResponse;
import com.ventas.key.mis.productos.service.api.IDopoMexService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("dipomex")
public class DopoMexController {

    private final IDopoMexService service;
    public DopoMexController(IDopoMexService service) {
        this.service = service;
    }

    @GetMapping("/getCodigoPostal/{codigoPostal}")
    public Mono<CodigoPostalResponse> getCodigoPostal(@PathVariable String codigoPostal) {
        return service.getCodigoPostal(codigoPostal);
    }
}
