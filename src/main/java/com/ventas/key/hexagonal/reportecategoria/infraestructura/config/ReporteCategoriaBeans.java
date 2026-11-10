package com.ventas.key.hexagonal.reportecategoria.infraestructura.config;

import com.ventas.key.hexagonal.reportecategoria.aplicacion.servicio.ArmarReporteService;
import com.ventas.key.hexagonal.reportecategoria.dominio.puerto.salida.CalculosVigentesPort;
import com.ventas.key.hexagonal.reportecategoria.dominio.puerto.salida.LectorVentasPort;
import com.ventas.key.hexagonal.reportecategoria.infraestructura.salida.archivo.ExcelReporteGenerador;
import com.ventas.key.hexagonal.reportecategoria.infraestructura.salida.archivo.PdfReporteGenerador;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReporteCategoriaBeans {

    @Bean
    public ArmarReporteService armarReporteService(LectorVentasPort lector, CalculosVigentesPort calculos) {
        return new ArmarReporteService(lector, calculos);
    }

    @Bean
    public ExcelReporteGenerador excelReporteGenerador() {
        return new ExcelReporteGenerador();
    }

    @Bean
    public PdfReporteGenerador pdfReporteGenerador() {
        return new PdfReporteGenerador();
    }
}
