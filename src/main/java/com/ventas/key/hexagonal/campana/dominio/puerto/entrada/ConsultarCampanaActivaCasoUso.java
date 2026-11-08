package com.ventas.key.hexagonal.campana.dominio.puerto.entrada;

import com.ventas.key.hexagonal.campana.dominio.modelo.Campana;

import java.util.Optional;

/** [Hexagonal: Driving Port] La campaña que está vigente hoy (encendida y dentro de sus fechas), si hay. Solo lee. */
public interface ConsultarCampanaActivaCasoUso {

    Optional<Campana> vigente();
}
