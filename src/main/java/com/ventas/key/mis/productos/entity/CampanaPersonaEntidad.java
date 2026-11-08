package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Tabla {@code campana_persona} (migration_campana_personas.sql): una persona con código de campaña. El {@code uuid} es único y es suyo para siempre. */
@Entity
@Table(name = "campana_persona")
@Getter
@Setter
@NoArgsConstructor
public class CampanaPersonaEntidad extends BaseId {

    @Column(nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(length = 150)
    private String nombre;

    @Column(length = 150)
    private String correo;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false)
    private boolean consentimiento;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
}
