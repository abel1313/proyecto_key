package com.ventas.key.mis.productos.models.resenas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResenaResumenDto {
    private Integer varianteId;
    private Double promedio;
    private Long totalResenas;
    private Map<Integer, Long> conteoPorEstrella;
}
