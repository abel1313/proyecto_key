package com.ventas.key.mis.productos.dto;

import com.ventas.key.mis.productos.entity.EstadoCargaImagen;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Estado de la carga rapida de imagenes, leido directo de producto/variante — no hay
// tabla de seguimiento aparte. Se usa como response de subir-imagen, estado, fallidas
// y reintentar-imagen en CargaImagenesController.
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EstadoCargaProductoDto {

    private Integer productoId;
    private Integer varianteId;
    private EstadoCargaImagen estadoImagen;
    private Long imagenId;
    private String urlImagen;
    private String mensajeErrorImagen;
}
