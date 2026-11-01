package com.ventas.key.hexagonal.revisionventas.dominio.excepcion;

/** Errores de la Revisión de ventas, con mensaje en palabras del dueño. */
public class RevisionVentasException extends RuntimeException {

    protected RevisionVentasException(String mensaje) {
        super(mensaje);
    }

    /** Las fechas pedidas no tienen sentido. */
    public static final class FechasInvalidas extends RevisionVentasException {
        public FechasInvalidas(String mensaje) {
            super(mensaje);
        }
    }

    /** Este tipo no se puede revisar en este ambiente (falta una tabla o columna). */
    public static final class RevisionNoDisponible extends RevisionVentasException {
        public RevisionNoDisponible(String mensaje) {
            super(mensaje);
        }
    }
}
