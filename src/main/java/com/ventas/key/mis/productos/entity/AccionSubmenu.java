package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.*;

// Fase 3 de permisos (2026-08-27, piloto en Modelos): dentro de una pantalla (Submenu), un
// boton/accion puntual configurable por rol (ej. "Modelos" -> habilitar, eliminar,
// crear-variantes, compartir-imagen, descargar-excel, filtros-admin). Mas fino que
// rol_submenu_escritura (que es un unico "Editar" para toda la pantalla): un rol puede tener
// Editar sin tener "eliminar", o viceversa. Requiere que el rol ya tenga el Ver (rol_submenu) de
// `submenu` -- lo garantiza RolesServiceImpl, no la BD (mismo criterio que rol_submenu_escritura).
@Entity
@Table(name = "accion_submenu")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class AccionSubmenu extends BaseId {

    @ManyToOne
    @JoinColumn(name = "submenu_id", nullable = false)
    private Submenu submenu;

    // Identificador estable que usa el back para el authority ("PANTALLA_<ruta>_ACCION_<clave>")
    // y el front para el checkbox (ej. "habilitar", "eliminar", "crear-variantes").
    @Column(nullable = false, length = 60)
    private String clave;

    // Texto para mostrar en Gestión de roles (ej. "Habilitar / deshabilitar producto").
    @Column(nullable = false, length = 120)
    private String etiqueta;

    // Texto largo opcional para el tooltip del checkbox en Gestión de roles -- explica en dónde
    // exactamente aparece esta acción en la pantalla real (2026-08-28, pedido del usuario tras
    // separar "filtros-admin" en un permiso por checkbox: quería que cada opción "dijera" dónde
    // se va a poner sin tener que adivinar). Null en las acciones de antes de esta fecha.
    @Column(length = 255)
    private String descripcion;

    // Sub-encabezado para agrupar visualmente el checklist de Gestión de roles (2026-09-04,
    // pedido del usuario: antes las acciones de una pantalla salian todas juntas en una sola
    // lista -- confuso con 15+ casillas). Ej. "Filtros", "Tarjeta de modelo", "Buscador". Null =
    // sin agrupar, cae en "Otras acciones" en el front. El AGRUPAMIENTO en pantalla lo decide
    // esta columna, pero el ORDEN dentro y entre grupos sigue siendo `orden` -- las filas de una
    // misma categoria deben quedar con `orden` consecutivo para que el front las muestre juntas
    // (agrupa por bloques contiguos, no reordena).
    @Column(length = 60)
    private String categoria;

    private Integer orden;
}
