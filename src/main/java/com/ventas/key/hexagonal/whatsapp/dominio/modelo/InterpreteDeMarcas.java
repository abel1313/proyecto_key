package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee las marcas de pedido que deja el modelo de IA al final de su respuesta (regla R16):
 * {@code ##PEDIDO{...json...}##} cuando ya tiene los datos y {@code ##CANCELAR[número]##} cuando el cliente quiere cancelar.
 * Y limpia todo eso (y las «ref» internas del catálogo) para que el cliente nunca lo lea.
 *
 * <p>Java puro. Lo que el modelo escribe es una propuesta, no una orden: aquí solo se lee, sin validar nada.</p>
 */
public final class InterpreteDeMarcas {

    static final String INICIO_PEDIDO = "##PEDIDO";
    private static final Pattern CANCELAR = Pattern.compile("##CANCELAR\\[\\s*#?\\s*(\\d{1,9})\\s*\\]##");
    private static final Pattern CANCELAR_CUALQUIERA = Pattern.compile("##CANCELAR(\\[[^\\]]*\\])?##");
    // La «ref» es el id interno del artículo en el catálogo que ve el modelo: no es para el cliente.
    private static final Pattern REF = Pattern.compile(
            "\\s*[\\[(]\\s*\\bref\\b\\s*:?\\s*\\d+\\s*[\\])]|\\s*\\bref\\b\\s*:?\\s*\\d+\\b", Pattern.CASE_INSENSITIVE);

    private InterpreteDeMarcas() {
    }

    public static boolean contieneMarcaDePedido(String cruda) {
        return cruda != null && cruda.contains(INICIO_PEDIDO);
    }

    /** @throws MarcaDePedidoInvalidaException si la marca está rota */
    public static SolicitudDePedido leerPedido(String cruda) {
        int ini = cruda == null ? -1 : cruda.indexOf(INICIO_PEDIDO);
        if (ini < 0) {
            throw new MarcaDePedidoInvalidaException("No hay marca de pedido");
        }
        int llave = llaveDespuesDeLaMarca(cruda, ini);
        if (llave < 0) {
            throw new MarcaDePedidoInvalidaException("La marca no trae datos");
        }
        JsonPlano.Leido leido;
        try {
            leido = JsonPlano.leerDesde(cruda, llave);
        } catch (RuntimeException e) {
            throw new MarcaDePedidoInvalidaException("JSON de la marca inválido: " + e.getMessage());
        }
        if (!(leido.valor() instanceof Map<?, ?> m)) {
            throw new MarcaDePedidoInvalidaException("La marca no es un objeto");
        }
        return aSolicitud(m);
    }

    public static OptionalInt numeroDeCancelacion(String cruda) {
        if (cruda == null) {
            return OptionalInt.empty();
        }
        Matcher m = CANCELAR.matcher(cruda);
        if (!m.find()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(m.group(1)));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    public static boolean contieneMarcaDeCancelacion(String cruda) {
        return cruda != null && CANCELAR_CUALQUIERA.matcher(cruda).find();
    }

    /**
     * Quita {@code ##PEDIDO{...}##} (si el JSON está roto o sin cerrar, quita desde {@code ##PEDIDO} hasta el final:
     * es preferible perder texto a mostrar al cliente un JSON), {@code ##CANCELAR[..]##} y las «ref» del catálogo.
     */
    public static String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        String t = texto;
        int ini = t.indexOf(INICIO_PEDIDO);
        while (ini >= 0) {
            int fin = finDeLaMarca(t, ini);
            t = t.substring(0, ini) + (fin < 0 ? "" : t.substring(fin));
            ini = t.indexOf(INICIO_PEDIDO);
        }
        t = CANCELAR_CUALQUIERA.matcher(t).replaceAll("");
        t = REF.matcher(t).replaceAll("");
        return t.replaceAll("[ \\t]{2,}", " ").trim();
    }

    /** La «{» tiene que venir inmediatamente después de ##PEDIDO (solo espacios en medio); si no, no es una marca válida. */
    private static int llaveDespuesDeLaMarca(String t, int ini) {
        int i = ini + INICIO_PEDIDO.length();
        while (i < t.length() && Character.isWhitespace(t.charAt(i))) {
            i++;
        }
        return i < t.length() && t.charAt(i) == '{' ? i : -1;
    }

    private static int finDeLaMarca(String t, int ini) {
        int llave = llaveDespuesDeLaMarca(t, ini);
        if (llave < 0) {
            // «##PEDIDO##» o «##PEDIDO» suelto: se quita solo la marca, nunca texto ajeno que venga después.
            int fin = ini + INICIO_PEDIDO.length();
            return t.startsWith("##", fin) ? fin + 2 : fin;
        }
        try {
            int fin = JsonPlano.leerDesde(t, llave).fin();
            int cierre = t.indexOf("##", fin);
            // Solo espacios entre el JSON y el «##» de cierre.
            if (cierre >= 0 && t.substring(fin, cierre).isBlank()) {
                return cierre + 2;
            }
            return fin;
        } catch (RuntimeException e) {
            return -1;
        }
    }

    private static SolicitudDePedido aSolicitud(Map<?, ?> m) {
        List<LineaSolicitada> lineas = new ArrayList<>();
        Object arts = m.get("articulos");
        if (arts instanceof List<?> lista) {
            for (Object o : lista) {
                if (o instanceof Map<?, ?> a) {
                    lineas.add(new LineaSolicitada(entero(a.get("ref")), entero(a.get("cantidad"))));
                }
            }
        }
        return new SolicitudDePedido(lineas, texto(m.get("nombre")), booleano(m.get("telefonoDeWhatsapp")),
                texto(m.get("telefono")), texto(m.get("correo")), texto(m.get("recibe")), texto(m.get("direccion")),
                texto(m.get("lugar")), booleano(m.get("recogerEnLocal")), texto(m.get("observaciones")),
                booleano(m.get("irPagando")), booleano(m.get("usarDatosPrevios")));
    }

    private static String texto(Object o) {
        if (o == null) {
            return null;
        }
        String t = (o instanceof Double d && d == Math.rint(d) ? String.valueOf(d.longValue()) : String.valueOf(o)).trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean booleano(Object o) {
        if (o instanceof Boolean b) {
            return b;
        }
        return o instanceof String s && (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("si") || s.equalsIgnoreCase("sí"));
    }

    /** Solo un entero limpio: «1 o 2», «-7» o «[ref 5]» no valen (antes se quitaban los signos y salía 12 o 7). */
    private static Integer entero(Object o) {
        if (o instanceof Number n) {
            double d = n.doubleValue();
            return d == Math.rint(d) && d >= 0 && d < 1_000_000_000 ? (int) d : null;
        }
        if (o instanceof String s) {
            String t = s.trim();
            return t.matches("\\d{1,9}") ? Integer.valueOf(t) : null;
        }
        return null;
    }
}
