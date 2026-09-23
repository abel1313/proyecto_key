package com.ventas.key.hexagonal.precio.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.precio.dominio.puerto.entrada.CambiarPrecioCasoUso;
import com.ventas.key.hexagonal.precio.infraestructura.dto.CambiarPrecioRequest;
import com.ventas.key.hexagonal.precio.infraestructura.dto.PreciosProductoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapters — Controllers]
 *
 * <p>Permiso: accion "cambiar-precio" de "tienda/buscar" (SecurityConfig +
 * migration_accion_tienda_cambiar_precio.sql).
 */
@RestController
@RequestMapping("/v1/precios")
@RequiredArgsConstructor
public class PrecioController {

    private final CambiarPrecioCasoUso casoUso;

    @PutMapping("/producto/{productoId}")
    public ResponseEntity<PreciosProductoResponse> cambiar(@PathVariable Integer productoId,
                                                           @RequestBody CambiarPrecioRequest request) {
        return ResponseEntity.ok(PreciosProductoResponse.de(
                casoUso.cambiar(productoId, request.precioVenta(), request.precioRebaja())));
    }
}
