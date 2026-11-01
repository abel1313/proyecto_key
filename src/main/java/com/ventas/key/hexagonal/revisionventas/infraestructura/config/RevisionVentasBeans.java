package com.ventas.key.hexagonal.revisionventas.infraestructura.config;

import com.ventas.key.hexagonal.revisionventas.aplicacion.servicio.RevisarVentasService;
import com.ventas.key.hexagonal.revisionventas.dominio.puerto.entrada.RevisarVentasCasoUso;
import com.ventas.key.hexagonal.revisionventas.dominio.puerto.salida.HallazgosPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Arma el caso de uso de la Revisión de ventas con su adaptador. El servicio es Java puro. */
@Configuration
public class RevisionVentasBeans {

    @Bean
    public RevisarVentasCasoUso revisarVentasCasoUso(HallazgosPort hallazgos, Clock reloj) {
        return new RevisarVentasService(hallazgos, reloj);
    }
}
