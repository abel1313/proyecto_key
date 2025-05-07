package com.ventas.key.mis.productos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ProductoDTO;
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
    public ResponseEntity<PginaDto<List<ProductoDTO>>> getProductos2(@RequestParam int size, @RequestParam int page) throws Exception{
        return ResponseEntity.status(HttpStatus.OK).body(this.pServiceImpl.getAll(size,page) );
    }

    @GetMapping("buscarNombreOrCodigoBarra")
    public ResponseEntity<PginaDto<List<ProductoDTO>>> buscarNombreOrCodigoBarra(@RequestParam int size, 
                                                                                @RequestParam int page,
                                                                                @RequestParam String nombre) throws Exception{
        return ResponseEntity.status(HttpStatus.OK).body(this.pServiceImpl.findNombreOrCodigoBarra(size,page,nombre) );
    }
}
