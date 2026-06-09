package com.ventas.key.mis.productos.exeption;

public class ExceptionStockInsuficiente extends RuntimeException {

    public ExceptionStockInsuficiente(String mensaje) {
        super(mensaje);
    }
}