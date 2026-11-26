package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

/** Lo que contestó Meta al intentar enviar. Un rechazo NUNCA se reintenta solo (regla R6). */
public sealed interface ResultadoEnvio {

    /** Código con el que Meta rechaza un mensaje libre fuera de la ventana de 24 h ("Re-engagement message"). */
    int CODIGO_FUERA_DE_VENTANA = 131047;

    record Enviado(String waMessageId) implements ResultadoEnvio {
    }

    /** {@code codigo} es el de Meta (null si ni siquiera se pudo hablar con Meta). */
    record Rechazado(Integer codigo, String motivo) implements ResultadoEnvio {

        public boolean esFueraDeVentana() {
            return codigo != null && codigo == CODIGO_FUERA_DE_VENTANA;
        }
    }
}
