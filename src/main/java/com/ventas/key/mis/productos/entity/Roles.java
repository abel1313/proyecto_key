package com.ventas.key.mis.productos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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

    // @Fetch(SELECT) en las 4 colecciones de este archivo: sin esto, las 4 (EAGER +
    // @ManyToMany) se traen con JOIN en la MISMA query que carga el Roles -- y como
    // Usuario.roles es EAGER, cargar un solo Usuario terminaba en un SELECT con estas 4
    // colecciones unidas por JOIN a la vez (más permisosExtra de Usuario, ver Usuario.java).
    // El producto cartesiano de 5 colecciones unidas (ej. 20 acciones x 15 permisos x 10
    // submenus x ...) se multiplica en vez de sumarse, y puede llegar a millones de filas para
    // un solo usuario -- encontrado en QA 2026-09-02: OutOfMemoryError: Java heap space
    // cargando un usuario via loadUserByUsername, que se ejecuta en CADA peticion autenticada
    // (ver JwtAuthenticationFilter), no solo en el login. Con @Fetch(SELECT) cada colección se
    // trae en su propio SELECT separado -- sigue siendo EAGER, solo que ya no se multiplica.
    @Fetch(FetchMode.SELECT)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "rol_permiso",
            joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "permiso_id")
    )
    private Set<Permiso> permisos = new HashSet<>();

    // Pantallas base del rol -- Fase 1 de PLAN_PERMISOS_PANTALLAS.md (repo compartido). Separado
    // de "permisos" a proposito: esto es visibilidad de pantalla, no accion sobre datos.
    @Fetch(FetchMode.SELECT)
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
    @Fetch(FetchMode.SELECT)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "rol_submenu_escritura",
            joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "submenu_id")
    )
    private Set<Submenu> submenusEscritura = new HashSet<>();

    // Fase 3 de permisos (2026-08-27, piloto en Modelos): acciones puntuales dentro de una
    // pantalla que el rol puede usar (ej. "eliminar", "habilitar" en Modelos), independientes
    // entre si y de "submenusEscritura" -- un rol puede tener Editar sin "eliminar", o viceversa.
    // Cada AccionSubmenu aqui SIEMPRE debe pertenecer a un Submenu que el rol ya tenga en
    // "submenus" -- lo garantiza RolesServiceImpl, no la BD.
    @Fetch(FetchMode.SELECT)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "rol_accion",
            joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "accion_submenu_id")
    )
    private Set<AccionSubmenu> acciones = new HashSet<>();

    @JsonIgnore
    @ManyToMany(mappedBy = "roles")
    private Set<Usuario> usuarios = new HashSet<>();
}