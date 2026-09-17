package com.ventas.key.mis.productos.dto.negocio;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContactosPublicosDto {
    private String whatsappUrl;
    private String facebookUrl;
    private String instagramUrl;
    private String tiktokUrl;
    private String direccion;
    private Double latitud;
    private Double longitud;
}
