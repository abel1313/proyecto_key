package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.Venta;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.reportes.ProductoMasVendidoDto;
import com.ventas.key.mis.productos.models.reportes.PromocionReporteDto;
import com.ventas.key.mis.productos.models.reportes.ReporteClienteDto;
import com.ventas.key.mis.productos.models.reportes.ReporteDiarioDto;
import com.ventas.key.mis.productos.models.reportes.ReporteMensualDto;
import com.ventas.key.mis.productos.models.reportes.VentaResumenItem;
import com.ventas.key.mis.productos.repository.IClienteRepository;
import com.ventas.key.mis.productos.repository.IDetalleVentaVarianteRepository;
import com.ventas.key.mis.productos.repository.IPromocionRepository;
import com.ventas.key.mis.productos.repository.IVentaRepository;
import com.ventas.key.mis.productos.service.api.IReporteVentasService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteVentasServiceImpl implements IReporteVentasService {

    private final IVentaRepository iVentaRepository;
    private final IDetalleVentaVarianteRepository iDetalleVentaVarianteRepository;
    private final IClienteRepository iClienteRepository;
    private final IPromocionRepository iPromocionRepository;

    @Override
    public ReporteDiarioDto reporteDiario(LocalDate fecha) {
        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.atTime(23, 59, 59);
        Object[] fila = iVentaRepository.sumVentas(desde, hasta);
        return new ReporteDiarioDto(
                fecha,
                fila[0] != null ? (Double) fila[0] : 0.0,
                fila[1] != null ? (Double) fila[1] : 0.0,
                (Long) fila[2]);
    }

    @Override
    public ReporteMensualDto reporteMensual(String mes) {
        YearMonth ym = YearMonth.parse(mes, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDateTime desde = ym.atDay(1).atStartOfDay();
        LocalDateTime hasta = ym.atEndOfMonth().atTime(23, 59, 59);

        Object[] totales = iVentaRepository.sumVentas(desde, hasta);
        List<Object[]> filasPorDia = iVentaRepository.sumVentasPorDia(desde, hasta);

        List<ReporteDiarioDto> porDia = new ArrayList<>();
        for (Object[] fila : filasPorDia) {
            porDia.add(new ReporteDiarioDto(
                    ((java.sql.Date) fila[0]).toLocalDate(),
                    fila[1] != null ? (Double) fila[1] : 0.0,
                    fila[2] != null ? (Double) fila[2] : 0.0,
                    (Long) fila[3]));
        }

        return new ReporteMensualDto(
                mes,
                totales[0] != null ? (Double) totales[0] : 0.0,
                totales[1] != null ? (Double) totales[1] : 0.0,
                (Long) totales[2],
                porDia);
    }

    @Override
    public ReporteClienteDto reporteCliente(int clienteId) {
        Cliente cliente = iClienteRepository.findById(clienteId)
                .orElseThrow(() -> new ExceptionDataNotFound("Cliente no encontrado: " + clienteId));

        List<Venta> ventas = iVentaRepository.findByClienteIdOrderByFechaVentaDesc(clienteId);

        List<VentaResumenItem> items = ventas.stream()
                .map(v -> new VentaResumenItem(v.getId(), v.getFechaVenta(), v.getTotalVenta(), v.getGananciaTotal()))
                .toList();

        double totalGastado = ventas.stream()
                .mapToDouble(v -> v.getTotalVenta() != null ? v.getTotalVenta() : 0.0)
                .sum();

        return new ReporteClienteDto(clienteId, cliente.getNombrePersona(), (long) ventas.size(), totalGastado, items);
    }

    @Override
    public List<ProductoMasVendidoDto> productosMasVendidos(LocalDate desde, LocalDate hasta, int limite) {
        List<Object[]> filas = iDetalleVentaVarianteRepository.productosMasVendidos(
                desde, hasta, PageRequest.of(0, limite));

        return filas.stream()
                .map(f -> new ProductoMasVendidoDto(
                        (Integer) f[0],
                        (String) f[1],
                        (String) f[2],
                        (String) f[3],
                        (Long) f[4],
                        (Double) f[5]))
                .toList();
    }

    @Override
    public List<PromocionReporteDto> reportePromociones(LocalDate desde, LocalDate hasta) {
        List<Object[]> filas = iPromocionRepository.reportePromociones(desde, hasta);

        return filas.stream()
                .map(f -> new PromocionReporteDto(
                        (Integer) f[0],
                        (String) f[1],
                        ((Number) f[2]).longValue(),
                        ((Number) f[3]).longValue(),
                        ((Number) f[4]).doubleValue(),
                        ((Number) f[5]).doubleValue(),
                        f[6] != null ? ((Date) f[6]).toLocalDate() : null))
                .toList();
    }
}
