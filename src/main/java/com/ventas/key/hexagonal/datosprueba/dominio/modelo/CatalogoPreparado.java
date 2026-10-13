package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

/**
 * Un modelo o artículo que se dio de alta para una prueba (R18): el nombre que le da la prueba
 * (Art-A, M-OFF...), cómo encontrarlo en pantalla (nombre del modelo y código de barras) y qué es.
 *
 * @param tipo       MODELO o ARTICULO
 * @param articuloId el artículo (en un MODELO, su primer artículo)
 */
public record CatalogoPreparado(String clave, String tipo, int modeloId, int articuloId, String nombre,
                                String codigoBarras, String queEs) {

    public static final String MODELO = "MODELO";
    public static final String ARTICULO = "ARTICULO";
}
