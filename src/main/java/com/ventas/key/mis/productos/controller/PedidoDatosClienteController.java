package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.pedidos.DatosClientePedidoRequest;
import com.ventas.key.mis.productos.service.CacheService;
import com.ventas.key.mis.productos.service.PedidoDatosClienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Datos del cliente del pedido {} rechazados por la base: {}", pedidoId, e.getMostSpecificCause().getMessage());
            ResponseGeneric<Object> error = new ResponseGeneric<>((Object) null);
            error.setMensaje(com.ventas.key.mis.productos.handleExeption.MensajesParaUsuario.traducir(e.getMostSpecificCause().getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException e) {
            log.error("Error agregando los datos del cliente al pedido {}", pedidoId, e);
            ResponseGeneric<Object> error = new ResponseGeneric<>((Object) null);
            error.setMensaje("No se pudieron guardar los datos del cliente.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /** Corrige los datos del cliente (sin registro) de un pedido. */
    @PutMapping("/{pedidoId}/datos-cliente")
    public ResponseEntity<ResponseGeneric<Object>> editar(@PathVariable int pedidoId, @RequestBody DatosClientePedidoRequest request) {
        return responder(pedidoId, "Datos del cliente actualizados", () -> datosCliente.editar(pedidoId, request));
    }

    /** Elige cual dato se muestra del pedido: {@code {"mostrar": "CLIENTE"}} o {@code {"mostrar": "RECEPTOR"}}. */
    @PutMapping("/{pedidoId}/datos-visibles")
    public ResponseEntity<ResponseGeneric<Object>> datosVisibles(@PathVariable int pedidoId, @RequestBody Map<String, String> cuerpo) {
        return responder(pedidoId, "Listo, se mostrarán esos datos", () -> datosCliente.cambiarDatosVisibles(pedidoId, cuerpo == null ? null : cuerpo.get("mostrar")));
    }

    private ResponseEntity<ResponseGeneric<Object>> responder(int pedidoId, String mensajeOk, Runnable accion) {
        try {
            accion.run();
            cacheService.evictAll();
            ResponseGeneric<Object> ok = new ResponseGeneric<>((Object) null);
            ok.setCode(200);
            ok.setMensaje(mensajeOk);
            return ResponseEntity.ok(ok);
        } catch (IllegalArgumentException e) {
            ResponseGeneric<Object> error = new ResponseGeneric<>((Object) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Datos del cliente del pedido {} rechazados por la base: {}", pedidoId, e.getMostSpecificCause().getMessage());
            ResponseGeneric<Object> error = new ResponseGeneric<>((Object) null);
            error.setMensaje(com.ventas.key.mis.productos.handleExeption.MensajesParaUsuario.traducir(e.getMostSpecificCause().getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException e) {
            log.error("Error cambiando los datos del cliente del pedido {}", pedidoId, e);
            ResponseGeneric<Object> error = new ResponseGeneric<>((Object) null);
            error.setMensaje("No se pudieron guardar los datos del pedido.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
