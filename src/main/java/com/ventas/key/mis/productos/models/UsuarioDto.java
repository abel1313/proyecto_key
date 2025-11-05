package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class UsuarioDto {
    private long idUsuario;
    private String nombre;

    public UsuarioDto(long idUsuario,String nombre){
        this.idUsuario = idUsuario;
        this.nombre = nombre;
    }
}
