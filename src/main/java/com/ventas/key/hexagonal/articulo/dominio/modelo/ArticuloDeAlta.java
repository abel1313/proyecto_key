package com.ventas.key.hexagonal.articulo.dominio.modelo;

/**
 * Un articulo tal como llega en el alta, antes de guardarse.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Responde las dos preguntas que el alta necesita y que hasta el 2026-09-22 nadie hacia:
 * <b>¿esto describe un articulo de verdad?</b> y <b>¿que le falta que deba heredar del modelo?</b>
 *
 * <p>Solo trae los datos que hacen falta para decidir. Las imagenes no viajan aqui (pesan y no
 * hace falta mirarlas): basta saber <i>si trae</i>.
 *
 * @param id             null si es nuevo; con id es una edicion de uno que ya existe
 * @param talla          lo propio del articulo, lo unico que lo distingue de sus hermanos
 * @param color          idem
 * @param marca          idem
 * @param descripcion    idem
 * @param presentacion   idem
 * @param contenidoNeto  idem
 * @param stock          piezas
 * @param traeImagenes   si el alta le adjunto fotos
 * @param categoriaId    su categoria propia, o null para heredar la del modelo (R2)
 */
public record ArticuloDeAlta(
        Integer id,
        String talla,
        String color,
        String marca,
        String descripcion,
        String presentacion,
        String contenidoNeto,
        int stock,
        boolean traeImagenes,
        Integer categoriaId) {

    /**
     * Si esto describe un articulo de verdad, o es el formulario base que se lleno y se vacio (R1).
     *
     * <p>La pantalla de alta tiene el formulario base y la seccion de varias tallas, y son
     * independientes. Cuando alguien llena el base, agrega tallas y despues vacia el base, el base
     * seguia viajando: el back recibia 3 y creaba 3, y el tercero nacia sin talla, sin color y sin
     * nada -- eso es el "dice que voy a guardar 3 cuando agregue 2".
     *
     * <p>La regla es deliberadamente conservadora: descartar de mas seria perder un alta que
     * alguien hizo, asi que basta <b>cualquier</b> senal de contenido para conservarlo.
     */
    public boolean describeAlgo() {
        return yaExiste() || tieneStock() || traeImagenes || tieneAlgunDatoPropio();
    }

    /** Un articulo que ya existe nunca se descarta: vaciarle los campos es una edicion valida. */
    public boolean yaExiste() {
        return id != null;
    }

    private boolean tieneStock() {
        return stock > 0;
    }

    /**
     * Si le llenaron al menos un campo suyo.
     *
     * <p>Que sea "al menos uno" y no "todos" es a proposito: un articulo que solo se distingue por
     * el color es tan valido como uno con talla, color y marca.
     */
    public boolean tieneAlgunDatoPropio() {
        return hayTexto(talla)
                || hayTexto(color)
                || hayTexto(marca)
                || hayTexto(descripcion)
                || hayTexto(presentacion)
                || hayTexto(contenidoNeto);
    }

    /**
     * La categoria con la que se guarda: la suya si la trae, si no la del modelo (R2).
     *
     * <p>Heredar solo lo que falta -- y no pisar lo que vino -- permite que la mayoria salgan con
     * la del modelo y que alguno se separe, sin tener que elegirla articulo por articulo.
     */
    public Integer categoriaEfectiva(Integer categoriaDelModelo) {
        return categoriaId != null ? categoriaId : categoriaDelModelo;
    }

    /**
     * Un dato basico (color, marca, descripcion, contenido) con el que se guarda (R3).
     *
     * <p>Igual que la categoria: si el articulo nuevo lo trae vacio toma el del modelo, y si lo
     * trae lo conserva. Un articulo que ya existe no hereda: vaciarle un campo es una edicion.
     */
    public String datoEfectivo(String propio, String delModelo) {
        if (yaExiste() || hayTexto(propio) || !hayTexto(delModelo)) {
            return propio;
        }
        return delModelo;
    }

    private static boolean hayTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
