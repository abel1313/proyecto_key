package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.dto.GastosReporteDto;
import com.ventas.key.mis.productos.entity.Gastos;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IGastosRepository;
import com.ventas.key.mis.productos.repository.IVentaRepository;
import com.ventas.key.mis.productos.service.api.IGastosService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GastosServiceImpl extends CrudAbstractServiceImpl<
        Gastos, List<Gastos>, Optional<Gastos>, Integer, PginaDto<List<Gastos>>>
        implements IGastosService {

    private final IGastosRepository iRepository;
    private final IVentaRepository iVentaRepository;

    public GastosServiceImpl(final IGastosRepository iRepository,
                             final IVentaRepository iVentaRepository,
                             final ErrorGenerico error) {
        super(iRepository, error);
        this.iRepository = iRepository;
        this.iVentaRepository = iVentaRepository;
    }

    @Override
    public Gastos saveGastosNT(Gastos gastos) {
        return iRepository.save(gastos);
    }

    @Override
    public PginaDto<List<Gastos>> buscar(LocalDate desde, LocalDate hasta,
                                          Gastos.CategoriaGasto categoria, int page, int size) {
        LocalDate desdeFecha = desde != null ? desde : LocalDate.now();
        LocalDate hastaFecha = hasta != null ? hasta : desdeFecha;

        Page<Gastos> resultado = iRepository.buscar(desdeFecha, hastaFecha, categoria,
                PageRequest.of(page, size));

        PginaDto<List<Gastos>> dto = new PginaDto<>();
        dto.setT(resultado.getContent());
        dto.setTotalRegistros((int) resultado.getTotalElements());
        dto.setTotalPaginas(resultado.getTotalPages());
        dto.setPagina(page);
        return dto;
    }

    @Override
    public Gastos editar(Integer id, Gastos datos) throws Exception {
        Gastos existente = iRepository.findById(id)
                .orElseThrow(() -> new Exception("Gasto no encontrado"));

        if (datos.getDescripcion() != null) existente.setDescripcion(datos.getDescripcion());
        if (datos.getMonto() != null) existente.setMonto(datos.getMonto());
        if (datos.getFecha() != null) existente.setFecha(datos.getFecha());
        if (datos.getCategoria() != null) existente.setCategoria(datos.getCategoria());
        if (datos.getProveedor() != null) existente.setProveedor(datos.getProveedor());
        if (datos.getComprobante() != null) existente.setComprobante(datos.getComprobante());
        if (datos.getNotas() != null) existente.setNotas(datos.getNotas());

        return iRepository.save(existente);
    }

    @Override
    public void eliminar(Integer id) throws Exception {
        if (!iRepository.existsById(id)) {
            throw new Exception("Gasto no encontrado");
        }
        iRepository.deleteById(id);
    }

    @Override
    public GastosReporteDto reporte(LocalDate desde, LocalDate hasta) {
        LocalDate desdeFecha = desde != null ? desde : LocalDate.now().withDayOfMonth(1);
        LocalDate hastaFecha = hasta != null ? hasta : LocalDate.now();

        // Gastos
        Double totalGastos = iRepository.sumTotal(desdeFecha, hastaFecha);
        totalGastos = totalGastos != null ? totalGastos : 0.0;

        Map<String, Double> porCategoria = new HashMap<>();
        for (Object[] row : iRepository.sumPorCategoria(desdeFecha, hastaFecha)) {
            String cat = row[0] != null ? row[0].toString() : "OTROS";
            Double suma = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            porCategoria.put(cat, suma);
        }

        // Ventas
        LocalDateTime desdeTs = desdeFecha.atStartOfDay();
        LocalDateTime hastaTs = hastaFecha.atTime(LocalTime.MAX);
        Object[] ventasTotales = iVentaRepository.sumVentas(desdeTs, hastaTs);

        Double totalVentas = ventasTotales[0] != null ? ((Number) ventasTotales[0]).doubleValue() : 0.0;
        Double totalGanancia = ventasTotales[1] != null ? ((Number) ventasTotales[1]).doubleValue() : 0.0;
        Long totalTx = ventasTotales[2] != null ? ((Number) ventasTotales[2]).longValue() : 0L;

        GastosReporteDto dto = new GastosReporteDto();
        dto.setFechaInicio(desdeFecha);
        dto.setFechaFin(hastaFecha);
        dto.setTotalVentas(totalVentas);
        dto.setTotalGananciaProductos(totalGanancia);
        dto.setTotalTransacciones(totalTx);
        dto.setTotalGastos(totalGastos);
        dto.setGastosPorCategoria(porCategoria);
        dto.setGananciaNeta(totalGanancia - totalGastos);
        return dto;
    }
}
