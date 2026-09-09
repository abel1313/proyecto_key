package com.ventas.key.mis.productos.dto.negocio;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RedSocialUpdateDto {
    private String nombre;
    private String url;
    private Boolean activo;
}
