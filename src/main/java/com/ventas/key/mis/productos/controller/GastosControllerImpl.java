package com.ventas.key.mis.productos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ventas.key.mis.productos.entity.Gastos;
import com.ventas.key.mis.productos.service.api.IGastosService;

@RestController
@RequestMapping("gastos")
public class GastosControllerImpl {


    private final IGastosService iService;
    public GastosControllerImpl(
        final IGastosService iService
    ){
        this.iService = iService;
    }

    @PostMapping("/save")
    public ResponseEntity<Gastos> save(@RequestBody Gastos gastos ) throws Exception{
        return ResponseEntity.status(HttpStatus.OK).body(this.iService.save(gastos) );
    }

    @GetMapping("/getGastos")
    public ResponseEntity<List<Gastos>> getVentas(@RequestParam int size, 
                                                 @RequestParam int page) throws Exception{
        return ResponseEntity.status(HttpStatus.OK).body(this.iService.findAll(page,size) );
    }

}
