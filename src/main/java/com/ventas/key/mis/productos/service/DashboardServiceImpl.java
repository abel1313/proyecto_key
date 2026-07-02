package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.models.dashboard.DashboardResumenDto;
import com.ventas.key.mis.productos.repository.IGastosRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.repository.IVentaRepository;
import com.ventas.key.mis.productos.service.api.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private static final int UMBRAL_STOCK_BAJO = 5;

    private final IVentaRepository iVentaRepository;
    private final IPedidoRepository iPedidoRepository;
    private final IVarianteRepository iVarianteRepository;
    private final IGastosRepository iGastosRepository;

    @Override
    public DashboardResumenDto resumen() {
        LocalDate hoy = LocalDate.now();

        Object[] filaHoy = iVentaRepository.sumVentas(hoy.atStartOfDay(), hoy.atTime(23, 59, 59));
        Double ventasHoy = filaHoy[0] != null ? ((Number) filaHoy[0]).doubleValue() : 0.0;

        LocalDate primerDiaMes = hoy.withDayOfMonth(1);
        LocalDate ultimoDiaMes = hoy.withDayOfMonth(hoy.lengthOfMonth());
        LocalDateTime desdeMes = primerDiaMes.atStartOfDay();
        LocalDateTime hastaMes = ultimoDiaMes.atTime(23, 59, 59);

        Object[] filaMes = iVentaRepository.sumVentas(desdeMes, hastaMes);
        Double ventasMes = filaMes[0] != null ? ((Number) filaMes[0]).doubleValue() : 0.0;
        Double gananciaMes = filaMes[1] != null ? ((Number) filaMes[1]).doubleValue() : 0.0;

        Double gastosMes = iGastosRepository.sumTotal(primerDiaMes, ultimoDiaMes);
        if (gastosMes == null) gastosMes = 0.0;

        Double montoPorCobrar = iPedidoRepository.sumMontoPorCobrar();
        if (montoPorCobrar == null) montoPorCobrar = 0.0;

        return new DashboardResumenDto(
                ventasHoy,
                ventasMes,
                gananciaMes,
                gastosMes,
                gananciaMes - gastosMes,
                iPedidoRepository.countPendientesEntregar(),
                iPedidoRepository.countCreditosActivos(),
                montoPorCobrar,
                iVarianteRepository.countStockBajo(UMBRAL_STOCK_BAJO));
    }
}
