package com.ventas.key.mis.productos.models.pedidos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class ProgramarEntregaZonaRequest {
    /** Fecha en la que se entregará -- es la que va en el correo al cliente. */
    private LocalDate fecha;
    private String hora;
    private String puntoEncuentro;

    /**
     * Rango de FECHA DE PEDIDO que se estaba viendo en pantalla al programar.
     *
     * Va en el request a propósito: antes el back recalculaba la semana en curso por su cuenta,
     * así que si la pantalla mostraba otro rango se les mandaba el correo a un conjunto de
     * pedidos distinto del que el admin tenía a la vista. Mandando el mismo rango que se listó,
     * se avisa exactamente a los pedidos que se vieron. Si vienen null, se usa la semana en
     * curso como antes.
     */
    private LocalDate desde;
    private LocalDate hasta;
}
