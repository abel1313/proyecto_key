package com.ventas.key.hexagonal.reportecategoria.infraestructura.config;

import com.ventas.key.hexagonal.datoslegales.dominio.modelo.DatosLegales;
import com.ventas.key.hexagonal.datoslegales.dominio.puerto.entrada.DatosLegalesCasoUso;
import com.ventas.key.hexagonal.reportecategoria.aplicacion.servicio.ArmarReporteService;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.FiltroReporte;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.ReporteCalculado;
import com.ventas.key.hexagonal.reportecategoria.infraestructura.salida.archivo.EncabezadoReporte;
import com.ventas.key.hexagonal.reportecategoria.infraestructura.salida.archivo.ExcelReporteGenerador;
import com.ventas.key.hexagonal.reportecategoria.infraestructura.salida.archivo.PdfReporteGenerador;
import com.ventas.key.mis.productos.entity.ReporteGeneradoEntidad;
import com.ventas.key.mis.productos.repository.IReporteGeneradoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Arma el reporte EN SEGUNDO PLANO (pool propio, ver AsyncConfig): el administrador pide el reporte, la pantalla no espera y el archivo
 * terminado queda guardado en {@code reporte_generado}. No usa RabbitMQ: producción no lo tiene configurado.
 * Este es el único camino que GUARDA los cálculos de precio que faltaban (sin flete).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcesadorReporteAsincrono {

    private final IReporteGeneradoRepository repo;
    private final ArmarReporteService armar;
    private final ExcelReporteGenerador excel;
    private final PdfReporteGenerador pdf;
    private final DatosLegalesCasoUso datosLegales;

    @Async("reportesExecutor")
    public void procesar(int reporteId, FiltroReporte filtro, String formato, String usuario, String nombreArchivo) {
        ReporteGeneradoEntidad r = repo.findById(reporteId).orElse(null);
        if (r == null) return;
        try {
            r.setEstado("GENERANDO");
            repo.save(r);
            ReporteCalculado reporte = armar.armar(filtro, true, usuario);
            EncabezadoReporte encabezado = new EncabezadoReporte(EncabezadoReporte.NOMBRE_NEGOCIO, contactoDelNegocio(), usuario, LocalDateTime.now());
            byte[] bytes = "PDF".equals(formato) ? pdf.generar(reporte, encabezado) : excel.generar(reporte, encabezado);
            r.setContenido(bytes);
            r.setTamano((long) bytes.length);
            r.setNombreArchivo(nombreArchivo);
            r.setEstado("LISTO");
            r.setError(null);
        } catch (Exception e) {
            log.error("No se pudo generar el reporte {}: {}", reporteId, e.getMessage(), e);
            r.setEstado("ERROR");
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            r.setError(msg.length() > 480 ? msg.substring(0, 480) : msg);
        }
        r.setTerminado(LocalDateTime.now());
        repo.save(r);
    }

    /** Los datos del negocio que estén capturados en «Datos legales» (responsable, domicilio, teléfono, correo, horario). Si falla o no hay, va solo el nombre. */
    private List<String> contactoDelNegocio() {
        List<String> l = new ArrayList<>();
        try {
            DatosLegales d = datosLegales.consultar();
            if (d.nombreResponsable() != null) l.add(d.nombreResponsable());
            if (d.domicilio() != null) l.add(d.domicilio());
            if (d.telefono() != null) l.add("Tel. " + d.telefono());
            if (d.correo() != null) l.add(d.correo());
            if (d.horarioAtencion() != null) l.add("Horario: " + d.horarioAtencion());
        } catch (Exception e) {
            log.warn("El reporte sale sin datos del negocio: {}", e.getMessage());
        }
        return l;
    }
}
