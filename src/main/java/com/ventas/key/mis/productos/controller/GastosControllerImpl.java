package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.dto.GastosRequest;
import com.ventas.key.mis.productos.entity.Gastos;
import com.ventas.key.mis.productos.mapper.GastosMapper;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.service.api.IGastosService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<Gastos> save(@RequestBody GastosRequest gastos ) throws Exception{
        return ResponseEntity.status(HttpStatus.OK).body(this.iService.save(GastosMapper.dtoToEntity(gastos)));
    }

    @GetMapping("/getGastos")
    public ResponseEntity<PginaDto<List<Gastos>>> getVentas(@RequestParam int size, 
                                                 @RequestParam int page) throws Exception{
        return ResponseEntity.status(HttpStatus.OK).body(this.iService.findAllNew(page,size) );
    }

}
