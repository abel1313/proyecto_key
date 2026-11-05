package com.ventas.key.hexagonal.cortecaja.dominio.puerto.entrada;

import com.ventas.key.hexagonal.cortecaja.dominio.modelo.CorteDeCaja;

/** [Hexagonal: Driving Port] [Clean: Use Case Input Boundary] */
public interface ArmarCorteDeCajaCasoUso {

    /**
     * Arma el corte: los pedidos sin entregar y con algo por cobrar, agrupados por zona de entrega.
     *
     * @param zonaId si viene, solo esa zona; {@code null} = todas (con «Sin zona» al final)
     */
    CorteDeCaja armar(Integer zonaId);
}
