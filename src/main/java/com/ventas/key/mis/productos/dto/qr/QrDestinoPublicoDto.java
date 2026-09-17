package com.ventas.key.mis.productos.dto.qr;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QrDestinoPublicoDto {
    private Integer id;
    private String nombre;
    private String url;
    private String descripcion;
    private String icono;
}
