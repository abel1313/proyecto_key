package com.ventas.key.hexagonal.pagotarjeta.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lo que el front necesita de un cobro. Sin datos internos (id de la pasarela, quien lo inicio, version).
 *
 * @param estado       codigo estable para la logica de la pantalla (CREADO, ENVIADO, APROBADO, RECHAZADO, CANCELADO,
 *                     EXPIRADO, COBRADO_SIN_APLICAR, DEVUELTO_PARCIAL, DEVUELTO)
 * @param estadoTexto  el estado en palabras del dueño, para mostrarlo tal cual
 * @param enProceso    true mientras sigue esperando respuesta: la pantalla debe seguir consultando
 * @param venceEn      hasta cuando espera respuesta (UTC)
 */
public record CobroResponse(Long id, Integer pedidoId, String canal, BigDecimal monto, int cuotas, String estado,
                            String estadoTexto, boolean enProceso, Instant venceEn, BigDecimal devuelto,
                            BigDecimal restanteADevolver) {

    public static CobroResponse de(IntentoDePago i) {
        return new CobroResponse(i.id(), i.pedidoId(), i.canal().name(), i.monto(), i.cuotas(), i.estado().name(),
                i.estado().etiqueta(), i.estado().estaActivo(), i.creadoEn().plus(i.canal().vigencia()), i.devuelto(),
                i.restanteADevolver());
    }
}
