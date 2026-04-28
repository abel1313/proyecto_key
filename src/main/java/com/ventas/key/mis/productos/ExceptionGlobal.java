package com.ventas.key.mis.productos;

import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionDuplicado;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.exeption.MensajeError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDate;
import java.time.LocalDateTime;

@ControllerAdvice
@Slf4j
public class ExceptionGlobal{

    @ExceptionHandler(ExceptionDataNotFound.class)
    public ResponseEntity<MensajeError> dataNotFound(ExceptionDataNotFound exceptionDataNotFound) {
        log.info("Se capturo la exception dataNotFound ", exceptionDataNotFound);
        MensajeError mensajeError = MensajeError.builder().code(HttpStatus.BAD_REQUEST.value()).fecha(LocalDate.now().toString()).message(exceptionDataNotFound.getMessage()).build();
        return new ResponseEntity<>(mensajeError, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ExceptionErrorInesperado.class)
    public ResponseEntity<MensajeError> exceptionInesperado(ExceptionErrorInesperado exceptionInesperado) {
        log.info("Se capturo la exception exceptionInesperado ", exceptionInesperado);
        MensajeError mensajeError = MensajeError.builder().code(HttpStatus.BAD_REQUEST.value()).fecha(LocalDate.now().toString()).message(exceptionInesperado.getMessage()).build();
        return new ResponseEntity<>(mensajeError, HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(ExceptionDuplicado.class)
    public ResponseEntity<MensajeError> exceptionInesperado(ExceptionDuplicado exceptionDuplicado) {
        log.info("Se capturo la exception exceptionDuplicado ", exceptionDuplicado);
        MensajeError mensajeError = MensajeError.builder().code(HttpStatus.BAD_REQUEST.value()).fecha(LocalDateTime.now().toString()).message(exceptionDuplicado.getMessage()).build();
        return new ResponseEntity<>(mensajeError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MensajeError> exceptionInesperadoEx(Exception exceptionDuplicado) {
        log.info("Se capturo la exception exceptionDuplicado ", exceptionDuplicado);
        MensajeError mensajeError = MensajeError.builder().code(HttpStatus.BAD_REQUEST.value()).fecha(LocalDateTime.now().toString()).message(exceptionDuplicado.getMessage()).build();
        return new ResponseEntity<>(mensajeError, HttpStatus.BAD_REQUEST);
    }

}
