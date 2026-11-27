package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/** Un dato obligatorio del pedido que todavía no está (o no es válido). */
public enum Falta {
    ARTICULO("qué artículo quieres"),
    CANTIDAD("cuántas piezas quieres"),
    NOMBRE("tu nombre completo"),
    TELEFONO("un teléfono de 10 dígitos para contactarte"),
    CORREO("tu correo electrónico"),
    RECIBE("el nombre de quien recibe"),
    DIRECCION("la dirección de entrega"),
    LUGAR("el lugar de entrega (o si prefieres recoger en el local)"),
    LINEAS("menos artículos distintos (máximo " + ValidadorDeSolicitud.MAX_LINEAS + " por pedido; el resto va en otro pedido)");

    private final String texto;

    Falta(String texto) {
        this.texto = texto;
    }

    public String texto() {
        return texto;
    }
}
