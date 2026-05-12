package com.ventas.key.mis.productos.dto;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class ClienteSinRegistroDto {

    private String nombre_persona;
    private String segundo_nombre;
    private String apeido_Paterno;
    private String apeido_Materno;
    private String fecha_Nacimiento;
    private String sexo;
    private String correo_Electronico;
    private String numero_Telefonico;

}
