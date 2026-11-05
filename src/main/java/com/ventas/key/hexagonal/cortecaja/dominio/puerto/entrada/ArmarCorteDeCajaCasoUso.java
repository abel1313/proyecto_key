package com.ventas.key.hexagonal.cortecaja.dominio.puerto.entrada;

import com.ventas.key.hexagonal.cortecaja.dominio.modelo.CorteDeCaja;
import com.ventas.key.hexagonal.cortecaja.dominio.modelo.FiltroDeCorte;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface ArmarCorteDeCajaCasoUso {

    /** Arma el corte con los pedidos que cumplen el filtro, agrupados por zona de entrega («Sin zona» al final). */
    CorteDeCaja armar(FiltroDeCorte filtro);
}
