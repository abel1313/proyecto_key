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
 * Filtros que un usuario dejó puestos en una pantalla. Reglas en el dominio hexagonal
 * {@code preferenciafiltro}; vive aquí solo porque Spring escanea las entidades desde este paquete.
 */
@Entity
@Table(name = "preferencia_filtro",
        uniqueConstraints = @UniqueConstraint(name = "uq_preferencia_filtro", columnNames = {"usuario_id", "pantalla"}))
@Getter
@Setter
@NoArgsConstructor
public class PreferenciaFiltro extends BaseId {

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "pantalla", nullable = false, length = 40)
    private String pantalla;

    @Column(name = "filtros", nullable = false, columnDefinition = "TEXT")
    private String filtros;

    @Column(name = "actualizado", nullable = false)
    private LocalDateTime actualizado;
}
