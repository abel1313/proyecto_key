package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.pedidos.DatosClientePedidoRequest;
import com.ventas.key.mis.productos.service.CacheService;
import com.ventas.key.mis.productos.service.PedidoDatosClienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Agregar los datos del cliente a un pedido que no los tiene. Solo administrador (regla en SecurityConfig). Sin logica: delega en el servicio. */
@RestController
@RequestMapping("/v1/pedidos")
@RequiredArgsConstructor
@Slf4j
public class PedidoDatosClienteController {

    private final PedidoDatosClienteService datosCliente;
    private final CacheService cacheService;

    @PostMapping("/{pedidoId}/datos-cliente")
    public ResponseEntity<ResponseGeneric<Object>> agregar(@PathVariable int pedidoId, @RequestBody DatosClientePedidoRequest request) {
        try {
            datosCliente.agregar(pedidoId, request);
            cacheService.evictAll();
            ResponseGeneric<Object> ok = new ResponseGeneric<>((Object) null);
            ok.setCode(200);
            ok.setMensaje("Datos del cliente guardados");
            return ResponseEntity.ok(ok);
        } catch (IllegalArgumentException e) {
            ResponseGeneric<Object> error = new ResponseGeneric<>((Object) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException e) {
            log.error("Error agregando los datos del cliente al pedido {}", pedidoId, e);
            ResponseGeneric<Object> error = new ResponseGeneric<>((Object) null);
            error.setMensaje("No se pudieron guardar los datos del cliente.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
