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

import com.ventas.key.mis.productos.entity.Venta;
import com.ventas.key.mis.productos.models.DetalleVentaDto;
import com.ventas.key.mis.productos.models.TotalDetalle;
import com.ventas.key.mis.productos.service.VentaServiceImpl;

@RestController
@RequestMapping("ventas")
public class VentaControllerImpl {


    private final VentaServiceImpl vImpl;
    public VentaControllerImpl(
        final VentaServiceImpl vImpl
    ){
        this.vImpl = vImpl;
    }
    @PostMapping("/save")
    public ResponseEntity<Venta> save(@RequestBody List<DetalleVentaDto> lista ) throws Exception{
        return ResponseEntity.status(HttpStatus.OK).body(this.vImpl.saveVentaDetalle(lista) );
    }
    @PostMapping("/getVentas")
    public ResponseEntity<List<Venta>> getVentas(@RequestParam int size, 
                                            @RequestParam int page,
                                            @RequestParam String nombre ) throws Exception{
        return ResponseEntity.status(HttpStatus.OK).body(this.vImpl.findAll(page,size) );
    }
        @GetMapping("/getTotalVentas")
    public ResponseEntity<List<TotalDetalle>> getTotalVentas() throws Exception{
        return ResponseEntity.status(HttpStatus.OK).body(this.vImpl.getTotalDetalle() );
    }
}
