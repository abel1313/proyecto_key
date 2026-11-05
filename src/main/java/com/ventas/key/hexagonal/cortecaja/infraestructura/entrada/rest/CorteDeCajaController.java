package com.ventas.key.hexagonal.cortecaja.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.cortecaja.dominio.puerto.entrada.ArmarCorteDeCajaCasoUso;
import com.ventas.key.hexagonal.cortecaja.infraestructura.entrada.rest.dto.CorteDeCajaResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 *
 * <p>{@code GET /v1/reportes/corte-caja}: solo administrador (regla propia en {@code SecurityConfig}) y solo lectura. Sin lógica: traduce
 * HTTP al caso de uso y el resultado al JSON del front.
 */
@RestController
@RequestMapping("/v1/reportes/corte-caja")
@RequiredArgsConstructor
@Slf4j
public class CorteDeCajaController {

    private final ArmarCorteDeCajaCasoUso armar;

    @GetMapping
    public ResponseEntity<ResponseGeneric<CorteDeCajaResponse>> corte(@RequestParam(required = false) Integer zonaId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(CorteDeCajaResponse.de(armar.armar(zonaId))));
        } catch (RuntimeException e) {
            log.error("Error al armar el corte de caja", e);
            ResponseGeneric<CorteDeCajaResponse> error = new ResponseGeneric<>((CorteDeCajaResponse) null);
            error.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            error.setMensaje("No se pudo armar el corte de caja.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
