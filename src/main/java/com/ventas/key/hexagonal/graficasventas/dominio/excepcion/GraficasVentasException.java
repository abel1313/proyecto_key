package com.ventas.key.hexagonal.graficasventas.dominio.excepcion;

/** Datos pedidos que no tienen sentido, con mensaje en palabras del dueño. */
public class GraficasVentasException extends RuntimeException {

    private GraficasVentasException(String mensaje) {
        super(mensaje);
    }

    public static GraficasVentasException fechasInvalidas() {
        return new GraficasVentasException("La fecha «desde» no puede ser después de «hasta».");
    }

    public static GraficasVentasException mesesInvalidos(int minimo, int maximo) {
        return new GraficasVentasException("Los meses a mostrar deben ser de " + minimo + " a " + maximo + ".");
    }

    public static GraficasVentasException limiteInvalido(int minimo, int maximo) {
        return new GraficasVentasException("La cantidad de productos a mostrar debe ser de " + minimo + " a " + maximo + ".");
    }

    public static GraficasVentasException diasInvalidos(int minimo, int maximo) {
        return new GraficasVentasException("Los días a revisar deben ser de " + minimo + " a " + maximo + ".");
    }
}
