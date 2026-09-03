package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Tabla unificada para pagos online (Checkout Pro de Mercado Pago y PayPal) -- decidido 2026-09-03
// que NO viven en Pedido (evita agregarle columnas de pasarela a una entidad que ya es grande) y
// comparten una sola tabla en vez de una por proveedor, a diferencia de MpPaymentIntent que es
// solo para Point (terminal fisica, un flujo completamente distinto -- no se toca ni se mezcla
// con esta tabla).
@Entity
@Table(name = "pago_online")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PagoOnline extends BaseId {

    // "MP_CHECKOUT" | "PAYPAL"
    @Column(nullable = false, length = 20)
    private String proveedor;

    @Column(name = "pedido_id", nullable = false)
    private Integer pedidoId;

    @Column(name = "cliente_id", nullable = false)
    private Integer clienteId;

    // MP: preference id devuelto al crear la Preference. PayPal: order id devuelto al crear la orden.
    @Column(name = "referencia_externa", nullable = false, length = 120)
    private String referenciaExterna;

    // MP: payment.id una vez que el cliente paga (llega por webhook/back_url). PayPal: capture id.
    // Null mientras el pago sigue pendiente de completarse.
    @Column(name = "pago_id_externo", length = 120)
    private String pagoIdExterno;

    @Column(nullable = false)
    private Double monto;

    // Estado normalizado propio (no el string crudo de cada pasarela): CREATED, APPROVED,
    // REJECTED, CANCELLED, REFUNDED -- ver PagoOnlineEstado. Cada servicio de pasarela traduce el
    // estado propio de MP/PayPal a este set comun.
    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_update")
    private LocalDateTime fechaUpdate;
}
