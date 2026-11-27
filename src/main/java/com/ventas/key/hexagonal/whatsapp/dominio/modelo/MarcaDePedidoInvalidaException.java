package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/** La marca {@code ##PEDIDO{...}##} vino mal escrita (JSON roto o sin cerrar). No se crea nada. */
public class MarcaDePedidoInvalidaException extends RuntimeException {
    public MarcaDePedidoInvalidaException(String mensaje) {
        super(mensaje);
    }
}
