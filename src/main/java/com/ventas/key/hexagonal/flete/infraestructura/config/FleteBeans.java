package com.ventas.key.hexagonal.flete.infraestructura.config;

import com.ventas.key.hexagonal.flete.aplicacion.servicio.AdministrarViajesService;
import com.ventas.key.hexagonal.flete.dominio.puerto.entrada.AdministrarViajesCasoUso;
import com.ventas.key.hexagonal.flete.dominio.puerto.salida.ViajesPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Arma el caso de uso con su adaptador. El servicio es Java puro. */
@Configuration
public class FleteBeans {
    @Bean
    public AdministrarViajesCasoUso administrarViajesCasoUso(ViajesPort viajes) {
        return new AdministrarViajesService(viajes);
    }
}
