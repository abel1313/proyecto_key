package com.ventas.key.mis.productos.models;

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
}