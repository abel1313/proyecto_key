package com.ventas.key.mis.productos;

import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionDuplicado;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.exeption.ExceptionStockInsuficiente;
import com.ventas.key.mis.productos.handleExeption.GenericException;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class ExceptionGlobal {

    @ExceptionHandler(ExceptionDataNotFound.class)
    public ResponseEntity<ResponseGeneric<Void>> dataNotFound(ExceptionDataNotFound ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ExceptionErrorInesperado.class)
    public ResponseEntity<ResponseGeneric<Void>> errorNegocio(ExceptionErrorInesperado ex) {
        log.warn("Error de negocio: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ExceptionDuplicado.class)
    public ResponseEntity<ResponseGeneric<Void>> duplicado(ExceptionDuplicado ex) {
        log.warn("Recurso duplicado: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ExceptionStockInsuficiente.class)
    public ResponseEntity<ResponseGeneric<Void>> stockInsuficiente(ExceptionStockInsuficiente ex) {
        log.warn("Stock insuficiente: {}", ex.getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(GenericException.class)
    public ResponseEntity<ResponseGeneric<Void>> genericException(GenericException ex) {
        HttpStatus status = switch (ex.getCodigo()) {
            case 1062 -> HttpStatus.CONFLICT;
            case 500  -> HttpStatus.INTERNAL_SERVER_ERROR;
            default   -> HttpStatus.BAD_REQUEST;
        };
        log.warn("Error generico {}: {}", ex.getCodigo(), ex.getMessage());
        return build(status, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseGeneric<Void>> validacionFallida(MethodArgumentNotValidException ex) {
        String errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validacion fallida: {}", errores);
        return build(HttpStatus.BAD_REQUEST, "Datos invalidos: " + errores);
    }

    // Sin este handler, un @RequestParam obligatorio que no llega (ej. varianteId en los
    // endpoints de redes sociales) caia en el catch-all de Exception.class de abajo -- salia
    // como 500 "Error interno del servidor" en vez de decir claramente que parametro faltaba.
    // MissingServletRequestParameterException extiende ServletException, no RuntimeException,
    // por eso no lo agarraba el handler de RuntimeException de arriba.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ResponseGeneric<Void>> parametroFaltante(MissingServletRequestParameterException ex) {
        log.warn("Parametro requerido faltante: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Falta el parámetro requerido: " + ex.getParameterName());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResponseGeneric<Void>> archivoMuyGrande(MaxUploadSizeExceededException ex) {
        log.warn("Archivo demasiado grande: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "El archivo excede el tamaño maximo permitido");
    }

    // Fallo de infraestructura al hacer commit de la transaccion (ej. violacion de unique
    // constraint que no se valido antes de guardar, timeout de conexion, etc.) -- Spring lo
    // envuelve en TransactionSystemException, que ES una RuntimeException, asi que sin este
    // handler especifico caia en el de abajo y el mensaje crudo de JPA/Hibernate (tipo "could
    // not commit JPA transaction; nested exception is...") se le mostraba tal cual al usuario
    // en vez de quedarse en el log (encontrado 2026-09-04, hotfix urgente en prod).
    // DataAccessException cubre el resto de fallos de Spring Data/JPA que no pasan por commit
    // (ej. una query que falla directo).
    @ExceptionHandler({TransactionSystemException.class, DataAccessException.class})
    public ResponseEntity<ResponseGeneric<Void>> errorBaseDatos(Exception ex) {
        log.error("Error de base de datos / transaccion no controlado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el cambio. Intenta de nuevo en unos minutos.");
    }

    // Gran parte de las validaciones de negocio (stock insuficiente, precio invalido, promocion
    // vencida, etc.) se lanzan como RuntimeException simple en vez de un tipo propio. Sin este
    // handler caian en el catch-all de Exception.class de abajo y el mensaje real se perdia,
    // mostrando siempre "Error interno del servidor" aunque fuera un error de validacion normal.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResponseGeneric<Void>> runtimeException(RuntimeException ex) {
        log.warn("Error de negocio: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseGeneric<Void>> noControlada(Exception ex) {
        log.error("Error no controlado en el servidor", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    private ResponseEntity<ResponseGeneric<Void>> build(HttpStatus status, String mensaje) {
        ResponseGeneric<Void> body = new ResponseGeneric<>(mensaje, status.value(), null, null);
        return ResponseEntity.status(status).body(body);
    }
}
