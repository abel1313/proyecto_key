package com.ventas.key.mis.productos.entity;

import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un color vendible de un TipoFlor (ej. "Rosa eterna" + "Rojo"). El precio por flor vive en
// TipoFlor (todos los colores de la misma especie cuestan igual); aqui solo vive el stock real
// de ESE color especifico y su variante "sombra" (con el color seteado) para poder venderse.
@Entity
@Table(name = "color_flor")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ColorFlor extends BaseId {

    @ManyToOne
    @JoinColumn(name = "tipo_flor_id", nullable = false)
    private TipoFlor tipoFlor;

    @Column(nullable = false, length = 50)
    private String nombre;

    // Stock real de flores sueltas de este color -- el admin lo edita aqui, el service lo
    // sincroniza a la variante "sombra" de abajo (ver ProductoSombraServiceImpl).
    private Integer stock;

    @Column(nullable = false)
    private Boolean activo = true;

    // Variante "sombra" (con su Producto detras, color = este nombre) -- ver comentario en
    // ProductoSombraServiceImpl. La administra unicamente ColorFlorServiceImpl.
    @ManyToOne
    @JoinColumn(name = "variante_id")
    private Variantes variante;
}
