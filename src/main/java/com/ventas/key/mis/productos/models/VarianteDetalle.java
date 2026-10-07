package com.ventas.key.mis.productos.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VarianteDetalle {

    private Integer id;
    private Integer productoId;
    private Integer palabraClaveId;
    private String talla;
    private String descripcion;
    private String color;
    private String presentacion;
    private int stock;
    private String marca;
    private String contenidoNeto;
    private List<ImagenDTO> listImagenes = new ArrayList<>();
    private Long imagenPrincipalId;
    /**
     * Agregar (+) o quitar (-) stock al MODELO en el mismo guardado (Agregar articulo, 2026-10-06,
     * PLAN_ALTA_MODELO_Y_ARTICULOS.md B1). Se toma el primero que venga por modelo; null o 0 = no
     * tocar. El modelo nunca queda con menos de lo que ya esta repartido.
     */
    private Integer ajusteStockModelo;
}