package com.ventas.key.mis.productos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ventas.key.mis.productos.entity.Venta;
import com.ventas.key.mis.productos.models.PagoMPRequest;
import com.ventas.key.mis.productos.models.TotalDetalle;
import com.ventas.key.mis.productos.models.VentaDirectaRequest;
import com.ventas.key.mis.productos.models.VentaDirectaResponse;
import com.ventas.key.mis.productos.service.MercadoPagoService;
import com.ventas.key.mis.productos.service.VentaServiceImpl;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("ventas")
@Slf4j
public class VentaControllerImpl {

    private final VentaServiceImpl vImpl;
    private final MercadoPagoService mercadoPagoService;

    public VentaControllerImpl(
        final VentaServiceImpl vImpl,
        final MercadoPagoService mercadoPagoService
    ){
        this.vImpl = vImpl;
        this.mercadoPagoService = mercadoPagoService;
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
}
