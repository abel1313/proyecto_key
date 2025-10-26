package com.ventas.key.mis.productos.handleExeption;

public class ErrorResponse {

    private String error;
    private String detalle;

    public ErrorResponse(String error, String detalle) {
        this.error = error;
        this.detalle = detalle;
    }
}
