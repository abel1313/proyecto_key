package com.ventas.key.hexagonal.cortecaja.infraestructura.config;

import com.ventas.key.hexagonal.cortecaja.aplicacion.servicio.ArmarCorteDeCajaService;
import com.ventas.key.hexagonal.cortecaja.dominio.puerto.entrada.ArmarCorteDeCajaCasoUso;
import com.ventas.key.hexagonal.cortecaja.dominio.puerto.salida.PedidosAbiertosPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Arma el caso de uso del corte de caja con su adaptador. El servicio es Java puro. */
@Configuration
public class CorteDeCajaBeans {

    @Bean
    public ArmarCorteDeCajaCasoUso armarCorteDeCajaCasoUso(PedidosAbiertosPort pedidos, Clock reloj) {
        return new ArmarCorteDeCajaService(pedidos, reloj);
    }
}
