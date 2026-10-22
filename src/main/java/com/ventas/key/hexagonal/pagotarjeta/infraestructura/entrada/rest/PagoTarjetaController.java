package com.ventas.key.hexagonal.pagotarjeta.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CanalDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.CancelarCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.ConfirmarCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.DevolverCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.IniciarCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.OfrecerPlanesCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.IntentosDePagoPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PedidoACobrarPort;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.entrada.rest.dto.CobroResponse;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.entrada.rest.dto.DevolverCobroRequest;
import com.ventas.key.hexagonal.pagotarjeta.infraestructura.entrada.rest.dto.IniciarCobroRequest;
import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Cobrar un pedido con tarjeta: en la terminal del local (solo administrador) o desde la tienda en linea (el dueño).
 *
 * <p>[Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 *
 * <p><b>Autorizacion (aqui, no en el dominio):</b> el administrador puede todo; un cliente solo toca los cobros y
 * pedidos que son suyos y solo en linea. Cuando el pedido o cobro no es suyo se responde 404 (no 403): los ids
 * son consecutivos y un 403 confirmaria que existen. {@code iniciadoPor} sale del token, nunca del body, y el
 * monto y el tope de cuotas los calcula el back (T2, M6). Devolver es solo administrador (tambien en SecurityConfig).
 *
 * <p>Existe solo con {@code pagos.tarjeta.habilitado=true} (ver {@code PagoTarjetaBeans}).
 */
@RestController
@RequestMapping("/v1/pagos-tarjeta")
@ConditionalOnProperty(name = "pagos.tarjeta.habilitado", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class PagoTarjetaController {

    private final IniciarCobroCasoUso iniciar;
    private final ConfirmarCobroCasoUso confirmar;
    private final CancelarCobroCasoUso cancelar;
    private final DevolverCobroCasoUso devolver;
    private final OfrecerPlanesCasoUso ofrecerPlanes;
    private final IntentosDePagoPort intentos;
    private final PedidoACobrarPort pedidos;
    private final CacheService cacheService;

    /** Meses sin intereses que se pueden ofrecer para ese pedido (vacio = solo de contado). */
    @GetMapping("/pedido/{pedidoId}/meses")
    public ResponseEntity<Object> meses(@PathVariable Integer pedidoId) {
        return ejecutar(false, () -> {
            exigirPedidoPropio(pedidoId);
            return ofrecerPlanes.mesesPara(pedidoId);
        });
    }

    @PostMapping
    public ResponseEntity<Object> iniciar(@RequestBody IniciarCobroRequest request) {
        return ejecutar(true, () -> {
            if (request == null || request.pedidoId() == null || request.canal() == null) {
                throw PagoConTarjetaException.datosInvalidos("faltan el pedido o el canal de pago");
            }
            CanalDePago canal = canal(request.canal());
            if (canal == CanalDePago.TERMINAL && !AuthenticationUtils.isAdminContext()) {
                throw new NoEncontrado();                          // la terminal es del local, no del cliente
            }
            exigirPedidoPropio(request.pedidoId());
            Usuario usuario = AuthenticationUtils.currentUsuario();
            int cuotas = request.cuotas() == null ? 1 : request.cuotas();
            return CobroResponse.de(iniciar.iniciar(request.pedidoId(), canal, cuotas, usuario.getUsername()));
        });
    }

    /** Estado del cobro. Le pregunta a la pasarela y aplica el resultado: la pantalla lo llama mientras {@code enProceso}. */
    @GetMapping("/{intentoId}")
    public ResponseEntity<Object> estado(@PathVariable Long intentoId) {
        return ejecutar(true, () -> {
            exigirCobroPropio(intentoId);
            return CobroResponse.de(confirmar.confirmar(intentoId));
        });
    }

    @PostMapping("/{intentoId}/cancelar")
    public ResponseEntity<Object> cancelar(@PathVariable Long intentoId) {
        return ejecutar(true, () -> {
            exigirCobroPropio(intentoId);
            return CobroResponse.de(cancelar.cancelar(intentoId));
        });
    }

    @PostMapping("/{intentoId}/devolver")
    public ResponseEntity<Object> devolver(@PathVariable Long intentoId,
                                           @RequestBody(required = false) DevolverCobroRequest request) {
        return ejecutar(true, () -> {
            if (!AuthenticationUtils.isAdminContext()) {
                throw new NoEncontrado();
            }
            Usuario usuario = AuthenticationUtils.currentUsuario();
            return CobroResponse.de(devolver.devolver(intentoId, request != null ? request.monto() : null,
                    usuario.getUsername()));
        });
    }

    // ── Autorizacion ──────────────────────────────────────────────────────

    private void exigirPedidoPropio(Integer pedidoId) {
        var pedido = pedidos.buscar(pedidoId).orElseThrow(NoEncontrado::new);
        exigirDueño(pedido.clienteId());
    }

    private void exigirCobroPropio(Long intentoId) {
        IntentoDePago intento = intentos.buscar(intentoId).orElseThrow(NoEncontrado::new);
        exigirDueño(intento.clienteId());
    }

    /** Admin siempre; un cliente solo si el dueño es el. Un pedido sin dueño registrado (null) es solo del admin. */
    private static void exigirDueño(Integer clienteIdDelPedido) {
        if (AuthenticationUtils.isAdminContext()) {
            return;
        }
        Integer propio = AuthenticationUtils.currentUsuarioOpt()
                .map(Usuario::getCliente).map(c -> c.getId()).orElse(null);
        if (propio == null || clienteIdDelPedido == null || !Objects.equals(propio, clienteIdDelPedido)) {
            throw new NoEncontrado();
        }
    }

    private static CanalDePago canal(String texto) {
        try {
            return CanalDePago.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw PagoConTarjetaException.datosInvalidos("el canal debe ser TERMINAL o EN_LINEA");
        }
    }

    // ── Errores: el unico lugar donde se mapean excepciones a status ──────

    private static final class NoEncontrado extends RuntimeException {
        NoEncontrado() {
            super("No se encontró");
        }
    }

    private ResponseEntity<Object> ejecutar(boolean escribe, Supplier<Object> operacion) {
        try {
            Object respuesta = operacion.get();
            if (escribe) {
                cacheService.evictAll();
            }
            return ResponseEntity.ok(new ResponseGeneric<>(respuesta));
        } catch (NoEncontrado e) {
            return error(HttpStatus.NOT_FOUND, "No se encontró el pedido o el cobro");
        } catch (PagoConTarjetaException.ConflictoDeConcurrencia e) {
            return error(HttpStatus.CONFLICT, e.getMessage());
        } catch (PagoConTarjetaException.CobroNoCreado e) {
            return error(HttpStatus.BAD_GATEWAY, e.getMessage());
        } catch (PagoConTarjetaException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error en pagos con tarjeta", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la operación. Revisa el estado del cobro antes de volver a intentarlo");
        }
    }

    private static ResponseEntity<Object> error(HttpStatus status, String mensaje) {
        ResponseGeneric<Object> error = new ResponseGeneric<>((Object) null);
        error.setCode(status.value());
        error.setMensaje(mensaje);
        return ResponseEntity.status(status).body(error);
    }
}
