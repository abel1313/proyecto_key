package com.ventas.key.mis.productos.dto.negocio;

import lombok.*;

/** Lo que manda Configuracion del negocio al guardar la ubicacion del local. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UbicacionUpdateDto {
    private String direccion;
    private Double latitud;
    private Double longitud;
}
