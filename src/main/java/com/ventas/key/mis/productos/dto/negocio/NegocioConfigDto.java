package com.ventas.key.mis.productos.dto.negocio;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NegocioConfigDto {
    private boolean abierto;
    private String whatsappUrl;
    private String facebookUrl;
    private String horaApertura; // "HH:mm"
    private String horaCierre;   // "HH:mm"
}