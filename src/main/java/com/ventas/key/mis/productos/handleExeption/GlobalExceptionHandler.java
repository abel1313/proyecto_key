package com.ventas.key.mis.productos.handleExeption;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GenericException.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(GenericException ex) {
        HttpStatus status = switch (ex.getCodigo()) {
            case 1062 -> HttpStatus.CONFLICT;
            case 500  -> HttpStatus.INTERNAL_SERVER_ERROR;
            default   -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status)
                .body(new ErrorResponse("Error " + ex.getCodigo(), ex.getMessage()));
    }

}
