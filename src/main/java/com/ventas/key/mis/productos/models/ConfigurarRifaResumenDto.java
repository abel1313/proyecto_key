package com.ventas.key.mis.productos.models;

import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}