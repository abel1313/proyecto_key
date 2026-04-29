package com.ventas.key.mis.productos.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ResultadoCargaDto {
    private int insertados;
    private int omitidos;
    private List<String> errores;
}