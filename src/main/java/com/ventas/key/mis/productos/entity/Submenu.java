package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.*;

// Item real del menu que navega a una pantalla (ej. "Modelos" -> productos/buscar) -- ver
// PLAN_PERMISOS_PANTALLAS.md (repo compartido). Es lo que en ese documento se llama "Pantalla":
// la unidad que se le asigna a un rol/usuario para decidir que ve en su menu (Fase 1). `menu`
// nulo = item de nivel superior sin grupo (ej. Home, Tienda, Favoritos, Chat, QR, Login) --
// mismo criterio que ya usa navbar.component.html para esos casos (fuera de cualquier acordeon).
@Entity
@Table(name = "submenu")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Submenu extends BaseId {

    @ManyToOne
    @JoinColumn(name = "menu_id")
    private Menu menu;

    @Column(nullable = false, length = 80)
    private String nombre;

    // routerLink de Angular tal cual aparece en el template (ej. "productos/buscar"), sin "/"
    // inicial -- mismo formato que ya usa GROUP_ROUTES en navbar.component.ts.
    @Column(nullable = false, length = 150)
    private String ruta;

    @Column(length = 10)
    private String icono;

    // Texto para el boton info (ℹ️) en Gestion de roles: que es esta pantalla y donde vive en el
    // menu (2026-08-28, mismo pedido que ya se resolvio para las acciones puntuales de
    // AccionSubmenu -- "cada opcion" incluye tambien el Ver/Editar de cada pantalla, no solo las
    // acciones finas). Null en pantallas viejas sin describir todavia.
    @Column(length = 255)
    private String descripcion;

    // Texto para el boton info (ℹ️) del checkbox "Editar" en Gestion de roles (2026-09-04) --
    // distinto de `descripcion` (que explica el "Ver"). Hace falta aparte porque el back
    // (SecurityConfig.pantallaEscribir) a veces comparte el permiso de escritura entre varias
    // pantallas hermanas via OR (ej. Modelos + Agregar modelo + Agregar producto son la MISMA
    // authority "escribir"), algo que no se puede adivinar solo mirando la pantalla actual -- un
    // usuario marco "Editar" en Modelos y nunca vio que hiciera nada ahi, porque Modelos es solo
    // buscar/listar, sin formulario propio: el efecto se nota en las otras pantallas del grupo.
    // Null = usa el texto generico de verInfoVerEditar() en el front.
    @Column(length = 255)
    private String descripcionEscritura;

    private Integer orden;
}
