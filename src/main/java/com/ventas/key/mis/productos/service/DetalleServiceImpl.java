package com.ventas.key.mis.productos.service;

import org.springframework.stereotype.Service;

import com.ventas.key.mis.productos.entity.DetalleVenta;
import com.ventas.key.mis.productos.repository.IDetalleVentaRepository;

@Service
public class DetalleServiceImpl {

    private final IDetalleVentaRepository iRepository;
    public DetalleServiceImpl(
        final IDetalleVentaRepository iRepository
    ){
        this.iRepository = iRepository;
    }

    public DetalleVenta save(DetalleVenta detalle){
        return this.iRepository.save(detalle);
    }
}
