package com.ventas.key.mis.productos.dto.negocio;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HorarioUpdateDto {
    private String horaApertura; // "09:00"
    private String horaCierre;   // "21:00"
}