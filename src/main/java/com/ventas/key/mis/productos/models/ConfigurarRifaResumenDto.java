package com.ventas.key.mis.productos.models;

import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConfigurarRifaResumenDto {
    private Integer id;
    private LocalDateTime fechaHoraLimite;
    private Boolean activa;
    private int totalVariantes;
    private long variantesSorteadas;
    private ConfigurarRifa.TipoRifa tipo;
    private String mesReferencia;
    private Boolean esPrueba;

    // La ventana de boletos viaja en el resumen porque es lo que el listado de rifas
    // (/activas, /activas/hoy, /buscar) alimenta al front. Faltaban aquí, así que al
    // recargar la pantalla de rifa PLATAFORMAS la rifa volvía sin fechas y el wizard
    // pedía configurar el rango otra vez aunque ya estuviera guardado en la BD.
    private LocalDate fechaInicioBoletos;
    private LocalDate fechaFinBoletos;
}