package com.ventas.key.hexagonal.campana.aplicacion.servicio;

import com.ventas.key.hexagonal.campana.dominio.modelo.Campana;
import com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ConsultarCampanaActivaCasoUso;
import com.ventas.key.hexagonal.campana.dominio.puerto.salida.CampanasPort;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

/** [Clean: Use Case] La campaña vigente hoy. Si por error hubiera más de una encendida, gana la que empezó más tarde. Java puro. */
public class ConsultarCampanaActivaService implements ConsultarCampanaActivaCasoUso {

    private final CampanasPort campanas;
    private final Clock reloj;

    public ConsultarCampanaActivaService(CampanasPort campanas, Clock reloj) {
        this.campanas = campanas;
        this.reloj = reloj;
    }

    @Override
    public Optional<Campana> vigente() {
        LocalDate hoy = LocalDate.now(reloj);
        return campanas.activas().stream()
                .filter(c -> c.vigenteEn(hoy))
                .max(java.util.Comparator.comparing(Campana::fechaInicio));
    }
}
