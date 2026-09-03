package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.PagoOnline;
import com.ventas.key.mis.productos.entity.Venta;
import com.ventas.key.mis.productos.repository.IPagoOnlineRepository;
import com.ventas.key.mis.productos.repository.IVentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Orquesta el reembolso de un pago online (Checkout Pro MP / PayPal) cuando hay una devolución
 * real -- 2026-09-03, respondiendo a la duda: "¿qué pasa si se lleva el producto y después
 * cancela, se le regresaría el dinero Y el producto?".
 *
 * A PROPÓSITO no está enganchado dentro de PedidoServiceImpl.deletePedidoById (cancelar/devolver
 * el pedido) -- son dos pasos DELIBERADAMENTE separados:
 * 1. El admin cancela el pedido (ya existe, ver deletePedidoById) -- ahí se regresa el stock y la
 *    Venta pasa a "Devuelta". Esto YA asume que el admin verificó que el producto físico volvió
 *    (el sistema no tiene forma de comprobar eso solo, es responsabilidad operativa humana).
 * 2. Si además se cobró por una pasarela online, el admin dispara el reembolso APARTE, en un
 *    segundo clic explícito (POST /v1/pagos-online/{pedidoId}/reembolsar). Nunca es automático
 *    con el solo hecho de cancelar -- eso evitaría exactamente el riesgo que se preguntó: que un
 *    clic accidental o mal intencionado devuelva el dinero sin que el producto haya vuelto.
 * Este servicio exige que el paso 1 ya haya pasado (Venta.estadoVenta == "Devuelta") antes de
 * dejar reembolsar -- no se puede reembolsar un pedido que no se canceló primero.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoOnlineService {

    private final IPagoOnlineRepository iPagoOnlineRepository;
    private final IVentaRepository iVentaRepository;
    private final MercadoPagoCheckoutService mercadoPagoCheckoutService;
    private final PayPalCheckoutService payPalCheckoutService;

    @Transactional
    public void reembolsarPorPedido(Integer pedidoId) {
        Venta venta = iVentaRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new RuntimeException("El pedido " + pedidoId + " no tiene una venta asociada"));
        if (!"Devuelta".equals(venta.getEstadoVenta())) {
            throw new RuntimeException("El pedido " + pedidoId + " todavía no está cancelado/devuelto -- "
                    + "cancélalo primero (eso confirma que el producto ya regresó) antes de reembolsar el dinero");
        }

        PagoOnline pago = iPagoOnlineRepository.findByPedidoIdOrderByFechaCreacionDesc(pedidoId,
                        org.springframework.data.domain.PageRequest.of(0, 1))
                .stream().filter(p -> "APPROVED".equals(p.getEstado())).findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "El pedido " + pedidoId + " no tiene un pago online aprobado que reembolsar "
                                + "(si se cobró en efectivo/terminal, ese reembolso se maneja aparte, no por aquí)"));

        if (pago.getPagoIdExterno() == null || pago.getPagoIdExterno().isBlank()) {
            throw new RuntimeException("El pago online del pedido " + pedidoId + " no tiene un id de pago externo registrado");
        }

        switch (pago.getProveedor()) {
            case MercadoPagoCheckoutService.PROVEEDOR -> mercadoPagoCheckoutService.reembolsar(pago.getPagoIdExterno());
            case PayPalCheckoutService.PROVEEDOR -> payPalCheckoutService.reembolsar(pago.getPagoIdExterno());
            default -> throw new RuntimeException("Proveedor de pago desconocido: " + pago.getProveedor());
        }

        pago.setEstado("REFUNDED");
        pago.setFechaUpdate(LocalDateTime.now());
        iPagoOnlineRepository.save(pago);
        log.info("Pedido {} reembolsado por {} (pago online {})", pedidoId, pago.getProveedor(), pago.getId());
    }
}
