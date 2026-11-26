package com.ventas.key.hexagonal.whatsapp.dominio.excepcion;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.MensajeWhatsapp;

/**
 * El mensaje que escribió una persona desde la pantalla no salió. Ya quedó en el historial con su
 * motivo ({@link #registro()}); esta excepción solo le avisa a quien lo pidió.
 */
public class EnvioRechazadoException extends RuntimeException {

    private final transient MensajeWhatsapp registro;

    public EnvioRechazadoException(MensajeWhatsapp registro) {
        super(registro.motivo());
        this.registro = registro;
    }

    public MensajeWhatsapp registro() {
        return registro;
    }
}
