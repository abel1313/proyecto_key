package com.ventas.key.mis.productos.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import com.ventas.key.mis.productos.entity.Venta;
import com.ventas.key.mis.productos.models.PagoMPRequest;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.TotalDetalle;
import com.ventas.key.mis.productos.models.VentaDirectaRequest;
import com.ventas.key.mis.productos.models.VentaDirectaResponse;
import com.ventas.key.mis.productos.repository.IVentaRepository;
import com.ventas.key.mis.productos.service.MercadoPagoService;
import com.ventas.key.mis.productos.service.VentaServiceImpl;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/v1/ventas")
@Slf4j
public class VentaControllerImpl {

    private final VentaServiceImpl vImpl;
    private final MercadoPagoService mercadoPagoService;
    private final IVentaRepository iVentaRepository;

    public VentaControllerImpl(
        final VentaServiceImpl vImpl,
        final MercadoPagoService mercadoPagoService,
        final IVentaRepository iVentaRepository
    ){
        this.vImpl = vImpl;
        this.mercadoPagoService = mercadoPagoService;
        this.iVentaRepository = iVentaRepository;
    }

    @PostMapping("/save")
    public ResponseEntity<VentaDirectaResponse> save(@RequestBody VentaDirectaRequest request) throws Exception {
        VentaDirectaResponse response = this.vImpl.saveVentaDetalle(request);

        if (response.isRequiereTerminal()) {
            try {
                PagoMPRequest pagoMPRequest = new PagoMPRequest();
                pagoMPRequest.setPedidoId(response.getVentaId());
                pagoMPRequest.setClienteId(request.getClienteId());
                pagoMPRequest.setPagosYMesesId(request.getPagosYMesesId());
                pagoMPRequest.setTotalMonto(response.getTotalVenta());
                pagoMPRequest.setDescripcion(response.getDescripcionPago());

                String intentId = mercadoPagoService.iniciarPago(pagoMPRequest);
                response.setIntentId(intentId);
            } catch (Exception e) {
                log.error("Venta {} guardada pero falló el intent de MP: {}", response.getVentaId(), e.getMessage());
                throw e;
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @PostMapping("/getVentas")
    public ResponseEntity<List<Venta>> getVentas(@RequestParam int size, 
                                            @RequestParam int page,
                                            @RequestParam String nombre ) throws Exception{
        return ResponseEntity.status(HttpStatus.OK).body(this.vImpl.findAll(page,size) );
    }
        @GetMapping("/getTotalVentas")
    public ResponseEntity<List<TotalDetalle>> getTotalVentas() throws Exception{
        return ResponseEntity.status(HttpStatus.OK).body(this.vImpl.getTotalDetalle() );
    }

    @PostMapping("/reclamar")
    public ResponseEntity<ResponseGeneric<String>> reclamar(@RequestBody Map<String, String> body) {
        try {
            vImpl.reclamarVenta(body.get("codigo"));
            return ResponseEntity.ok(new ResponseGeneric<>("Compra vinculada a tu cuenta"));
        } catch (Exception e) {
            log.error("Error al reclamar venta: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @PostMapping("/{ventaId}/asignarCliente")
    public ResponseEntity<ResponseGeneric<String>> asignarCliente(
            @PathVariable Integer ventaId, @RequestBody Map<String, Integer> body) {
        try {
            vImpl.asignarClienteManual(ventaId, body.get("clienteId"));
            return ResponseEntity.ok(new ResponseGeneric<>("Cliente vinculado a la venta"));
        } catch (Exception e) {
            log.error("Error al asignar cliente a venta {}: {}", ventaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<ResponseGeneric<PginaDto<List<Venta>>>> buscar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            LocalDate desde = fecha != null ? fecha : (fechaInicio != null ? fechaInicio : LocalDate.now());
            LocalDate hasta = fecha != null ? fecha : (fechaFin != null ? fechaFin : desde);

            LocalDateTime desdeTs = desde.atStartOfDay();
            LocalDateTime hastaTs = hasta.atTime(LocalTime.MAX);

            Page<Venta> resultado = iVentaRepository.buscarPorFecha(desdeTs, hastaTs, PageRequest.of(page, size));

            PginaDto<List<Venta>> dto = new PginaDto<>();
            dto.setT(resultado.getContent());
            dto.setTotalRegistros((int) resultado.getTotalElements());
            dto.setTotalPaginas(resultado.getTotalPages());
            dto.setPagina(page);

            return ResponseEntity.ok(new ResponseGeneric<>(dto));
        } catch (Exception e) {
            log.error("Error al buscar ventas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseGeneric<>(null, e.getMessage()));
        }
    }
}
