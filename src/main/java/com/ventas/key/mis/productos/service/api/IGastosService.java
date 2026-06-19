package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.dto.GastosReporteDto;
import com.ventas.key.mis.productos.entity.Gastos;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PginaDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IGastosService extends ICrud<
        Gastos,
        List<Gastos>,
        Optional<Gastos>,
        Integer,
        PginaDto<List<Gastos>>> {

    Gastos saveGastosNT(Gastos gastos);

    PginaDto<List<Gastos>> buscar(LocalDate desde, LocalDate hasta,
                                   Gastos.CategoriaGasto categoria, int page, int size);

    Gastos editar(Integer id, Gastos datos) throws Exception;

    void eliminar(Integer id) throws Exception;

    GastosReporteDto reporte(LocalDate desde, LocalDate hasta);
}
