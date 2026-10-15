package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Un código postal que ya se consultó al servicio externo. Reglas en el dominio hexagonal
 * {@code codigopostal}; vive aquí solo porque Spring escanea las entidades desde este paquete.
 * Las colonias se guardan como un arreglo JSON de textos.
 */
@Entity
@Table(name = "codigo_postal",
        uniqueConstraints = @UniqueConstraint(name = "uq_codigo_postal", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
public class CodigoPostalGuardado extends BaseId {

    @Column(name = "codigo", nullable = false, length = 5)
    private String codigo;

    @Column(name = "municipio", nullable = false, length = 150)
    private String municipio;

    @Column(name = "estado", length = 150)
    private String estado;

    @Column(name = "colonias", nullable = false, columnDefinition = "TEXT")
    private String colonias;

    @Column(name = "consultado", nullable = false)
    private LocalDateTime consultado;
}
