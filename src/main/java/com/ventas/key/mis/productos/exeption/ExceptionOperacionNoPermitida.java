package com.ventas.key.mis.productos.exeption;

public class ExceptionOperacionNoPermitida extends RuntimeException {
    public ExceptionOperacionNoPermitida(String message) {
        super(message);
    }
}
