package com.ventas.key.hexagonal.precio.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.precio.dominio.puerto.entrada.CambiarPrecioArticuloCasoUso;
import com.ventas.key.hexagonal.precio.dominio.puerto.entrada.CambiarPrecioCasoUso;
import com.ventas.key.hexagonal.precio.infraestructura.dto.CambiarPrecioRequest;
import com.ventas.key.hexagonal.precio.infraestructura.dto.PreciosArticuloResponse;
import com.ventas.key.hexagonal.precio.infraestructura.dto.PreciosProductoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final CambiarPrecioArticuloCasoUso articulo;

    @PutMapping("/producto/{productoId}")
    public ResponseEntity<PreciosProductoResponse> cambiar(@PathVariable Integer productoId,
                                                           @RequestBody CambiarPrecioRequest request) {
        return ResponseEntity.ok(PreciosProductoResponse.de(
                casoUso.cambiar(productoId, request.precioVenta(), request.precioRebaja())));
    }

    /** Precio propio para un solo articulo (boton 💲 de la tarjeta, desde 2026-09-29). */
    @PutMapping("/articulo/{varianteId}")
    public ResponseEntity<PreciosArticuloResponse> cambiarArticulo(@PathVariable Integer varianteId,
                                                                   @RequestBody CambiarPrecioRequest request) {
        return ResponseEntity.ok(PreciosArticuloResponse.de(
                articulo.cambiar(varianteId, request.precioVenta(), request.precioRebaja())));
    }

    /** Le quita el precio propio al articulo: vuelve a cobrar el de su producto. */
    @DeleteMapping("/articulo/{varianteId}")
    public ResponseEntity<PreciosArticuloResponse> usarElDelProducto(@PathVariable Integer varianteId) {
        return ResponseEntity.ok(PreciosArticuloResponse.de(articulo.usarElDelProducto(varianteId)));
    }
}
