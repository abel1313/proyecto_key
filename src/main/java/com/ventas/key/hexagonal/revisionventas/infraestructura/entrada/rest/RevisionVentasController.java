package com.ventas.key.hexagonal.revisionventas.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.revisionventas.dominio.excepcion.RevisionVentasException;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.ResultadoRevision;
import com.ventas.key.hexagonal.revisionventas.dominio.puerto.entrada.RevisarVentasCasoUso;
import com.ventas.key.hexagonal.revisionventas.infraestructura.entrada.rest.dto.RevisionResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 *
 * <p>{@code GET /v1/reportes/revision-ventas}: solo administrador (regla propia en {@code SecurityConfig}) y solo lectura.
 * Sin lógica: traduce HTTP al caso de uso y el resultado al JSON del front.
 */
@RestController
@RequestMapping("/v1/reportes/revision-ventas")
@RequiredArgsConstructor
@Slf4j
public class RevisionVentasController {

    private final RevisarVentasCasoUso revisar;

    @GetMapping
    public ResponseEntity<ResponseGeneric<RevisionResponse>> revisar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            ResultadoRevision resultado = revisar.revisar(desde, hasta);
            return ResponseEntity.ok(new ResponseGeneric<>(RevisionResponse.de(resultado)));
        } catch (RevisionVentasException.FechasInvalidas e) {
            ResponseGeneric<RevisionResponse> error = new ResponseGeneric<>((RevisionResponse) null);
            error.setCode(HttpStatus.BAD_REQUEST.value());
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException e) {
            log.error("Error en la revisión de ventas", e);
            ResponseGeneric<RevisionResponse> error = new ResponseGeneric<>((RevisionResponse) null);
            error.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            error.setMensaje("No se pudo hacer la revisión de ventas.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
