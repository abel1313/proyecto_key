package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.service.api.IDopoMexService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("dipomex")
public class DopoMexController {

    private IDopoMexService service;

//    @GetMapping(name = "getCodigoPostal/{codigoPostal}")
//    public Mono<CodigoPostalResponse> getCodigoPostal(String codigoPostal) {
//        return service.getCodigoPostal(codigoPostal);
//    }
}
