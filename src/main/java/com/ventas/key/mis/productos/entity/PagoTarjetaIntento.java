package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tabla {@code pago_tarjeta_intento}: un intento de cobro con tarjeta del dominio
 * {@code hexagonal/pagotarjeta}. Vive aqui (y no en el dominio) porque el escaneo de JPA solo cubre
 * este paquete. No tiene nada que ver con {@link MpPaymentIntent} (flujo viejo de la terminal).
 *
 * <p>Las fechas se guardan en UTC (DATETIME(3)), sin depender de la zona del servidor.
 * {@code version} es el candado optimista: dos procesos que cambian el mismo cobro a la vez no se
 * pisan, el segundo falla. La columna {@code activo_pedido} de la tabla (generada, con indice unico)
 * impide dos cobros activos del mismo pedido; no se mapea porque la calcula la base.
 */
@Entity
@Table(name = "pago_tarjeta_intento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoTarjetaIntento extends BaseId {

    @Column(name = "pedido_id", nullable = false)
    private Integer pedidoId;

    @Column(name = "cliente_id")
    private Integer clienteId;

    @Column(nullable = false, length = 20)
    private String canal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private Integer cuotas;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "id_externo", length = 100)
    private String idExterno;

    @Column(name = "iniciado_por", nullable = false, length = 100)
    private String iniciadoPor;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal devuelto;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @Version
    @Column(nullable = false)
    private Long version;
}
