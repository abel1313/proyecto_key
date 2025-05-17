package com.ventas.key.mis.productos.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("productos-eje/")
public class PruebaControllerImpl {


    @GetMapping("data")
    public String getData(){
        return "nada";
    }

}
