package com.ventas.key.hexagonal.grupopedido.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.GrupoNoEncontradoException;
import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.GrupoPedidoException;
import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.PedidosDeDistintoTipoException;
import com.ventas.key.hexagonal.grupopedido.dominio.modelo.AbonoAlGrupo;
import com.ventas.key.hexagonal.grupopedido.dominio.puerto.entrada.UnirPedidosCasoUso;
import com.ventas.key.hexagonal.grupopedido.infraestructura.dto.AbonoGrupoRequest;
import com.ventas.key.hexagonal.grupopedido.infraestructura.dto.AbonoGrupoResponse;
import com.ventas.key.hexagonal.grupopedido.infraestructura.dto.CambiarTitularRequest;
import com.ventas.key.hexagonal.grupopedido.infraestructura.dto.SeparacionResponse;
import com.ventas.key.hexagonal.grupopedido.infraestructura.dto.SepararRequest;
import com.ventas.key.hexagonal.grupopedido.infraestructura.dto.CobroContadoRequest;
import com.ventas.key.hexagonal.grupopedido.infraestructura.dto.CobroContadoResponse;
import com.ventas.key.hexagonal.grupopedido.infraestructura.dto.DeshacerGrupoRequest;
import com.ventas.key.hexagonal.grupopedido.infraestructura.dto.GrupoPedidosResponse;
import com.ventas.key.hexagonal.grupopedido.infraestructura.dto.TipoPorPedidoResponse;
import com.ventas.key.hexagonal.grupopedido.infraestructura.dto.UnirPedidosRequest;
import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Unir pedidos para cobrarlos y entregarlos juntos, y deshacerlo.
 *
 * <p>[Hexagonal: Driving Adapter] [Clean: Interface Adapter]
 *
 * <p>Permisos en {@code SecurityConfig}: abonar usa la accion {@code abonar} que ya existe en el
 * detalle del pedido; lo demas usa {@code unir-pedidos} ({@code migration_grupo_pedido.sql}).
 */
@RestController
@RequestMapping("/v1/grupos-pedido")
@Slf4j
@RequiredArgsConstructor
public class GrupoPedidoController {

    private final UnirPedidosCasoUso casoUso;
    private final CacheService cacheService;

    @PostMapping
    public ResponseEntity<Object> unir(@RequestBody UnirPedidosRequest request) {
        return ejecutar(true, () -> GrupoPedidosResponse.de(
                casoUso.unir(request.getPedidoIds(), request.getPedidoTitularId(), request.getNota(), usuarioActual())));
    }

    @GetMapping("/{grupoId}")
    public ResponseEntity<Object> consultar(@PathVariable Integer grupoId) {
        return ejecutar(false, () -> GrupoPedidosResponse.de(casoUso.consultar(grupoId)));
    }

    /** El grupo activo del pedido; 204 si no esta en ninguno. */
    @GetMapping("/por-pedido/{pedidoId}")
    public ResponseEntity<Object> porPedido(@PathVariable Integer pedidoId) {
        return casoUso.grupoActivoDe(pedidoId)
                .<ResponseEntity<Object>>map(g -> ResponseEntity.ok(new ResponseGeneric<>(GrupoPedidosResponse.de(g))))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{grupoId}/abonos")
    public ResponseEntity<Object> abonar(@PathVariable Integer grupoId, @RequestBody AbonoGrupoRequest request) {
        return ejecutar(true, () -> AbonoGrupoResponse.de(casoUso.abonar(grupoId,
                new AbonoAlGrupo(centavos(request.getMonto()), request.getMetodoPago(),
                        request.getMontoDado() == null ? null : centavos(request.getMontoDado()),
                        request.getNota()),
                usuarioActual())));
    }

    /** Confirma de una vez los pedidos de contado del grupo que falten (R11). */
    @PostMapping("/{grupoId}/cobrar-contado")
    public ResponseEntity<Object> cobrarDeContado(@PathVariable Integer grupoId,
                                                  @RequestBody CobroContadoRequest request) {
        return ejecutar(true, () -> CobroContadoResponse.de(
                casoUso.cobrarDeContado(grupoId, request.getPagosYMesesId(), usuarioActual())));
    }

    /** Separa uno o varios pedidos; en un grupo a credito, repartiendo lo abonado (R14-R15). */
    @PostMapping("/{grupoId}/separar")
    public ResponseEntity<Object> separar(@PathVariable Integer grupoId, @RequestBody SepararRequest request) {
        Map<Integer, Long> reparto = new LinkedHashMap<>();
        if (request.getReparto() != null) {
            for (SepararRequest.Parte parte : request.getReparto()) {
                if (parte.getPedidoId() != null && parte.getMonto() != null) {
                    reparto.put(parte.getPedidoId(), centavos(parte.getMonto()));
                }
            }
        }
        return ejecutar(true, () -> SeparacionResponse.de(casoUso.separar(grupoId, request.getPedidosQueSalen(),
                reparto, request.getNuevoTitularId(), request.getMotivo(), usuarioActual())));
    }

    /** Cambia quien paga y recoge (R16). */
    @PutMapping("/{grupoId}/titular")
    public ResponseEntity<Object> cambiarTitular(@PathVariable Integer grupoId, @RequestBody CambiarTitularRequest request) {
        return ejecutar(true, () -> GrupoPedidosResponse.de(
                casoUso.cambiarTitular(grupoId, request.getPedidoTitularId(), usuarioActual())));
    }

    @PostMapping("/{grupoId}/deshacer")
    public ResponseEntity<Object> deshacer(@PathVariable Integer grupoId,
                                           @RequestBody(required = false) DeshacerGrupoRequest request) {
        return ejecutar(true, () -> GrupoPedidosResponse.de(
                casoUso.deshacer(grupoId, request != null ? request.getMotivo() : null, usuarioActual())));
    }

    /** El unico lugar donde se mapean excepciones a status. */
    private ResponseEntity<Object> ejecutar(boolean escribe, Supplier<Object> operacion) {
        try {
            Object respuesta = operacion.get();
            if (escribe) {
                cacheService.evictAll();
            }
            return ResponseEntity.ok(new ResponseGeneric<>(respuesta));

        } catch (PedidosDeDistintoTipoException e) {
            // Trae el tipo de cada pedido para que la pantalla marque cuales hay que cambiar.
            List<TipoPorPedidoResponse> tipos = e.tipoPorPedido().entrySet().stream()
                    .map(x -> new TipoPorPedidoResponse(x.getKey(), x.getValue())).toList();
            ResponseGeneric<Object> error = new ResponseGeneric<>((Object) tipos);
            error.setCode(HttpStatus.BAD_REQUEST.value());
            error.setMensaje(e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (GrupoNoEncontradoException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (GrupoPedidoException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (RuntimeException e) {
            // registrarAbono avisa sus rechazos de negocio con RuntimeException simple
            // (p. ej. "excede el saldo"): se muestran tal cual para que el cajero sepa que paso.
            log.error("Error en grupo de pedidos", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la operacion: " + e.getMessage());
        }
    }

    private static ResponseEntity<Object> error(HttpStatus status, String mensaje) {
        ResponseGeneric<Object> error = new ResponseGeneric<>((Object) null);
        error.setCode(status.value());
        error.setMensaje(mensaje);
        return ResponseEntity.status(status).body(error);
    }

    private static long centavos(Double monto) {
        return monto == null ? 0 : Math.round(monto * 100);
    }

    private static Integer usuarioActual() {
        return AuthenticationUtils.currentUsuarioOpt().map(Usuario::getId).orElse(null);
    }
}
