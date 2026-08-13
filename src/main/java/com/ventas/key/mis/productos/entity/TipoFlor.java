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

@Entity
@Table(name = "tipo_flor")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TipoFlor extends BaseId {

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "precio_por_flor", nullable = false)
    private Double precioPorFlor;

    @Column(name = "precio_costo")
    private Double precioCosto;

    // Stock real de flores sueltas disponibles -- el admin lo edita aqui, el service lo
    // sincroniza a la variante "sombra" de abajo (ver ProductoSombraServiceImpl).
    private Integer stock;

    @Column(nullable = false)
    private Boolean activo = true;

    // Variante "sombra" (con su Producto detras) que representa este tipo de flor en el
    // catalogo general -- permite que una linea de "N flores de este tipo" se venda a traves
    // del flujo normal de Pedido/Venta (stock, precio, cancelacion, Mercado Pago) sin tocar
    // ese codigo. La administra unicamente TipoFlorServiceImpl, nunca se edita a mano.
    @ManyToOne
    @JoinColumn(name = "variante_id")
    private Variantes variante;
}
