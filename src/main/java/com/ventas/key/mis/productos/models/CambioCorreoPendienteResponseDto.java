package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CambioCorreoPendienteResponseDto {

    private boolean pendiente;
    private String correoPendiente;
    private LocalDateTime expiraEn;
}
