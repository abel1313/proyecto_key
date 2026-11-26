package com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.PlantillasPropuestas;

import java.util.List;

/**
 * Las plantillas propuestas para Meta. {@code puedeEnviarPlantillas} es false mientras la cuenta no tenga
 * método de pago válido: la pantalla debe deshabilitar el botón y decir por qué.
 */
public record PlantillasResponse(boolean puedeEnviarPlantillas, List<Item> plantillas) {

    public record Item(String nombre, String idioma, String categoria, String cuerpo, List<String> variables, String pie) {
    }

    public static PlantillasResponse de(boolean puedeEnviarPlantillas) {
        return new PlantillasResponse(puedeEnviarPlantillas, PlantillasPropuestas.TODAS.stream()
                .map(p -> new Item(p.nombre(), p.idioma(), p.categoria(), p.cuerpo(), p.variables(), p.pie()))
                .toList());
    }
}
