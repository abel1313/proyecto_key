package com.ventas.key.mis.productos.exeption;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@ToString
public class MensajeError {

    private int code;
    private String message;
    private String fecha;

}
