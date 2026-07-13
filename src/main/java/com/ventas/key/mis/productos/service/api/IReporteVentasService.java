package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.models.reportes.ProductoMasVendidoDto;
import com.ventas.key.mis.productos.models.reportes.PromocionReporteDto;
import com.ventas.key.mis.productos.models.reportes.ReporteClienteDto;
import com.ventas.key.mis.productos.models.reportes.ReporteDiarioDto;
import com.ventas.key.mis.productos.models.reportes.ReporteMensualDto;

import java.time.LocalDate;
import java.util.List;

public interface IReporteVentasService {
    ReporteDiarioDto reporteDiario(LocalDate fecha);
    ReporteMensualDto reporteMensual(String mes);
    ReporteClienteDto reporteCliente(int clienteId);
    List<ProductoMasVendidoDto> productosMasVendidos(LocalDate desde, LocalDate hasta, int limite);
    List<PromocionReporteDto> reportePromociones(LocalDate desde, LocalDate hasta);
}
