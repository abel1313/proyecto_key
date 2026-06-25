package com.ventas.key.mis.productos.mapper;

import com.ventas.key.mis.productos.dto.GastosRequest;
import com.ventas.key.mis.productos.entity.Gastos;

import java.time.LocalDate;

public class GastosMapper {

    public static Gastos dtoToEntity(GastosRequest dto) {
        Gastos g = new Gastos();
        g.setDescripcion(dto.getDescripcion());
        g.setMonto(dto.getMonto());
        g.setFecha(dto.getFecha() != null ? dto.getFecha() : LocalDate.now());
        g.setCategoria(dto.getCategoria() != null ? dto.getCategoria() : Gastos.CategoriaGasto.OTROS);
        g.setProveedor(dto.getProveedor());
        g.setComprobante(dto.getComprobante());
        g.setNotas(dto.getNotas());
        return g;
    }
}
