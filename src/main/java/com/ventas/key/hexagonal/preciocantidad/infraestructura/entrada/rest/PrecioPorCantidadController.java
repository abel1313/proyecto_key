package com.ventas.key.hexagonal.preciocantidad.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.preciocantidad.dominio.puerto.entrada.CalcularPreciosPorCantidadCasoUso;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.entrada.rest.dto.PrecioPorCantidadRequest;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.entrada.rest.dto.PrecioPorCantidadResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [Hexagonal: Driving Adapter] {@code POST /v1/precio-cantidad/calcular}: con lo que lleva la venta o el carrito, dice a qué productos les
 * toca su precio de grupo. Solo calcula y NO guarda: el precio que se cobra lo vuelve a decidir el back al guardar la venta. Requiere sesión.
 */
@RestController
@RequestMapping("/v1/precio-cantidad")
@RequiredArgsConstructor
@Slf4j
public class PrecioPorCantidadController {

    private final CalcularPreciosPorCantidadCasoUso calculo;

    @PostMapping("/calcular")
    public ResponseEntity<ResponseGeneric<PrecioPorCantidadResponse>> calcular(@RequestBody PrecioPorCantidadRequest request) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(PrecioPorCantidadResponse.de(calculo.calcular(request.aDominio()).values())));
        } catch (RuntimeException e) {
            log.error("No se pudo calcular el precio por cantidad", e);
            ResponseGeneric<PrecioPorCantidadResponse> r = new ResponseGeneric<>((PrecioPorCantidadResponse) null);
            r.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            r.setMensaje("No se pudo calcular el precio por cantidad.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(r);
        }
    }
}
