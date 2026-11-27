package com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.HorarioDeAtencion;

import java.util.Optional;

/** El horario en que una persona atiende. Vacío si el negocio no tiene uno configurado. [Hexagonal: Driven Port] */
public interface HorarioDeAtencionPort {

    Optional<HorarioDeAtencion> actual();
}
