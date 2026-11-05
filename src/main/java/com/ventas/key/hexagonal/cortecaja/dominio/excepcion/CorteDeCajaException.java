package com.ventas.key.hexagonal.cortecaja.dominio.excepcion;

/** Errores del corte de caja, en palabras del dueño. Se traducen a 400 en el controlador. */
public abstract class CorteDeCajaException extends RuntimeException {

    protected CorteDeCajaException(String mensaje) {
        super(mensaje);
    }

    public static class FechasInvalidas extends CorteDeCajaException {
        public FechasInvalidas(String mensaje) {
            super(mensaje);
        }
    }

    /** Salieron más pedidos de los que se pueden sumar de una vez. */
    public static class DemasiadosPedidos extends CorteDeCajaException {
        public DemasiadosPedidos(int maximo) {
            super("Son más de " + maximo + " pedidos. Acota las fechas o elige una zona.");
        }
    }
}
