package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.*;

// Catálogo dinámico de variables de personalización visual (pantalla "Personalización", solo
// ADMIN) -- mismo patrón "dar de alta" que Menu/Submenu, no un singleton de columnas fijas: cada
// fila es una variable CSS editable, así que agregar una nueva no requiere tocar código.
//
// `clave` es el nombre del custom property CSS SIN el prefijo "--" (ej. "app-bg" -> se aplica
// como --app-bg). El front la usa tal cual, así que solo tiene efecto visual si algún .scss ya
// consume var(--esa-clave) -- ver TemaService (front) para el bucle que aplica todas las filas.
//
// `valorOscuro` puede quedar NULL: si no se da de alta, se usa `valorClaro` para los dos modos
// (variables estructurales como card-radius/card-shadow, que no son color y no cambian de noche).
@Entity
@Table(name = "tema_variable")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class TemaVariable extends BaseId {

    @Column(nullable = false, unique = true, length = 60)
    private String clave;

    @Column(nullable = false, length = 80)
    private String etiqueta;

    // Agrupa la pantalla de Personalización en secciones (ej. "Marca", "Card", "Sidebar").
    @Column(length = 40)
    private String grupo;

    // 'color' | 'numero' | 'seleccion' -- le dice al front qué input pintar. 'seleccion' hoy solo
    // aplica a card-shadow (el front trae su propio mapa fijo suave/media/fuerte).
    @Column(length = 20)
    private String tipo = "color";

    @Column(length = 200)
    private String valorClaro;

    @Column(length = 200)
    private String valorOscuro;

    // Orden de aparición dentro de su grupo en la pantalla de Personalización. NULL = al final.
    private Integer orden;
}
