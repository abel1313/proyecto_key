package com.ventas.key.hexagonal.graficasventas.infraestructura.config;

import com.ventas.key.hexagonal.graficasventas.aplicacion.servicio.ConsultarGraficasService;
import com.ventas.key.hexagonal.graficasventas.dominio.puerto.entrada.ConsultarGraficasCasoUso;
import com.ventas.key.hexagonal.graficasventas.dominio.puerto.salida.GraficasPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Arma el caso de uso de las gráficas con su adaptador. El servicio es Java puro. */
@Configuration
public class GraficasVentasBeans {

    @Bean
    public ConsultarGraficasCasoUso consultarGraficasCasoUso(GraficasPort puerto, Clock reloj) {
        return new ConsultarGraficasService(puerto, reloj);
    }
}
