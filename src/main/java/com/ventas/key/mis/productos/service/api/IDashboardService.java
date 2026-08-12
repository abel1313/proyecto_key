package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.dashboard.AccesoUsuarioDto;
import com.ventas.key.mis.productos.models.dashboard.DashboardResumenDto;

import java.time.LocalDate;
import java.util.List;

public interface IDashboardService {
    DashboardResumenDto resumen();

    /** desde/hasta null = sin filtro de fecha (todo el historico). */
    PginaDto<List<AccesoUsuarioDto>> listarAccesos(LocalDate desde, LocalDate hasta, int pagina, int size);
}
