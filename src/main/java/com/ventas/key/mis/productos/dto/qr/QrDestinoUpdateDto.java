package com.ventas.key.mis.productos.dto.qr;

import lombok.*;

/** Todos los campos son opcionales: los que lleguen en null se dejan como estaban. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QrDestinoUpdateDto {
    private String nombre;
    private String url;
    private String descripcion;
    private String icono;
    private Boolean activo;
    private Integer orden;
}
