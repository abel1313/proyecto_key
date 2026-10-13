package com.ventas.key.hexagonal.datosprueba.dominio.modelo;

import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.PedidosPruebaPort;

import java.util.Locale;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>R20 (2026-10-13, pedido del dueño: "en la venta se pone el nombre exacto de la prueba para que
 * coincida en todos lados"): los nombres con los que se ven los datos de una prueba en pantalla. Un solo
 * lugar para que lo que crea el servicio, lo que busca la limpieza y lo que dice el documento de pruebas
 * sean siempre lo mismo.
 *
 * <ul>
 *   <li>El <b>cliente</b> de cada pedido se llama como la prueba y la clave: <b>"Prueba 24 K4"</b>. Es lo que
 *       se ve en la tarjeta del pedido y lo que busca el buscador de Mis pedidos y el de Unir (nombre de
 *       3 letras o más). Un pedido no lleva su clave escrita en ningún otro campo que se vea.</li>
 *   <li>Los <b>modelos</b> se llaman <b>"QA Prueba 17 Art-A"</b>: sin número al final, para que el nombre sea el
 *       del documento. El código de barras (2098…) es lo que los distingue entre sí.</li>
 * </ul>
 */
public final class NombresDePrueba {

    private static final String DOMINIO_CORREO = "@ejemplo.invalid";

    private NombresDePrueba() {
    }

    /** Cómo se ve el cliente de un pedido en pantalla: "Prueba 24 K4". */
    public static String cliente(String prueba, String clave) {
        return "Prueba " + prueba + " " + clave;
    }

    /**
     * Correo del cliente con cuenta de un pedido 🕓 Pendiente. {@code .invalid} es un dominio reservado:
     * no le llega a nadie. Uno por prueba y clave, para que cada pedido tenga su propio nombre.
     */
    public static String correo(String prueba, String clave) {
        return "cliente.prueba.qa." + slug(prueba) + "." + slug(clave) + DOMINIO_CORREO;
    }

    /** Nombre de un modelo de la prueba: "QA Prueba 17 Art-A"; sin etiqueta, "QA Prueba 24 Artículo". */
    public static String modelo(String prueba, String etiqueta) {
        return prefijoModelos(prueba) + (etiqueta == null || etiqueta.isBlank() ? "Artículo" : etiqueta);
    }

    /**
     * Con lo que empieza el nombre de todos los modelos de esa prueba ("QA Prueba 17 "). El espacio del
     * final evita confundir la prueba 1 con la 12.
     */
    public static String prefijoModelos(String prueba) {
        return "QA Prueba " + prueba + " ";
    }

    /** La nota que lleva el pedido (no se muestra en pantalla; sirve para reconocerlo en la base). */
    public static String observacion(String prueba, String clave) {
        return PedidosPruebaPort.MARCA_PEDIDO + " Prueba " + prueba + " · " + clave;
    }

    private static String slug(String texto) {
        return texto.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }
}
