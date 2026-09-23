package com.ventas.key.hexagonal.pedidoarticulo.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.CambioRompePromocionException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.excepcion.EdicionPedidoException;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.modelo.PedidoEditable;
import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.entrada.EditarArticulosCasoUso;
import com.ventas.key.hexagonal.pedidoarticulo.infraestructura.dto.AgregarArticuloRequest;
import com.ventas.key.hexagonal.pedidoarticulo.infraestructura.dto.CambiarArticuloRequest;
import com.ventas.key.hexagonal.pedidoarticulo.infraestructura.dto.OpcionesPromocionResponse;
import com.ventas.key.hexagonal.pedidoarticulo.infraestructura.dto.PedidoArticulosResponse;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Editar los articulos de un pedido ya creado.
 *
 * <p>[Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 *
 * <p>Solo traduce HTTP: valida nada de negocio, delega todo al caso de uso y mapea sus excepciones
 * a status. La unica decision propia es el <b>409</b> de {@link CambioRompePromocionException},
 * que no es un rechazo sino una pregunta al usuario (R4).
 *
 * <p>Los tres endpoints estan protegidos por acciones configurables, no por {@code hasRole}: ver
 * {@code migration_accion_pedido_articulos.sql} y los matchers en {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/v1/pedidos")
@Slf4j
@RequiredArgsConstructor
public class PedidoArticuloController {

    private final EditarArticulosCasoUso editarArticulos;
    private final CacheService cacheService;

    /** Agrega un articulo al pedido, a precio de catalogo. */
    @PostMapping("/{pedidoId}/articulos")
    public ResponseEntity<Object> agregar(@PathVariable Integer pedidoId,
                                          @RequestBody AgregarArticuloRequest request) {
        return ejecutar(() -> editarArticulos.agregar(pedidoId,
                new EditarArticulosCasoUso.AgregarArticulo(
                        request.getVarianteId(),
                        request.getCantidad() != null ? request.getCantidad() : 1,
                        request.getPrecioUnitario())));
    }

    /**
     * Cambia una linea por otro articulo.
     *
     * <p>Responde <b>409</b> con las dos salidas cuando el articulo nuevo rompe una promocion y el
     * request no dijo que hacer.
     */
    @PutMapping("/{pedidoId}/articulos/{detalleId}")
    public ResponseEntity<Object> cambiar(@PathVariable Integer pedidoId,
                                          @PathVariable Integer detalleId,
                                          @RequestBody CambiarArticuloRequest request) {
        return ejecutar(() -> editarArticulos.cambiar(pedidoId, detalleId,
                new EditarArticulosCasoUso.CambiarArticulo(
                        request.getVarianteId(),
                        request.getCantidad(),
                        request.getPrecioUnitario(),
                        modoDe(request.getModo()))));
    }

    /** Saca del pedido todas las lineas de una promocion y devuelve su stock. */
    @DeleteMapping("/{pedidoId}/promociones/{promocionId}")
    public ResponseEntity<Object> quitarPromocion(@PathVariable Integer pedidoId,
                                                  @PathVariable Integer promocionId) {
        return ejecutar(() -> editarArticulos.quitarPromocion(pedidoId, promocionId));
    }

    /**
     * El unico lugar donde se mapean excepciones a status, para los tres endpoints.
     *
     * <p>Sin esto cada metodo repetiria el mismo try/catch de tres ramas y bastaria olvidar una
     * para que el 409 de la promocion saliera como un 400 cualquiera y el modal nunca apareciera.
     */
    private ResponseEntity<Object> ejecutar(Operacion operacion) {
        try {
            PedidoArticulosResponse respuesta = PedidoArticulosResponse.de(operacion.correr());
            cacheService.evictAll();
            return ResponseEntity.ok(new ResponseGeneric<>(respuesta));

        } catch (CambioRompePromocionException e) {
            // 409 Conflict: el pedido esta intacto y hay que elegir como seguir. No es un error
            // del request -- mandarlo como 400 haria que el front lo trate como "corregi y
            // reintenta", que es justo lo contrario de lo que toca hacer.
            log.info("Pedido {}: el cambio rompe la promocion '{}', se devuelven las opciones",
                    e.promocion().promocionId(), e.promocion().descripcion());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(OpcionesPromocionResponse.de(e));

        } catch (EdicionPedidoException e) {
            ResponseGeneric<Object> error = new ResponseGeneric<>((Object) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (RuntimeException e) {
            log.error("Error inesperado editando los articulos de un pedido", e);
            ResponseGeneric<Object> error = new ResponseGeneric<>((Object) null);
            error.setMensaje("No se pudo editar el pedido: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private static EditarArticulosCasoUso.ModoCambio modoDe(String modo) {
        if (modo == null || modo.isBlank()) {
            return EditarArticulosCasoUso.ModoCambio.VALIDAR;
        }
        try {
            return EditarArticulosCasoUso.ModoCambio.valueOf(modo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new EdicionPedidoException("Modo '" + modo + "' no valido. Los validos son "
                    + "VALIDAR, QUITAR_PROMOCION y CONSERVAR_PROMOCION");
        }
    }

    @FunctionalInterface
    private interface Operacion {
        PedidoEditable correr();
    }
}
