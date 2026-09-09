package com.ventas.key.mis.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Excepcion de pantalla por usuario individual, encima de lo que ya da su rol via rol_submenu.
// concedido=true  -> se le suma esta pantalla aunque su rol no la de.
// concedido=false -> se le quita esta pantalla aunque su rol si la de.
// Formula (ver PLAN_PERMISOS_PANTALLAS.md seccion 3):
//   efectivas = (pantallas_del_rol U {concedido=true}) - {concedido=false}
@Entity
@Table(name = "usuario_submenu", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "submenu_id"}))
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class UsuarioSubmenu extends BaseId {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "submenu_id", nullable = false)
    private Submenu submenu;

    @Column(nullable = false)
    private Boolean concedido;
}
