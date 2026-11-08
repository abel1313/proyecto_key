package com.ventas.key.hexagonal.campana.infraestructura.config;

import com.ventas.key.hexagonal.campana.aplicacion.servicio.AdministrarCampanasService;
import com.ventas.key.hexagonal.campana.aplicacion.servicio.ConsultarCampanaActivaService;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.AdministrarCampanasCasoUso;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ConsultarCampanaActivaCasoUso;
import com.ventas.key.hexagonal.campana.aplicacion.servicio.LimiteDeIntentos;
import com.ventas.key.hexagonal.campana.aplicacion.servicio.ParticiparEnCampanaService;
import com.ventas.key.hexagonal.campana.aplicacion.servicio.ConsultarReporteDeCampanaService;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ConsultarReporteDeCampanaCasoUso;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ParticiparEnCampanaCasoUso;
import com.ventas.key.hexagonal.campana.dominio.puerto.salida.AvisarCodigoCampanaPort;
import com.ventas.key.hexagonal.campana.dominio.puerto.salida.CampanasPort;
import com.ventas.key.hexagonal.campana.dominio.puerto.salida.PersonasCampanaPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

/** Arma los casos de uso con su adaptador. Los servicios son Java puro. Su propio reloj: producción no tiene un bean Clock global. */
@Configuration
public class CampanaBeans {

    @Bean
    public AdministrarCampanasCasoUso administrarCampanasCasoUso(CampanasPort campanas) {
        return new AdministrarCampanasService(campanas);
    }

    @Bean
    public ConsultarCampanaActivaCasoUso consultarCampanaActivaCasoUso(CampanasPort campanas) {
        return new ConsultarCampanaActivaService(campanas, Clock.systemDefaultZone());
    }

    /** Máximo 15 intentos de «Verificar» por usuario cada 10 minutos: frena el adivinar códigos. */
    @Bean
    public ParticiparEnCampanaCasoUso participarEnCampanaCasoUso(ConsultarCampanaActivaCasoUso activa, PersonasCampanaPort personas,
                                                                 AvisarCodigoCampanaPort aviso) {
        return new ParticiparEnCampanaService(activa, personas, aviso,
                new LimiteDeIntentos(Clock.systemDefaultZone(), 15, Duration.ofMinutes(10)), () -> UUID.randomUUID().toString());
    }

    @Bean
    public ConsultarReporteDeCampanaCasoUso consultarReporteDeCampanaCasoUso(CampanasPort campanas, PersonasCampanaPort personas) {
        return new ConsultarReporteDeCampanaService(campanas, personas);
    }
}
