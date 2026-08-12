package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.HistorialAcceso;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.dashboard.AccesoUsuarioDto;
import com.ventas.key.mis.productos.models.dashboard.DashboardResumenDto;
import com.ventas.key.mis.productos.repository.IGastosRepository;
import com.ventas.key.mis.productos.repository.IHistorialAccesoRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.repository.IVentaRepository;
import com.ventas.key.mis.productos.service.api.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private static final int UMBRAL_STOCK_BAJO = 5;

    private final IVentaRepository iVentaRepository;
    private final IPedidoRepository iPedidoRepository;
    private final IVarianteRepository iVarianteRepository;
    private final IGastosRepository iGastosRepository;
    private final IHistorialAccesoRepository iHistorialAccesoRepository;
    private final IUsuarioRepository iUsuarioRepository;

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

    @Override
    public PginaDto<List<AccesoUsuarioDto>> listarAccesos(LocalDate desde, LocalDate hasta, int pagina, int size) {
        LocalDateTime desdeDt = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDt = hasta != null ? hasta.atTime(23, 59, 59) : null;

        Page<HistorialAcceso> page = iHistorialAccesoRepository.buscarPorRangoFecha(
                desdeDt, hastaDt, PageRequest.of(pagina - 1, size));

        // Un solo findAllById para todos los usuarios de la pagina, en vez de una consulta por
        // fila -- mismo criterio que se aplico en el resto del proyecto en la sesion de N+1.
        List<Integer> usuarioIds = page.getContent().stream().map(HistorialAcceso::getUsuarioId).distinct().toList();
        Map<Integer, String> usernamesPorId = iUsuarioRepository.findAllById(usuarioIds).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getUsername));

        List<AccesoUsuarioDto> dtos = page.getContent().stream().map(h -> new AccesoUsuarioDto(
                h.getUsuarioId(),
                usernamesPorId.getOrDefault(h.getUsuarioId(), "(usuario eliminado)"),
                h.getFechaLogin(),
                h.getUltimaActividad(),
                Duration.between(h.getFechaLogin(), h.getUltimaActividad()).toMinutes()
        )).toList();

        PginaDto<List<AccesoUsuarioDto>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(dtos);
        return resultado;
    }
}
