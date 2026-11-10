package com.ventas.key.hexagonal.reportecategoria.aplicacion.servicio;

import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.CalculoVigente;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.FilaBase;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.FilaReporte;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.FiltroReporte;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.NuevoCalculo;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.ReglasReporte;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.ReporteCalculado;
import com.ventas.key.hexagonal.reportecategoria.dominio.puerto.salida.CalculosVigentesPort;
import com.ventas.key.hexagonal.reportecategoria.dominio.puerto.salida.LectorVentasPort;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Arma el reporte: lee los productos con lo vendido, les pone el porcentaje del último cálculo guardado (o lo calcula sin flete si no
 * tenían) y suma los totales. Con {@code guardarCalculos} deja guardados los cálculos que no existían (los reportes generados en segundo
 * plano); la consulta en pantalla nunca escribe nada.
 */
public class ArmarReporteService {

    private final LectorVentasPort lector;
    private final CalculosVigentesPort calculos;

    public ArmarReporteService(LectorVentasPort lector, CalculosVigentesPort calculos) {
        this.lector = lector;
        this.calculos = calculos;
    }

    public ReporteCalculado armar(FiltroReporte filtro, boolean guardarCalculos, String usuario) {
        List<FilaBase> bases = lector.leer(filtro).stream().filter(b -> ReglasReporte.pasaVista(filtro.vista(), b)).toList();
        Map<Integer, CalculoVigente> vigentes = calculos.ultimos(bases.stream().map(FilaBase::productoId).toList());

        List<FilaReporte> filas = new ArrayList<>();
        List<NuevoCalculo> nuevos = new ArrayList<>();
        for (FilaBase b : bases) {
            CalculoVigente c = vigentes.get(b.productoId());
            BigDecimal margen;
            BigDecimal flete;
            boolean nuevo = false;
            boolean sinFlete;
            if (c != null) {
                margen = c.margenPct();
                flete = c.transporte();
                sinFlete = c.transporte() == null || c.transporte().signum() == 0;
            } else {
                margen = ReglasReporte.margenSobreVenta(b.precioCompra(), b.precioVenta());
                flete = BigDecimal.ZERO;
                sinFlete = true;
                nuevo = margen != null;
                if (nuevo && b.precioCompra().signum() > 0) {
                    nuevos.add(new NuevoCalculo(b.productoId(), b.precioCompra(), b.precioVenta(), margen));
                }
            }
            filas.add(new FilaReporte(b, margen, nuevo, flete == null ? BigDecimal.ZERO : flete, sinFlete,
                    ReglasReporte.margenReal(b.ganancia(), b.total()),
                    ReglasReporte.gananciaPorVender(b.stock(), b.precioCompra(), b.precioVenta())));
        }
        if (guardarCalculos && !nuevos.isEmpty()) calculos.guardarGenerados(nuevos, usuario);

        filas.sort(Comparator.comparingLong((FilaReporte f) -> f.base().piezas()).reversed()
                .thenComparing(f -> f.base().nombre() == null ? "" : f.base().nombre(), String.CASE_INSENSITIVE_ORDER));
        return new ReporteCalculado(filtro, filas, ReglasReporte.totales(filas));
    }
}
