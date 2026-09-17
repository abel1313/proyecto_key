package com.ventas.key.mis.productos.dto.qr;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QrDestinoCreateDto {
    private String nombre;
    private String url;
    private String descripcion;
    private String icono;
    /** Opcional — si no viene, se coloca al final de la lista. */
    private Integer orden;
}
