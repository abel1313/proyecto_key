package com.ventas.key.mis.productos.models;


import com.ventas.key.mis.productos.mapper.ProductoAdmin;
import com.ventas.key.mis.productos.mapper.ProductoUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProductoDTO {
    private String nombre;
    private Double precioCosto;
    private Double piezas;
    private String color;
    private Double precioVenta;
    private Double precioRebaja;
    private String descripcion;
    private Integer stock;
    private String marca;
    private String contenido;
    private String codigoBarras;
    private int idProducto;
    private char habilitado;


    public ProductoDTO(ProductoAdmin productoAdmin) {
        this.nombre = productoAdmin.getNombre();
        this.color = productoAdmin.getColor();
        this.precioCosto = productoAdmin.getPrecioCosto();
        this.piezas = productoAdmin.getPiezas();
        this.color = productoAdmin.getColor();
        this.precioVenta = productoAdmin.getPrecioVenta();
        this.precioRebaja = productoAdmin.getPrecioRebaja();
        this.descripcion = productoAdmin.getDescripcion();
        this.stock = productoAdmin.getStock();
        this.marca = productoAdmin.getMarca();
        this.contenido = productoAdmin.getContenido();
        this.codigoBarras = productoAdmin.getCodigoBarras();
        this.idProducto = productoAdmin.getIdProducto();
        this.habilitado = productoAdmin.getHabilitado();
    }

    public ProductoDTO(ProductoUser productoUser) {
        this.nombre = productoUser.getNombre();
        this.color = productoUser.getColor();
        this.precioVenta = productoUser.getPrecioVenta();
        this.descripcion = productoUser.getDescripcion();
        this.codigoBarras = productoUser.getCodigoBarras();
        this.idProducto = productoUser.getIdProducto();

    }
}
