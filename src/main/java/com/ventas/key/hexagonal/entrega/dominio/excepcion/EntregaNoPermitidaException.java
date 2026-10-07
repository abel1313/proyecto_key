package com.ventas.key.hexagonal.entrega.dominio.excepcion;

/**
 * El negocio no deja entregar (o regresar) el pedido. Se responde 400 con este mensaje.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 */
public class EntregaNoPermitidaException extends RuntimeException {
    public EntregaNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
