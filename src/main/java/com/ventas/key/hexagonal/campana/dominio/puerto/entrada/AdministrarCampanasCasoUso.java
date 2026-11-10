package com.ventas.key.hexagonal.campana.dominio.puerto.entrada;

import com.ventas.key.hexagonal.campana.dominio.modelo.Campana;

import java.util.List;

/** [Hexagonal: Driving Port] Lo que el administrador hace con las campañas. */
public interface AdministrarCampanasCasoUso {

    List<Campana> listar();

    Campana obtener(int id);

    Campana crear(Campana campana);

    /** Reemplaza nombre, fechas, meta y TODA la lista de precios; no cambia si está encendida (para eso están activar y apagar). */
    Campana actualizar(int id, Campana campana);

    void eliminar(int id);

    /** Enciende esta campaña y apaga cualquier otra. */
    Campana activar(int id);

    Campana apagar(int id);

    /** Una copia apagada («copia de …») con los mismos productos y precios, para armar el siguiente evento. */
    Campana duplicar(int id);
}
