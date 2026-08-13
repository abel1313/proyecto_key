package com.ventas.key.mis.productos.models.floreseternas;

public final class FloresEternasConstantes {

    // Umbral de la regla del papel: con MAS de esta cantidad de flores, el papel es obligatorio
    // y se cobra automatico (no se pregunta). Con esta cantidad o menos, el papel es opcional.
    public static final int UMBRAL_PAPEL_OBLIGATORIO = 10;

    // Anticipo requerido cuando el liston lleva una frase personalizada pendiente de validar.
    public static final double PORCENTAJE_ANTICIPO_FRASE_PERSONALIZADA = 0.5;

    public static final String AVISO_NO_REEMBOLSO =
            "Este ramo incluye una frase personalizada pendiente de validar. Se requiere un "
          + "anticipo del 50% para producirlo. Una vez entregado el ramo no hay reembolsos ni "
          + "cancelaciones.";

    private FloresEternasConstantes() {
    }
}
