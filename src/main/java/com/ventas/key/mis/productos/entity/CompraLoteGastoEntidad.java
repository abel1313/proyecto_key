package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Tabla {@code compra_lote_gasto}: un gasto extra opcional del viaje (gasolina, bolsas...). */
@Entity
@Table(name = "compra_lote_gasto")
@Getter
@Setter
@NoArgsConstructor
public class CompraLoteGastoEntidad extends BaseId {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false)
    private CompraLoteEntidad lote;

    @Column(nullable = false, length = 100)
    private String concepto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "cuenta_para_cubrir", nullable = false)
    private boolean cuentaParaCubrir = true;
}
