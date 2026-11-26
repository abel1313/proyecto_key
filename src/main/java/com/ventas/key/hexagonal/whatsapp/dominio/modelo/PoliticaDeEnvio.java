package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.time.Instant;

/**
 * Reglas R1 a R3 de {@code whatsapp/README.md}: qué se puede mandar y cuándo. Todo envío pasa por
 * aquí antes de hablar con Meta; la decisión nunca se le deja a la IA.
 */
public final class PoliticaDeEnvio {

    public static final String MOTIVO_FUERA_DE_VENTANA =
            "Fuera de la ventana de 24 h desde el último mensaje del cliente: solo se puede enviar una plantilla aprobada";
    public static final String MOTIVO_SIN_PAGO =
            "La cuenta de WhatsApp no tiene un método de pago válido en Meta: no se puede enviar una plantilla";

    private PoliticaDeEnvio() {
    }

    /** Texto, imagen, enlace o botones: solo con la ventana abierta. */
    public static DecisionDeEnvio paraMensajeLibre(Instant ultimoEntrante, Instant ahora) {
        return VentanaDe24Horas.abierta(ultimoEntrante, ahora)
                ? new DecisionDeEnvio.Enviar()
                : new DecisionDeEnvio.NoEnviar(EstadoMensaje.BLOQUEADO_VENTANA, MOTIVO_FUERA_DE_VENTANA);
    }

    /** Una plantilla, dentro o fuera de la ventana: solo si la cuenta ya tiene método de pago válido. */
    public static DecisionDeEnvio paraPlantilla(boolean metodoDePagoActivo) {
        return metodoDePagoActivo
                ? new DecisionDeEnvio.Enviar()
                : new DecisionDeEnvio.NoEnviar(EstadoMensaje.BLOQUEADO_SIN_PAGO, MOTIVO_SIN_PAGO);
    }
}
