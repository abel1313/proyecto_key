package com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida;

import java.time.LocalDateTime;

/**
 * [Hexagonal: Driven Port] Las rifas que necesita una prueba (R23), dadas de alta con los mismos servicios que
 * la pantalla Rifas → Agregar rifa: la configuración y el premio (que aparta 1 pieza del artículo).
 */
public interface RifasPruebaPort {

    /**
     * Una rifa diaria de prueba ("es prueba" prendido) cuya descripción es {@code nombre}, con un premio (el
     * artículo y su palabra clave) y el plazo de registro indicado. Un plazo ya vencido deja la rifa activa
     * (se puede abrir desde Rifas → Buscar) pero no acepta participantes nuevos. Regresa el número de la rifa.
     */
    int rifaConPremio(String nombre, int articuloId, String palabraClave, LocalDateTime plazoDeRegistro);

    /**
     * Desactiva las rifas de prueba de esa prueba que sigan activas (descripción "QA Prueba {prueba} …") y
     * regresa a su artículo la pieza apartada de cada premio, como al vencer una rifa de prueba. Nunca se
     * borran. Regresa cuántas desactivó.
     */
    int quitarRifasDeLaPrueba(String prueba);
}
