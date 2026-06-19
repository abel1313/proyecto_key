package com.ventas.key.mis.productos.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ConfigurarRifaPatchDto {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaHoraLimite;

    private ConfigurarRifa.TipoRifa tipo;

    private String mesReferencia;
}
