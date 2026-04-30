package com.ventas.key.mis.productos.dto.negocio;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NegocioEstadoDto {
    private boolean abierto;
    private String whatsappUrl;
    private String facebookUrl;
}