package com.ventas.key.mis.productos.mapper;

import com.ventas.key.mis.productos.dto.GastosRequest;
import com.ventas.key.mis.productos.entity.Gastos;

public class GastosMapper {

    public static Gastos dtoToEntity(GastosRequest gastos){
        Gastos  gastosEntity = new Gastos();
        gastosEntity.setId(gastos.getId());
        gastosEntity.setDescripcionGasto(gastos.getDescripcionGasto());
        gastosEntity.setPrecioGasto(gastos.getPrecioGasto());
        return gastosEntity;
    }
}
