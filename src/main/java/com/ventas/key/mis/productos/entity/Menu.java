package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.*;

// Grupo del menu lateral (ej. "Catalogo", "Envios", "Sistema") -- ver PLAN_PERMISOS_PANTALLAS.md
// (repo compartido). Reemplaza el string "grupo" fijo que hoy vive hardcodeado en
// navbar.component.ts (GROUP_ROUTES) por una tabla editable desde un admin nuevo. Las rutas
// reales que llevan a cada pantalla estan en Submenu, no aqui -- un Menu es solo el encabezado
// del acordeon, nunca navega por si solo.
@Entity
@Table(name = "menu")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Menu extends BaseId {

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    // Emoji, mismo criterio que el resto del sistema (sin imagenes) -- ej. "📦".
    @Column(length = 10)
    private String icono;

    // Orden de aparicion en el sidebar. NULL = al final, sin orden especifico.
    private Integer orden;
}
