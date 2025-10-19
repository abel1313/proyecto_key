package com.ventas.key.mis.productos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ventas.key.mis.productos.entity.LotesProductos;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ProductoDTO;
import com.ventas.key.mis.productos.models.ProductoDetalle;
import com.ventas.key.mis.productos.service.ProductosServiceImpl;
import com.ventas.key.mis.productos.service.api.ILoteProductoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("productos")
@RequiredArgsConstructor
public class ProductosControllerImpl {

    private final ProductosServiceImpl pServiceImpl;
    private final ILoteProductoService iLoteProductoService;
    


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

    @PostMapping("save")
    public ResponseEntity<Producto> save(@RequestBody ProductoDetalle producto) throws Exception{
        return ResponseEntity.status(HttpStatus.OK).body(this.pServiceImpl.saveProductoLote(producto) );
    }
    @PutMapping("save")
    public ResponseEntity<Producto> update(@RequestBody ProductoDetalle producto) throws Exception{
        return ResponseEntity.status(HttpStatus.OK).body(this.pServiceImpl.saveProductoLote(producto) );
    }
}
