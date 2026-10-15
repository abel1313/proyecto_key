package com.ventas.key.hexagonal.entrega.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.entrega.dominio.excepcion.EntregaNoPermitidaException;
import com.ventas.key.hexagonal.entrega.dominio.puerto.entrada.EntregarPedidoCasoUso;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 *
 * <p>📦 Entregar y "Regresar a Falta entregar". Protegidos por las acciones {@code entregar} y
 * {@code regresar-entrega} de Mis pedidos (SecurityConfig, migration_entrega_pedido.sql). Si el
 * pedido esta unido, actuan sobre todo el grupo.
 */
@RestController
@RequestMapping("/v1/pedidos")
@RequiredArgsConstructor
public class EntregaController {

    private final EntregarPedidoCasoUso casoUso;
    private final CacheService cacheService;

    @PostMapping("/{pedidoId}/entrega")
    public ResponseEntity<Object> entregar(@PathVariable Integer pedidoId) {
        return ejecutar(() -> casoUso.entregar(pedidoId));
    }

    @DeleteMapping("/{pedidoId}/entrega")
    public ResponseEntity<Object> regresar(@PathVariable Integer pedidoId) {
        return ejecutar(() -> casoUso.regresar(pedidoId));
    }

    private ResponseEntity<Object> ejecutar(Supplier<EntregarPedidoCasoUso.ResultadoEntrega> operacion) {
        try {
            EntregarPedidoCasoUso.ResultadoEntrega r = operacion.get();
            cacheService.evictAll();
            return ResponseEntity.ok(new ResponseGeneric<>(r));
        } catch (EntregaNoPermitidaException e) {
            ResponseGeneric<Object> error = new ResponseGeneric<>((Object) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
