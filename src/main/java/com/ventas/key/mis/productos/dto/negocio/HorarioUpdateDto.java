package com.ventas.key.mis.productos.dto.negocio;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HorarioUpdateDto {
    private String horaApertura; // "09:00"
    private String horaCierre;   // "21:00"
    /** Sabado y domingo. Si llegan ("" en los dos = quitar y volver a igual que entre semana). Null = no se tocan. */
    private String horaAperturaFinSemana;
    private String horaCierreFinSemana;
}