package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.DetallePago;
import com.ventas.key.mis.productos.entity.IvaTerminal;
import com.ventas.key.mis.productos.entity.PagosYMeses;
import com.ventas.key.mis.productos.entity.TarifaTerminal;
import com.ventas.key.mis.productos.entity.TipoPago;
import com.ventas.key.mis.productos.models.OpcionPagoDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.PagosCatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("pagos")
@RequiredArgsConstructor
public class PagosCatalogoController {

    private final PagosCatalogoService pagosCatalogoService;

    @GetMapping("/tipos-pago")
    public ResponseEntity<ResponseGeneric<List<TipoPago>>> getTiposPago() {
        try {
            return ResponseEntity.ok(new ResponseGeneric<List<TipoPago>>(pagosCatalogoService.getTiposPago()));
        } catch (Exception e) {
            ResponseGeneric<List<TipoPago>> error = new ResponseGeneric<>(null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/tarifas")
    public ResponseEntity<ResponseGeneric<List<TarifaTerminal>>> getTarifas() {
        try {
            return ResponseEntity.ok(new ResponseGeneric<List<TarifaTerminal>>(pagosCatalogoService.getTarifasTerminal()));
        } catch (Exception e) {
            ResponseGeneric<List<TarifaTerminal>> error = new ResponseGeneric<>(null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/iva")
    public ResponseEntity<ResponseGeneric<List<IvaTerminal>>> getIva() {
        try {
            return ResponseEntity.ok(new ResponseGeneric<List<IvaTerminal>>(pagosCatalogoService.getIva()));
        } catch (Exception e) {
            ResponseGeneric<List<IvaTerminal>> error = new ResponseGeneric<>(null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/opciones")
    public ResponseEntity<ResponseGeneric<List<DetallePago>>> getOpciones() {
        try {
            return ResponseEntity.ok(new ResponseGeneric<List<DetallePago>>(pagosCatalogoService.getOpcionesPago()));
        } catch (Exception e) {
            ResponseGeneric<List<DetallePago>> error = new ResponseGeneric<>(null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/opciones-estructuradas")
    public ResponseEntity<ResponseGeneric<List<OpcionPagoDto>>> getOpcionesEstructuradas() {
        try {
            return ResponseEntity.ok(new ResponseGeneric<List<OpcionPagoDto>>(pagosCatalogoService.getOpcionesEstructuradas()));
        } catch (Exception e) {
            ResponseGeneric<List<OpcionPagoDto>> error = new ResponseGeneric<>(null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/opciones-por-tipo/{tipoPagoId}")
    public ResponseEntity<ResponseGeneric<List<PagosYMeses>>> getOpcionesPorTipo(@PathVariable Integer tipoPagoId) {
        try {
            return ResponseEntity.ok(new ResponseGeneric<List<PagosYMeses>>(pagosCatalogoService.getOpcionesPorTipo(tipoPagoId)));
        } catch (Exception e) {
            ResponseGeneric<List<PagosYMeses>> error = new ResponseGeneric<>(null);
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}