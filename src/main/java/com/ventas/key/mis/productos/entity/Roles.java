package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Roles extends BaseId {

    @Column(name = "nombre_rol", nullable = false)
    private String nombreRol;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "rol_permiso",
            joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "permiso_id")
    )
    private Set<Permiso> permisos = new HashSet<>();

    // Pantallas base del rol -- Fase 1 de PLAN_PERMISOS_PANTALLAS.md (repo compartido). Separado
    // de "permisos" a proposito: esto es visibilidad de pantalla, no accion sobre datos.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "rol_submenu",
            joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "submenu_id")
    )
    private Set<Submenu> submenus = new HashSet<>();

    // Fase 2 de permisos de accion (2026-08-27): de las pantallas que el rol YA puede ver
    // (arriba), cuales ademas puede escribir (crear/editar/borrar), no solo mirar. Un submenu
    // aqui SIEMPRE debe estar tambien en "submenus" -- lo garantiza RolesServiceImpl, no la BD.
    // Sin esta distincion, dar una pantalla era todo-o-nada (ver == poder editar).
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "rol_submenu_escritura",
            joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "submenu_id")
    )
    private Set<Submenu> submenusEscritura = new HashSet<>();

    @JsonIgnore
    @ManyToMany(mappedBy = "roles")
    private Set<Usuario> usuarios = new HashSet<>();
}