package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector mínimo de JSON para la marca {@code ##PEDIDO{...}##}. Existe para que el dominio siga siendo Java puro
 * (sin Jackson). Entiende objetos, arreglos, texto, números, true, false y null; lo demás lo rechaza. Tiene tope de
 * tamaño y de profundidad porque lo que lee lo escribió un modelo de IA a partir de lo que escribió un cliente.
 */
final class JsonPlano {

    private static final int MAX_LARGO = 6000;
    private static final int MAX_PROFUNDIDAD = 6;

    private final String s;
    private int i;

    private JsonPlano(String s, int desde) {
        this.s = s;
        this.i = desde;
    }

    /** Resultado: el valor leído y la posición justo después de él. */
    record Leido(Object valor, int fin) {
    }

    static Leido leerDesde(String texto, int desde) {
        if (texto == null || texto.length() - desde > MAX_LARGO * 4) {
            throw new IllegalArgumentException("JSON demasiado largo");
        }
        JsonPlano p = new JsonPlano(texto, desde);
        Object v = p.valor(0);
        if (p.i - desde > MAX_LARGO) {
            throw new IllegalArgumentException("JSON demasiado largo");
        }
        return new Leido(v, p.i);
    }

    private Object valor(int profundidad) {
        if (profundidad > MAX_PROFUNDIDAD) {
            throw new IllegalArgumentException("JSON demasiado anidado");
        }
        espacios();
        if (i >= s.length()) {
            throw new IllegalArgumentException("JSON incompleto");
        }
        char c = s.charAt(i);
        if (c == '{') {
            return objeto(profundidad);
        }
        if (c == '[') {
            return arreglo(profundidad);
        }
        if (c == '"') {
            return texto();
        }
        if (s.startsWith("true", i)) {
            i += 4;
            return Boolean.TRUE;
        }
        if (s.startsWith("false", i)) {
            i += 5;
            return Boolean.FALSE;
        }
        if (s.startsWith("null", i)) {
            i += 4;
            return null;
        }
        return numero();
    }

    private Map<String, Object> objeto(int profundidad) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        i++; // {
        espacios();
        if (peek('}')) {
            i++;
            return mapa;
        }
        while (true) {
            espacios();
            if (!peek('"')) {
                throw new IllegalArgumentException("Se esperaba un nombre de campo");
            }
            String clave = texto();
            espacios();
            esperar(':');
            mapa.put(clave, valor(profundidad + 1));
            espacios();
            if (peek(',')) {
                i++;
                continue;
            }
            esperar('}');
            return mapa;
        }
    }

    private List<Object> arreglo(int profundidad) {
        List<Object> lista = new ArrayList<>();
        i++; // [
        espacios();
        if (peek(']')) {
            i++;
            return lista;
        }
        while (true) {
            lista.add(valor(profundidad + 1));
            espacios();
            if (peek(',')) {
                i++;
                continue;
            }
            esperar(']');
            return lista;
        }
    }

    private String texto() {
        i++; // comilla de apertura
        StringBuilder sb = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (i >= s.length()) {
                break;
            }
            char e = s.charAt(i++);
            switch (e) {
                case '"' -> sb.append('"');
                case '\\' -> sb.append('\\');
                case '/' -> sb.append('/');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    if (i + 4 > s.length()) {
                        throw new IllegalArgumentException("Escape \\u incompleto");
                    }
                    sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                    i += 4;
                }
                default -> throw new IllegalArgumentException("Escape desconocido");
            }
        }
        throw new IllegalArgumentException("Texto sin cerrar");
    }

    private Double numero() {
        int ini = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
            i++;
        }
        if (ini == i) {
            throw new IllegalArgumentException("Valor no reconocido");
        }
        return Double.valueOf(s.substring(ini, i));
    }

    private void espacios() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
    }

    private boolean peek(char c) {
        return i < s.length() && s.charAt(i) == c;
    }

    private void esperar(char c) {
        if (!peek(c)) {
            throw new IllegalArgumentException("Se esperaba '" + c + "'");
        }
        i++;
    }
}
