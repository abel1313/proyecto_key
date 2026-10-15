package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Arma modelos de prueba con nombres, tallas, colores y precios que parecen de la tienda (bolsas,
 * pantalones, faldas, blusas, vestidos, accesorios). Con la misma semilla sale siempre lo mismo,
 * asi una prueba que falla se puede repetir.
 *
 * <p>Solo arma los datos: no guarda nada.
 */
public final class CatalogoAleatorio {

    /** R4: codigos 2098 + 9 digitos. El rango 2099 ya lo usa datos_prueba_qa_catalogo.sql. */
    public static final String PREFIJO_CODIGO = "2098";
    public static final int STOCK_MIN = 6;
    public static final int STOCK_MAX = 20;

    private static final String[] COLORES = {
            "Negro", "Blanco", "Beige", "Camel", "Rojo", "Azul marino", "Verde olivo", "Rosa palo",
            "Gris", "Café", "Mostaza", "Vino", "Lila", "Turquesa"};
    private static final String[] TALLAS_ROPA = {"CH", "M", "G", "XG"};
    private static final String[] TALLAS_PANTALON = {"26", "28", "30", "32", "34", "36"};
    private static final String[] TALLA_UNICA = {"Única"};

    private enum Categoria {
        BOLSAS("Bolsas", TALLA_UNICA, 189, 899,
                new String[]{"Bolsa tote", "Bolsa bandolera", "Bolsa de mano", "Mochila", "Cartera", "Clutch"},
                new String[]{"de piel sintética", "de lona", "tejida", "acolchada", "de charol", "de gamuza"}),
        PANTALONES("Pantalones", TALLAS_PANTALON, 249, 699,
                new String[]{"Pantalón skinny", "Pantalón palazzo", "Jeans mom", "Pantalón cargo", "Pantalón de vestir"},
                new String[]{"de mezclilla", "de lino", "de gabardina", "stretch", "de tiro alto"}),
        FALDAS("Faldas", TALLAS_ROPA, 179, 549,
                new String[]{"Falda midi", "Falda plisada", "Falda de mezclilla", "Minifalda", "Falda larga"},
                new String[]{"floreada", "lisa", "con abertura", "de satín", "a cuadros"}),
        BLUSAS("Blusas", TALLAS_ROPA, 149, 449,
                new String[]{"Blusa manga corta", "Blusa de botones", "Blusa off shoulder", "Crop top", "Blusa de manga larga"},
                new String[]{"de lino", "de gasa", "de algodón", "con encaje", "estampada"}),
        VESTIDOS("Vestidos", TALLAS_ROPA, 299, 899,
                new String[]{"Vestido corto", "Vestido midi", "Vestido largo", "Vestido camisero", "Vestido de coctel"},
                new String[]{"floreado", "liso", "de satín", "con vuelo", "de punto"}),
        ACCESORIOS("Accesorios", TALLA_UNICA, 79, 349,
                new String[]{"Cinturón", "Mascada", "Gorra", "Lentes de sol", "Collar", "Aretes"},
                new String[]{"de piel sintética", "de seda", "dorado", "plateado", "trenzado"});

        final String nombre;
        final String[] tallas;
        final int precioMin;
        final int precioMax;
        final String[] tipos;
        final String[] estilos;

        Categoria(String nombre, String[] tallas, int precioMin, int precioMax, String[] tipos, String[] estilos) {
            this.nombre = nombre;
            this.tallas = tallas;
            this.precioMin = precioMin;
            this.precioMax = precioMax;
            this.tipos = tipos;
            this.estilos = estilos;
        }
    }

    private CatalogoAleatorio() {
    }

    /** Las categorias que usa el generador, para darlas de alta si no existen. */
    public static List<String> categorias() {
        List<String> nombres = new ArrayList<>();
        for (Categoria c : Categoria.values()) {
            nombres.add(c.nombre);
        }
        return nombres;
    }

    public static String codigoBarras(long numero) {
        return PREFIJO_CODIGO + String.format("%09d", numero);
    }

    /**
     * El modelo numero {@code numero} (el mismo que va en su codigo de barras).
     *
     * @param articulosMin minimo de articulos (1..4)
     * @param articulosMax maximo de articulos (1..4)
     */
    public static ModeloDePrueba modelo(long numero, int articulosMin, int articulosMax, Random rnd) {
        Categoria cat = Categoria.values()[rnd.nextInt(Categoria.values().length)];
        String tipo = elegir(cat.tipos, rnd);
        String estilo = elegir(cat.estilos, rnd);
        String colorBase = elegir(COLORES, rnd);

        double precio = redondearA9(cat.precioMin + rnd.nextInt(cat.precioMax - cat.precioMin + 1));
        // Uno de cada 3 trae "Otro precio" (rebaja menor); el resto rebaja = precio, como el alta.
        double rebaja = rnd.nextInt(3) == 0 ? Math.min(precio - 10, redondearA9(precio * 0.85)) : precio;
        double costo = Math.round(precio * (0.45 + rnd.nextDouble() * 0.15));

        String nombre = tipo + " " + estilo + " QA-" + numero;
        String descripcion = tipo + " " + estilo + ", color " + colorBase.toLowerCase()
                + ". Modelo de prueba generado para QA.";

        int cuantos = articulosMin + rnd.nextInt(articulosMax - articulosMin + 1);
        List<ArticuloDePrueba> articulos = articulos(cat, colorBase, cuantos, rnd);

        return new ModeloDePrueba(codigoBarras(numero), nombre, descripcion, cat.nombre, colorBase,
                costo, precio, rebaja, articulos);
    }

    /**
     * Ropa: mismo color en varias tallas. Talla unica (bolsas, accesorios): mismo modelo en varios
     * colores. Nunca dos articulos iguales (misma talla y color) en un modelo.
     */
    private static List<ArticuloDePrueba> articulos(Categoria cat, String colorBase, int cuantos, Random rnd) {
        Set<String> usados = new LinkedHashSet<>();
        List<ArticuloDePrueba> lista = new ArrayList<>();
        int intentos = 0;
        while (lista.size() < cuantos && intentos < 50) {
            intentos++;
            String talla = elegir(cat.tallas, rnd);
            String color = cat.tallas.length == 1 || lista.isEmpty() ? (lista.isEmpty() ? colorBase : elegir(COLORES, rnd)) : colorBase;
            if (usados.add(talla + "|" + color)) {
                int stock = STOCK_MIN + rnd.nextInt(STOCK_MAX - STOCK_MIN + 1);
                lista.add(new ArticuloDePrueba(talla, color, stock));
            }
        }
        return lista;
    }

    private static String elegir(String[] opciones, Random rnd) {
        return opciones[rnd.nextInt(opciones.length)];
    }

    /** 289, 349, 449: los precios de la tienda terminan en 9. */
    private static double redondearA9(double valor) {
        long decena = Math.round(valor / 10.0);
        return Math.max(9, decena * 10 - 1);
    }
}
