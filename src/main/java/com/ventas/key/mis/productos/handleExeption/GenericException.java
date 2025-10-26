package com.ventas.key.mis.productos.handleExeption;

import lombok.Getter;

@Getter
public class GenericException extends RuntimeException {

    private final int codigo;
    public GenericException(int codigo,String mensaje){
        super(mensaje);
        this.codigo = codigo;
    }
}
