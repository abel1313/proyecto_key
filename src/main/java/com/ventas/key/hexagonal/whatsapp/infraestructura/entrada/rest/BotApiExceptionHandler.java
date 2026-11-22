package com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapters]
 *
 * <p>Los controladores del bot ({@code /v1/bot/**}) responden con el estado que piden: 401 (llave del bot invalida), 503 (el servidor no tiene
 * {@code BOT_API_KEY}) o 400 (dato invalido). Sin esto, el manejador general {@code ExceptionGlobal} (que convierte cualquier
 * {@code RuntimeException} en 400) los volvia a todos 400, y el bot no podia distinguir "llave mala" de "mensaje mal armado". Solo aplica a
 * estos dos controladores y tiene prioridad sobre el general; el resto del back no cambia.
 */
@RestControllerAdvice(assignableTypes = {BotPublicacionesController.class, BotWhatsappController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BotApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> estado(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getReason() == null ? "" : e.getReason());
    }
}
