package com.ventas.key.mis.productos;

import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionDuplicado;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.exeption.ExceptionStockInsuficiente;
import com.ventas.key.mis.productos.exeption.MensajeError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class ExceptionGlobal {

    @ExceptionHandler(ExceptionDataNotFound.class)
    public ResponseEntity<MensajeError> dataNotFound(ExceptionDataNotFound ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        MensajeError mensajeError = MensajeError.builder()
                .code(HttpStatus.NOT_FOUND.value())
                .fecha(LocalDate.now().toString())
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(mensajeError, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ExceptionErrorInesperado.class)
    public ResponseEntity<MensajeError> exceptionErrorInesperado(ExceptionErrorInesperado ex) {
        log.warn("Error de negocio: {}", ex.getMessage());
        MensajeError mensajeError = MensajeError.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .fecha(LocalDate.now().toString())
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(mensajeError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ExceptionDuplicado.class)
    public ResponseEntity<MensajeError> exceptionDuplicado(ExceptionDuplicado ex) {
        log.warn("Recurso duplicado: {}", ex.getMessage());
        MensajeError mensajeError = MensajeError.builder()
                .code(HttpStatus.CONFLICT.value())
                .fecha(LocalDateTime.now().toString())
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(mensajeError, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ExceptionStockInsuficiente.class)
    public ResponseEntity<MensajeError> stockInsuficiente(ExceptionStockInsuficiente ex) {
        log.warn("Stock insuficiente: {}", ex.getMessage());
        MensajeError mensajeError = MensajeError.builder()
                .code(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .fecha(LocalDateTime.now().toString())
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(mensajeError, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MensajeError> validacionFallida(MethodArgumentNotValidException ex) {
        String errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validacion fallida: {}", errores);
        MensajeError mensajeError = MensajeError.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .fecha(LocalDateTime.now().toString())
                .message("Datos invalidos: " + errores)
                .build();
        return new ResponseEntity<>(mensajeError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<MensajeError> archivoMuyGrande(MaxUploadSizeExceededException ex) {
        log.warn("Archivo demasiado grande: {}", ex.getMessage());
        MensajeError mensajeError = MensajeError.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .fecha(LocalDateTime.now().toString())
                .message("El archivo excede el tamaño maximo permitido")
                .build();
        return new ResponseEntity<>(mensajeError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MensajeError> exceptionNoControlada(Exception ex) {
        log.error("Error no controlado en el servidor", ex);
        MensajeError mensajeError = MensajeError.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .fecha(LocalDateTime.now().toString())
                .message("Error interno del servidor")
                .build();
        return new ResponseEntity<>(mensajeError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
