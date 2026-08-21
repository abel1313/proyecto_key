package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.floreseternas.EditarRamoRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.EditarRamoResponseDto;
import com.ventas.key.mis.productos.models.floreseternas.FrasePendienteDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleResponseDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoPedidoDetalleValidarFraseRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.RevalidarPagoResponseDto;
import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.service.RamoPedidoDetalleServiceImpl;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Adjunta el "ticket de produccion" de un ramo a un Pedido ya creado por el flujo normal
// (POST /v1/pedidos/savePedido). Ver RamoPedidoDetalleServiceImpl para el porque.
@Slf4j
@RestController
@RequestMapping("v1/flores/pedidos")
public class RamoPedidoDetalleController {

    private final RamoPedidoDetalleServiceImpl ramoPedidoDetalleService;

    public RamoPedidoDetalleController(RamoPedidoDetalleServiceImpl ramoPedidoDetalleService) {
        this.ramoPedidoDetalleService = ramoPedidoDetalleService;
    }

    @PostMapping("/{pedidoId}/detalle")
    public ResponseEntity<ResponseGeneric<RamoPedidoDetalleResponseDto>> adjuntar(
            @PathVariable Integer pedidoId, @RequestBody RamoPedidoDetalleRequestDto request) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ramoPedidoDetalleService.adjuntar(pedidoId, request)));
        } catch (Exception e) {
            log.error("Error al adjuntar detalle de ramo al pedido {}: {}", pedidoId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/{pedidoId}/detalle")
    public ResponseEntity<ResponseGeneric<RamoPedidoDetalleResponseDto>> listarPorPedido(
            @PathVariable Integer pedidoId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ramoPedidoDetalleService.listarPorPedido(pedidoId)));
        } catch (Exception e) {
            log.error("Error al listar detalle de ramo del pedido {}: {}", pedidoId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseGeneric<>((List<RamoPedidoDetalleResponseDto>) null, "Error al listar"));
        }
    }

    @GetMapping("/frases-pendientes")
    public ResponseEntity<ResponseGeneric<PginaDto<List<FrasePendienteDto>>>> listarFrasesPendientes(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ramoPedidoDetalleService.listarFrasesPendientes(pagina, size)));
        } catch (Exception e) {
            log.error("Error al listar frases pendientes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseGeneric<>(null, "Error al listar"));
        }
    }

    // El front llama esto justo antes de POST /v1/abonos/{pedidoId} para saber si el plazo
    // urgente ya vencio -- si vencio, aqui mismo se agrega el cargo urgente al pedido
    // (recotizacion) y el front debe usar totalActual para el monto del abono, no uno calculado
    // antes. No toca el endpoint generico de abonos, ver RamoPedidoDetalleServiceImpl.
    @PostMapping("/{pedidoId}/revalidar-antes-de-pagar")
    public ResponseEntity<ResponseGeneric<RevalidarPagoResponseDto>> revalidarAntesDePagar(
            @PathVariable Integer pedidoId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ramoPedidoDetalleService.revalidarAntesDePagar(pedidoId)));
        } catch (Exception e) {
            log.error("Error al revalidar pago del pedido {}: {}", pedidoId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    // Solo ADMIN: reemplaza colores/accesorios de un ramo ya guardado y recotiza esa parte;
    // opcionalmente tambien fechaHoraEntrega/urgente (envio/liston siguen sin tocarse, ver
    // EditarRamoRequestDto). El front debe mostrar response.diferencia al admin -- si es
    // positiva, falta registrar esa diferencia como abono (POST /v1/abonos/{pedidoId}, ya
    // existente) para que el pedido quede cuadrado.
    @PutMapping("/{pedidoId}/editar-ramo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseGeneric<EditarRamoResponseDto>> editarRamo(
            @PathVariable Integer pedidoId, @RequestBody EditarRamoRequestDto request) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ramoPedidoDetalleService.editarRamo(pedidoId, request)));
        } catch (Exception e) {
            log.warn("Error al editar ramo del pedido {}: {}", pedidoId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    // El cliente cancela su propio pedido de flores, solo antes de pagar el anticipo -- pedido
    // explicito del dueno (2026-08-17). El chequeo de dueno va aqui (mismo patron que
    // ClienteControllerImpl): el ADMIN siempre puede (para eso ya tiene el endpoint generico,
    // pero no tiene sentido bloquearlo aqui tambien), cualquier otro solo si es su propio pedido.
    @DeleteMapping("/{pedidoId}/cancelar")
    public ResponseEntity<ResponseGeneric<String>> cancelarPropio(@PathVariable Integer pedidoId) {
        try {
            if (!AuthenticationUtils.isAdminContext()) {
                Usuario actual = AuthenticationUtils.currentUsuario();
                Integer clienteIdPropietario = ramoPedidoDetalleService.obtenerClienteIdPropietario(pedidoId);
                boolean esDueno = actual.getCliente() != null && clienteIdPropietario != null
                        && actual.getCliente().getId().equals(clienteIdPropietario);
                if (!esDueno) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new ResponseGeneric<>((String) null, "No autorizado -- este pedido no es tuyo"));
                }
            }
            ramoPedidoDetalleService.cancelarPropio(pedidoId);
            return ResponseEntity.ok(new ResponseGeneric<>("Pedido cancelado correctamente"));
        } catch (Exception e) {
            log.warn("Error al cancelar pedido {} por el cliente: {}", pedidoId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>((String) null, e.getMessage()));
        }
    }

    @PutMapping("/detalle/{id}/validar-frase")
    public ResponseEntity<ResponseGeneric<RamoPedidoDetalleResponseDto>> validarFrase(
            @PathVariable Integer id, @RequestBody RamoPedidoDetalleValidarFraseRequestDto request) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<>(ramoPedidoDetalleService.validarFrase(id, request)));
        } catch (Exception e) {
            log.error("Error al validar frase del detalle {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }
}
