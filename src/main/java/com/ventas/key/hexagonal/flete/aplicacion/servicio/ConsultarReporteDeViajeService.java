package com.ventas.key.hexagonal.flete.aplicacion.servicio;

import com.ventas.key.hexagonal.flete.dominio.excepcion.ViajeInvalidoException;
import com.ventas.key.hexagonal.flete.dominio.modelo.ReporteDeViaje;
import com.ventas.key.hexagonal.flete.dominio.puerto.entrada.ConsultarReporteDeViajeCasoUso;
import com.ventas.key.hexagonal.flete.dominio.puerto.salida.VentasDeViajePort;
import com.ventas.key.hexagonal.flete.dominio.puerto.salida.ViajesPort;

/** [Hexagonal: Application] [Clean: Use Case] Junta el viaje con sus ventas y deja que el dominio calcule. Java puro. */
public class ConsultarReporteDeViajeService implements ConsultarReporteDeViajeCasoUso {

    private final ViajesPort viajes;
    private final VentasDeViajePort ventas;

    public ConsultarReporteDeViajeService(ViajesPort viajes, VentasDeViajePort ventas) {
        this.viajes = viajes;
        this.ventas = ventas;
    }

    @Override
    public ReporteDeViaje reporte(int viajeId) {
        var viaje = viajes.buscar(viajeId).orElseThrow(() -> new ViajeInvalidoException("Ese viaje ya no existe."));
        return ReporteDeViaje.calcular(viaje, ventas.deViaje(viajeId));
    }
}
