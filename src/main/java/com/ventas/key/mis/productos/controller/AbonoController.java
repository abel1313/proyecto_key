package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.abonos.*;
import com.ventas.key.mis.productos.service.api.IAbonoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/abonos")
@RequiredArgsConstructor
public class AbonoController {

    private final IAbonoService abonoService;

    @PostMapping("/{pedidoId}")
    public ResponseEntity<ResponseGeneric<AbonoResponse>> registrarAbono(
            @PathVariable int pedidoId,
            @Valid @RequestBody AbonoRequest request) {
        try {
            AbonoResponse response = abonoService.registrarAbono(pedidoId, request);
            return ResponseEntity.ok(new ResponseGeneric<>(response));
        } catch (Exception e) {
            ResponseGeneric<AbonoResponse> error = new ResponseGeneric<>((AbonoResponse) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/{pedidoId}")
    public ResponseEntity<ResponseGeneric<List<AbonoResponse>>> obtenerAbonos(@PathVariable int pedidoId) {
        try {
            List<AbonoResponse> response = abonoService.obtenerAbonos(pedidoId);
            return ResponseEntity.ok(new ResponseGeneric<List<AbonoResponse>>(response));
        } catch (Exception e) {
            ResponseGeneric<List<AbonoResponse>> error = new ResponseGeneric<List<AbonoResponse>>((List<AbonoResponse>) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/reporte/estado-cuenta")
    public ResponseEntity<ResponseGeneric<List<EstadoCuentaDto>>> reporteEstadoCuenta() {
        try {
            List<EstadoCuentaDto> response = abonoService.reporteEstadoCuenta();
            return ResponseEntity.ok(new ResponseGeneric<List<EstadoCuentaDto>>(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseGeneric<>(null));
        }
    }

    @GetMapping("/reporte/pagados")
    public ResponseEntity<ResponseGeneric<List<ReportePagadosDto>>> reportePagados() {
        try {
            List<ReportePagadosDto> response = abonoService.reportePagados();
            return ResponseEntity.ok(new ResponseGeneric<List<ReportePagadosDto>>(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseGeneric<>(null));
        }
    }

    @GetMapping("/reporte/cancelados")
    public ResponseEntity<ResponseGeneric<List<ReporteCanceladosDto>>> reporteCancelados() {
        try {
            List<ReporteCanceladosDto> response = abonoService.reporteCancelados();
            return ResponseEntity.ok(new ResponseGeneric<List<ReporteCanceladosDto>>(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseGeneric<>(null));
        }
    }

    @PutMapping("/{pedidoId}/cancelar")
    public ResponseEntity<ResponseGeneric<CancelarAbonoResponse>> cancelarPedido(
            @PathVariable int pedidoId,
            @RequestBody(required = false) CancelarAbonoRequest request) {
        try {
            CancelarAbonoResponse response = abonoService.cancelarPedido(pedidoId,
                    request != null ? request : new CancelarAbonoRequest());
            return ResponseEntity.ok(new ResponseGeneric<>(response));
        } catch (Exception e) {
            ResponseGeneric<CancelarAbonoResponse> error = new ResponseGeneric<>((CancelarAbonoResponse) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/{pedidoIdOrigen}/transferir")
    public ResponseEntity<ResponseGeneric<TransferirAbonoResponse>> transferirAbono(
            @PathVariable int pedidoIdOrigen,
            @Valid @RequestBody TransferirAbonoRequest request) {
        try {
            TransferirAbonoResponse response = abonoService.transferirAbono(pedidoIdOrigen, request);
            return ResponseEntity.ok(new ResponseGeneric<>(response));
        } catch (Exception e) {
            ResponseGeneric<TransferirAbonoResponse> error = new ResponseGeneric<>((TransferirAbonoResponse) null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
