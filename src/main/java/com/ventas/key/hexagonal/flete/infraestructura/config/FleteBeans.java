package com.ventas.key.hexagonal.flete.infraestructura.config;

import com.ventas.key.hexagonal.flete.aplicacion.servicio.AdministrarViajesService;
import com.ventas.key.hexagonal.flete.aplicacion.servicio.AsignarViajeAVentaService;
import com.ventas.key.hexagonal.flete.aplicacion.servicio.ConsultarReporteDeViajeService;
import com.ventas.key.hexagonal.flete.dominio.puerto.entrada.AdministrarViajesCasoUso;
import com.ventas.key.hexagonal.flete.dominio.puerto.entrada.ConsultarReporteDeViajeCasoUso;
import com.ventas.key.hexagonal.flete.dominio.puerto.salida.VentasDeViajePort;
import com.ventas.key.hexagonal.flete.dominio.puerto.entrada.AsignarViajeAVentaCasoUso;
import com.ventas.key.hexagonal.flete.dominio.puerto.salida.LigasDeVentaPort;
import com.ventas.key.hexagonal.flete.dominio.puerto.salida.ViajesPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Arma los casos de uso con su adaptador. Los servicios son Java puro. */
@Configuration
public class FleteBeans {
    @Bean
    public AdministrarViajesCasoUso administrarViajesCasoUso(ViajesPort viajes) {
        return new AdministrarViajesService(viajes);
    }

    @Bean
    public AsignarViajeAVentaCasoUso asignarViajeAVentaCasoUso(LigasDeVentaPort ligas) {
        return new AsignarViajeAVentaService(ligas);
    }

    @Bean
    public ConsultarReporteDeViajeCasoUso consultarReporteDeViajeCasoUso(ViajesPort viajes, VentasDeViajePort ventas) {
        return new ConsultarReporteDeViajeService(viajes, ventas);
    }
}
