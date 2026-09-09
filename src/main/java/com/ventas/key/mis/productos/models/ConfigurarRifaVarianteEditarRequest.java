package com.ventas.key.mis.productos.models;

import lombok.Getter;
import lombok.Setter;

/**
 * Edición de un premio YA guardado en una rifa. Antes solo existía
 * {@code PUT /{id}/palabraClave}, así que el número de giros en el que sale el ganador
 * quedaba congelado en el valor con el que se agregó el premio: para cambiarlo había que
 * eliminar el premio y volverlo a crear (devolviendo y volviendo a reservar stock).
 *
 * Todos los campos son opcionales -- solo se aplica lo que venga distinto de null.
 */
@Getter
@Setter
public class ConfigurarRifaVarianteEditarRequest {
    private Integer giroGanador;
    private Integer orden;
    private Boolean permitirNuevos;
    private String palabraClave;
    /** Cambiar el producto del premio: devuelve el stock del anterior y reserva el del nuevo. */
    private Integer varianteId;
}
