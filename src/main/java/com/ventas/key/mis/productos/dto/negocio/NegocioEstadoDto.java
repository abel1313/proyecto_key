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
    private String instagramUrl;
    private String tiktokUrl;
    private String horaApertura; // "HH:mm"
    private String horaCierre;   // "HH:mm"
    private String horaAperturaFinSemana; // "HH:mm" o null = igual que entre semana
    private String horaCierreFinSemana;   // "HH:mm" o null
}