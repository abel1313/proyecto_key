package com.ventas.key.mis.productos.handleExeption;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduce los errores tecnicos (base de datos, Hibernate, Java) a un texto que entienda quien usa la pantalla. El detalle tecnico se queda en el log;
 * al front solo llega este texto. Un mensaje que ya esta escrito para personas (sin rastros tecnicos) se deja tal cual.
 */
public final class MensajesParaUsuario {

    private MensajesParaUsuario() {
    }

    /** Nombre amigable de lo que se repitio, segun la tabla de la restriccion (Duplicate entry 'X' for key 'tabla.indice'). */
    private static final Map<String, String> TABLAS = Map.ofEntries(
            Map.entry("codigo_barra", "El código de barras"), Map.entry("codigo_barras", "El código de barras"),
            Map.entry("variantes", "Ese artículo (talla y color)"), Map.entry("producto", "Ese producto"),
            Map.entry("productos", "Ese producto"), Map.entry("clientes", "Ese cliente"),
            Map.entry("clientes_sin_registro", "Ese cliente"), Map.entry("usuarios", "Ese usuario o correo"),
            Map.entry("usuario_modificacion", "Ese usuario o correo"), Map.entry("palabra_clave", "Esa categoría"),
            Map.entry("codigo_postal", "El código postal"), Map.entry("campana", "Esa campaña"),
            Map.entry("campana_persona", "Ese código de cliente"), Map.entry("rifas", "Esa rifa"),
            Map.entry("lugares_entrega", "Ese lugar de entrega"), Map.entry("roles", "Ese rol"));

    private static final Pattern DUPLICADO = Pattern.compile("Duplicate entry '(.*?)' for key '(?:([\\w$]+)\\.)?([\\w$]+)'", Pattern.DOTALL);
    private static final Pattern DATO_LARGO = Pattern.compile("Data too long for column '([\\w$]+)'");
    private static final Pattern SIN_VALOR = Pattern.compile("Column '([\\w$]+)' cannot be null|Field '([\\w$]+)' doesn't have a default value");

    /** Frases que solo aparecen en errores de la base o de Java (no palabras sueltas, para no tocar mensajes escritos para personas). */
    private static final String[] RASTROS_TECNICOS = {
            "could not execute", "could not commit", "could not extract", "jdbc", "hibernate", "sqlstate", "constraint fails", "constraintviolation",
            "org.springframework", "org.hibernate", "java.lang.", "java.sql.", "java.util.", "com.mysql", "jakarta.", "nullpointer",
            "duplicate entry", "data too long", "cannot be null", "doesn't have a default value", "foreign key constraint", "deadlock found",
            "lock wait timeout", "nested exception", "sql [", "stacktrace", "cannot invoke", "for input string", "no value present",
            "could not be parsed", "out of bounds for length", "json parse error", "unexpected character", "cannot deserialize"
    };

    /** ¿El mensaje parece de la base de datos o de Java (no se le puede mostrar a quien usa la pantalla)? */
    public static boolean esTecnico(String mensaje) {
        if (mensaje == null) {
            return false;
        }
        String m = mensaje.toLowerCase(Locale.ROOT);
        for (String r : RASTROS_TECNICOS) {
            if (m.contains(r)) {
                return true;
            }
        }
        return false;
    }

    /** El mismo mensaje si ya es claro; si es tecnico, su traduccion. Nunca devuelve null ni deja un rastro tecnico. */
    public static String paraUsuario(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return mensaje;
        }
        if (!esTecnico(mensaje)) {
            return mensaje;
        }
        return traducir(mensaje);
    }

    public static String traducir(String crudo) {
        String m = crudo == null ? "" : crudo;
        Matcher dup = DUPLICADO.matcher(m);
        if (dup.find()) {
            String valor = dup.group(1);
            String tabla = dup.group(2) == null ? "" : dup.group(2).toLowerCase(Locale.ROOT);
            String que = TABLAS.get(tabla);
            if (que == null) {
                String clave = dup.group(3).toLowerCase(Locale.ROOT);
                int mejor = -1;
                for (Map.Entry<String, String> e : TABLAS.entrySet()) {
                    if (clave.contains(e.getKey()) && e.getKey().length() > mejor) {
                        que = e.getValue();
                        mejor = e.getKey().length();
                    }
                }
            }
            // Un id interno (123) o una clave compuesta (12-34) no le dicen nada a quien usa la pantalla: se omite el valor.
            boolean mostrarValor = !valor.matches("[\\d\\-]+") || valor.length() > 6;
            return (que == null ? "Ese dato" : que) + (mostrarValor ? " «" + valor + "»" : "") + " ya está registrado. Escribe uno diferente.";
        }
        String bajo = m.toLowerCase(Locale.ROOT);
        Matcher largo = DATO_LARGO.matcher(m);
        if (largo.find()) {
            return "Uno de los textos es demasiado largo (" + legible(largo.group(1)) + "). Acórtalo e intenta de nuevo.";
        }
        Matcher sin = SIN_VALOR.matcher(m);
        if (sin.find()) {
            String col = sin.group(1) != null ? sin.group(1) : sin.group(2);
            return "Falta un dato obligatorio (" + legible(col) + "). Complétalo e intenta de nuevo.";
        }
        if (bajo.contains("cannot delete or update a parent row")) {
            return "No se puede eliminar porque ya se usa en otros registros (por ejemplo en pedidos o ventas).";
        }
        if (bajo.contains("cannot add or update a child row")) {
            return "Uno de los datos elegidos ya no existe o no es válido. Actualiza la pantalla e intenta de nuevo.";
        }
        if (bajo.contains("deadlock") || bajo.contains("lock wait")) {
            return "El sistema está ocupado con otro cambio. Espera unos segundos e intenta de nuevo.";
        }
        return "No se pudo guardar el cambio. Revisa los datos e intenta de nuevo; si sigue igual, avisa al administrador.";
    }

    private static String legible(String columna) {
        return columna == null ? "" : columna.replace('_', ' ');
    }
}
