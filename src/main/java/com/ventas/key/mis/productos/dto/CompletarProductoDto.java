package com.ventas.key.mis.productos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Body de PUT /v1/carga-imagenes/{productoId}/completar. Todos los campos son opcionales:
// el front manda solo lo que el usuario ya lleno en ese momento, cada campo no nulo
// sobreescribe el valor actual del producto/variante borrador.
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CompletarProductoDto {

    private String nombre;
    private Double precioCosto;
    private Double piezas;
    private String color;
    private Double precioVenta;
    private Double precioRebaja;
    private String descripcion;
    private String marca;
    private String contenido;
    private Integer palabraClaveId;

    // Codigo de barras REAL. Mientras no se mande, el producto conserva el placeholder
    // autogenerado en la carga rapida. En cuanto llega uno distinto al placeholder,
    // se crea el codigo real, se reasigna al producto y se borra el placeholder anterior.
    private String codigoBarras;

    // true para publicar el producto en el catalogo (habilitado=1). Se rechaza si el
    // producto todavia tiene el codigo de barras autogenerado.
    private Boolean habilitar;
}
