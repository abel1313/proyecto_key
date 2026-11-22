package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.text.Normalizer;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Lo que se contesta SIN llamar a la IA (cero tokens, respuesta predecible): precio, existencias, saludos, agradecimientos. Si el
 * mensaje no cae en nada de esto, se le pregunta al chatbot. Solo se anuncia el precio normal y si HAY o no (nunca la cantidad exacta).
 */
public final class RespuestasSinIa {

    private static final Pattern COMPRA = Pattern.compile(
            "\\b(lo quiero|la quiero|los quiero|las quiero|quiero comprar|quiero comprarlo|quiero comprarla|me lo apartas|me la apartas"
                    + "|apartamelo|apartamela|apartalo|apartala|me lo llevo|me la llevo|lo compro|la compro|me interesa"
                    + "|como lo compro|como la compro|como compro|quiero uno|quiero una|quiero apartar)\\b");
    private static final Pattern PRECIO = Pattern.compile("\\b(precio|cuanto cuesta|cuanto vale|cuanto sale|cuanto es|costo|cuesta|vale)\\b");
    private static final Pattern EXISTENCIA = Pattern.compile("\\b(hay|disponible|disponibles|existencia|existencias|queda|quedan|tienen|tiene)\\b");
    private static final Pattern GRACIAS = Pattern.compile("^(muchas )?gracias[ !.]*$|^gracias por (la )?info[a-z ]*[ !.]*$");
    // Si la pregunta trae un calificador ("hay EN negro", "cuesta la DE mano", "OTRA talla"), no es de plantilla: lo contesta el chatbot.
    private static final Pattern CALIFICADOR = Pattern.compile(
            "\\b(en|de|otro|otra|otros|otras|color|colores|talla|tallas|tamano|medida|medidas|mas|con|sin|para)\\b");
    private static final int MAX_PALABRAS = 6;
    private static final Pattern SALUDO = Pattern.compile("^(hola|buenas|buenos dias|buenas tardes|buenas noches|que tal)[ !.]*$");

    private RespuestasSinIa() {
    }

    /** Quiere comprar o apartar. Se detecta por palabras: es predecible y no cuesta tokens. */
    public static boolean esCompra(String texto) {
        return COMPRA.matcher(limpiar(texto)).find();
    }

    /** La respuesta de plantilla, o vacio si hay que preguntarle al chatbot. */
    public static Optional<String> para(String texto, ProductoParaPublicar producto) {
        String t = limpiar(texto);
        if (GRACIAS.matcher(t).find()) {
            return Optional.of("¡Con gusto! 💖");
        }
        if (SALUDO.matcher(t).find()) {
            return Optional.of("¡Hola! 😊 Dime, ¿en qué te ayudo con " + nombre(producto) + "?");
        }
        boolean pidePrecio = PRECIO.matcher(t).find();
        boolean pideExistencia = EXISTENCIA.matcher(t).find();
        // Solo cuando pregunta UNA cosa, corta y sin calificadores: "precio y en otro color" se lo contesta el chatbot.
        boolean sencilla = t.split(" ").length <= MAX_PALABRAS && !CALIFICADOR.matcher(t).find();
        if (pidePrecio && !pideExistencia && sencilla) {
            if (producto.precio() == null || producto.precio() <= 0) {
                return Optional.empty();
            }
            return Optional.of(nombre(producto) + " cuesta $" + Math.round(producto.precio()) + " MXN 💖");
        }
        if (pideExistencia && !pidePrecio && sencilla) {
            return Optional.of(producto.disponible() ? "Sí hay disponible ✅" : "Por ahora está agotado 😔");
        }
        return Optional.empty();
    }

    private static String nombre(ProductoParaPublicar p) {
        return p.nombre() == null || p.nombre().isBlank() ? "El artículo" : p.nombre().trim();
    }

    /** Minusculas, sin acentos ni signos de pregunta/exclamacion, con espacios simples: para comparar sin que importe como lo escribieron. */
    static String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        String s = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.replaceAll("[¿?¡]", " ").replaceAll("\\s+", " ").trim();
    }
}
