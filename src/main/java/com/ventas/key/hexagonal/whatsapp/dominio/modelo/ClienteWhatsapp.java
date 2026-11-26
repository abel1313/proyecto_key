package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.time.Duration;
import java.time.Instant;

/**
 * Un cliente (número) con quien hay conversación. {@code ultimoEntranteAt} es la marca de su último
 * mensaje entrante, la que abre la ventana de 24 h (regla R1).
 *
 * @param pausaBotDesde   desde cuándo una persona atiende (se reinicia con cada respuesta suya, regla R10)
 * @param botFijoHumano   la conversación se tomó a mano: el bot no contesta hasta que se la devuelvan
 * @param requiereAtencion hay algo que una persona debe revisar (escalado, respuesta pendiente, error de envío)
 */
public record ClienteWhatsapp(long id, String telefono, String nombre, Instant ultimoEntranteAt,
                              Instant ultimaActividadAt, Instant pausaBotDesde, boolean botFijoHumano,
                              boolean requiereAtencion, String ultimoMensaje, Instant creadoAt) {

    public boolean botPausado(Instant ahora, Duration pausa) {
        if (botFijoHumano) {
            return true;
        }
        return pausaBotDesde != null && ahora.isBefore(pausaBotDesde.plus(pausa));
    }

    public boolean ventanaAbierta(Instant ahora) {
        return VentanaDe24Horas.abierta(ultimoEntranteAt, ahora);
    }
}
