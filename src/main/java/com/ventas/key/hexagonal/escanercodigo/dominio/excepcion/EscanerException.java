package com.ventas.key.hexagonal.escanercodigo.dominio.excepcion;

/** Lo que el dominio del escáner remoto rechaza. Cada subclase sabe qué le pasó; el controller la traduce a HTTP. */
public class EscanerException extends RuntimeException {

    protected EscanerException(String mensaje) {
        super(mensaje);
    }

    /** La conexión con el celular no existe, ya venció o la PC la cerró. */
    public static class SesionNoDisponible extends EscanerException {
        public SesionNoDisponible() {
            super("La conexión con el celular ya no está activa. Vuelve a pulsar «Escanear con mi celular».");
        }
    }

    /** El texto escaneado no parece un código de barras. */
    public static class CodigoInvalido extends EscanerException {
        public CodigoInvalido() {
            super("Ese código no es válido. Tiene que tener de 3 a 100 caracteres, sin saltos de línea.");
        }
    }

    /** Se está pidiendo o mandando demasiado seguido. */
    public static class DemasiadasSolicitudes extends EscanerException {
        public DemasiadasSolicitudes(String mensaje) {
            super(mensaje);
        }
    }
}
