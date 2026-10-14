package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

/**
 * Algo que se dio de alta para una prueba (R18): el nombre que le da la prueba (Art-A, M-OFF, RC...),
 * cómo encontrarlo en pantalla y qué es. Casi siempre un modelo o un artículo; desde R23 (2026-10-14)
 * también una rifa, un código de "Agregar mi compra" o un usuario de prueba.
 *
 * @param tipo         MODELO, ARTICULO, RIFA, CODIGO o USUARIO
 * @param modeloId     el modelo (0 si no es catálogo)
 * @param articuloId   el artículo (en un MODELO, su primer artículo; 0 si no es catálogo)
 * @param codigoBarras lo que se copia al tocarlo en el recuadro 🧪: el código de barras del modelo; en un
 *                     CODIGO, el código de "Agregar mi compra"; en un USUARIO, el nombre de usuario
 */
public record CatalogoPreparado(String clave, String tipo, int modeloId, int articuloId, String nombre,
                                String codigoBarras, String queEs) {

    public static final String MODELO = "MODELO";
    public static final String ARTICULO = "ARTICULO";
    public static final String RIFA = "RIFA";
    public static final String CODIGO = "CODIGO";
    public static final String USUARIO = "USUARIO";

    /** Un dato que no es catálogo (rifa, código o usuario). */
    public static CatalogoPreparado otro(String clave, String tipo, String nombre, String paraCopiar, String queEs) {
        return new CatalogoPreparado(clave, tipo, 0, 0, nombre, paraCopiar, queEs);
    }
}
