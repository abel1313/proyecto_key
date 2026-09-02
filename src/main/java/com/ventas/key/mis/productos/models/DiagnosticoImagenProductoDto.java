package com.ventas.key.mis.productos.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DiagnosticoImagenProductoDto {

    private Integer productoId;
    private String nombreProducto;
    private int totalImagenesLocalDB;
    private List<ImagenDiagnosticoItem> imagenesLocalDB;
    private boolean imagenPresenteEnMicroservicio;
    private String detalleExternoLista;

    // El listado/búsqueda (productos/buscar, tienda/buscar) NO usa la imagen completa de arriba
    // -- usa una MINIATURA aparte (v1/imagenes/thumbnail/{imagenId}) que el back arma como texto
    // y manda al front sin verificar nunca que exista de verdad. Encontrado 2026-09-02: un
    // producto podía verse bien en detalle-producto (imagen completa) y salir sin imagen en el
    // listado, porque la miniatura específica no existía en el microservicio aunque la imagen
    // completa sí. Estos dos campos prueban esa URL exacta, por separado.
    private boolean miniaturaPresenteEnMicroservicio;
    private String detalleMiniatura;
}
