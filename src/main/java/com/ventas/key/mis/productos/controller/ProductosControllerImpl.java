package com.ventas.key.mis.productos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.service.ProductosServiceImpl;

@RestController
@RequestMapping("productos")
public class ProductosControllerImpl {

    private final ProductosServiceImpl pServiceImpl;
    public ProductosControllerImpl(
        final ProductosServiceImpl pServiceImpl
    ){
        this.pServiceImpl = pServiceImpl;
    }


    @GetMapping("getProductos2")
    public ResponseEntity<List<Producto>> getProductos2() throws Exception{
        return ResponseEntity.status(HttpStatus.OK).body(this.pServiceImpl.findAll(0,0) );
    }
}
