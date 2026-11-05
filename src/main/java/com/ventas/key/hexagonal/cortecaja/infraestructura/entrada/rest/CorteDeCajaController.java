package com.ventas.key.hexagonal.cortecaja.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.cortecaja.dominio.excepcion.CorteDeCajaException;
import com.ventas.key.hexagonal.cortecaja.dominio.modelo.FiltroDeCorte;
import com.ventas.key.hexagonal.cortecaja.dominio.puerto.entrada.ArmarCorteDeCajaCasoUso;
import com.ventas.key.hexagonal.cortecaja.infraestructura.entrada.rest.dto.CorteDeCajaResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
 * <p>{@code GET /v1/reportes/corte-caja} (varias zonas: {@code ?zonaId=1&zonaId=3}; {@code zonaId=0} = «Sin zona»): solo administrador (regla propia en {@code SecurityConfig}) y solo lectura. Sin lógica: traduce
 * HTTP al caso de uso y el resultado al JSON del front.
 */
@RestController
@RequestMapping("/v1/reportes/corte-caja")
@RequiredArgsConstructor
@Slf4j
public class CorteDeCajaController {

    private final ArmarCorteDeCajaCasoUso armar;

    @GetMapping
    public ResponseEntity<ResponseGeneric<CorteDeCajaResponse>> corte(
            @RequestParam(required = false) java.util.List<Integer> zonaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false, defaultValue = "POR_COBRAR") FiltroDeCorte.Pago pago,
            @RequestParam(required = false, defaultValue = "SIN_ENTREGAR") FiltroDeCorte.Entrega entrega) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(
                    CorteDeCajaResponse.de(armar.armar(new FiltroDeCorte(zonaId == null ? null : new java.util.HashSet<>(zonaId), desde, hasta, pago, entrega)))));
        } catch (CorteDeCajaException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al armar el corte de caja", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo armar el corte de caja.");
        }
    }

    private static ResponseEntity<ResponseGeneric<CorteDeCajaResponse>> error(HttpStatus estado, String mensaje) {
        ResponseGeneric<CorteDeCajaResponse> error = new ResponseGeneric<>((CorteDeCajaResponse) null);
        error.setCode(estado.value());
        error.setMensaje(mensaje);
        return ResponseEntity.status(estado).body(error);
    }
}
