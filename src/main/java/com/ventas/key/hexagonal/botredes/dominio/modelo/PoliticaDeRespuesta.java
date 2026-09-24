package com.ventas.key.hexagonal.botredes.dominio.modelo;

import java.util.regex.Pattern;

/**
 * Reglas de qué contesta el bot (ver {@code botredes/README.md}). El bot nunca deja a la persona
 * sin respuesta: si no puede contestar él, saluda, avisa al admin y se pausa.
 */
public final class PoliticaDeRespuesta {

    static final String PRESENTACION = "Soy el asistente automático de Novedades Jade. ";
    static final String SALUDO_ESCALADO_COMENTARIO = "En un momento te compartimos la información 💖";
    static final String SALUDO_ESCALADO_MENSAJE = "En un momento te atendemos 💖";
    static final String SALUDO_CORDIAL_COMENTARIO = "Gracias por tu comentario 💖";
    static final String SALUDO_CORDIAL_MENSAJE = "Gracias por escribirnos, ¿en qué te podemos ayudar? 💖";

    // Frases que delatan cómo funciona el bot por dentro. El cliente nunca debe leer que "le
    // mandamos un correo a la dueña" ni que "se escaló" su pregunta (pedido del dueño 2026-09-24).
    // No incluye "correo" suelto: la tienda puede dar su correo de contacto.
    private static final Pattern ALGO_INTERNO = Pattern.compile(
            "##"
                    + "|\\bescal(ar|o|é|e|aré|amos|ado|ada|ando)\\b"
                    + "|\\badministrador(a)?\\b|\\badmin\\b"
                    + "|\\b(mand|envi)(é|e|o|amos|aré|aremos)\\s+(un\\s+)?(correo|mail|e-?mail|aviso)"
                    + "|\\b(notifiqu|notific)(é|e|amos|aré|ado)\\b"
                    + "|\\bbot\\s+en\\s+pausa\\b|\\bpausad[oa]\\b"
                    + "|\\binstrucciones\\b|\\bprompt\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern SALUDO_AL_INICIO = Pattern.compile(
            "^\\s*¡?\\s*(hola|buenas|buen d[ií]a|buenos d[ií]as|buenas tardes|buenas noches)\\s*[!,.]*\\s*(😊)?\\s*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private PoliticaDeRespuesta() {
    }

    /**
     * Traduce la respuesta cruda del chatbot, con sus marcas ##ESCALAR##, ##GRACIAS## (seguida de
     * la frase de agradecimiento), ##FAREWELL## y ##BUSCAR[..]##.
     */
    public static ResultadoDelCerebro interpretar(String respuesta) {
        if (respuesta == null) {
            return new ResultadoDelCerebro.NoEntendido();
        }
        if (respuesta.contains("##ESCALAR##")) {
            return new ResultadoDelCerebro.Escalar();
        }
        if (respuesta.contains("##GRACIAS##")) {
            return new ResultadoDelCerebro.Agradecer(respuesta.replace("##GRACIAS##", "").trim());
        }
        if (respuesta.contains("##FAREWELL##")) {
            return new ResultadoDelCerebro.NoEntendido();
        }
        String limpia = respuesta.replaceAll("##BUSCAR\\[[^\\]]*\\]##", "").trim();
        return limpia.isBlank() ? new ResultadoDelCerebro.NoEntendido() : new ResultadoDelCerebro.Contestar(limpia);
    }

    public static Accion decidir(Canal canal, ResultadoDelCerebro resultado, boolean esPrimeraVez) {
        if (resultado instanceof ResultadoDelCerebro.Contestar contestar) {
            if (mencionaAlgoInterno(contestar.texto())) {
                return escalar(canal, esPrimeraVez, "la respuesta del bot mencionaba algo interno y no se publicó");
            }
            if (soloSaluda(contestar.texto())) {
                return agradecer(canal, esPrimeraVez, null);
            }
            return new Accion(contestar.texto(), false, false, null);
        }
        if (resultado instanceof ResultadoDelCerebro.Agradecer agradecer) {
            return agradecer(canal, esPrimeraVez, agradecer.frase());
        }
        if (resultado instanceof ResultadoDelCerebro.Escalar) {
            return escalar(canal, esPrimeraVez, "el bot no tenía el dato");
        }
        // No entendió. En un comentario público basta con un saludo; en un mensaje directo la
        // persona espera respuesta, así que lo atiende el admin.
        return canal == Canal.COMENTARIO
                ? agradecer(canal, esPrimeraVez, null)
                : escalar(canal, esPrimeraVez, "el bot no entendió el mensaje");
    }

    /**
     * El saludo lo pone siempre el código; el chatbot solo aporta la frase según lo que la persona
     * comentó ("¡Muchas gracias por compartir! 💖"). Si la frase no sirve, va la de siempre.
     */
    private static Accion agradecer(Canal canal, boolean esPrimeraVez, String frase) {
        String cuerpo = frase == null ? "" : SALUDO_AL_INICIO.matcher(frase).replaceFirst("").trim();
        boolean sirve = !cuerpo.isBlank() && cuerpo.length() <= 200 && !mencionaAlgoInterno(cuerpo);
        if (!sirve) {
            cuerpo = canal == Canal.COMENTARIO ? SALUDO_CORDIAL_COMENTARIO : SALUDO_CORDIAL_MENSAJE;
        }
        return new Accion(saludo(canal, esPrimeraVez) + cuerpo, false, false, null);
    }

    // El chatbot a veces contesta un "¡Hola! 😊" pelón a un saludo. Eso no es una respuesta cordial
    // (pasó en QA el 2026-09-24): se cambia por el agradecimiento.
    public static boolean soloSaluda(String texto) {
        String letras = texto.replaceAll("[^\\p{L}\\s]", "").trim().toLowerCase();
        return letras.matches("(hola|buenas|buen d[ií]a|buenos d[ií]as|buenas tardes|buenas noches)");
    }

    public static boolean mencionaAlgoInterno(String texto) {
        return texto != null && ALGO_INTERNO.matcher(texto).find();
    }

    /** Foto, audio o sticker sin texto: el bot no los puede leer. */
    public static Accion paraAdjunto(Canal canal, boolean esPrimeraVez) {
        return escalar(canal, esPrimeraVez, "mandó una foto, audio o sticker");
    }

    public static Accion escalar(Canal canal, boolean esPrimeraVez, String motivo) {
        String texto = saludo(canal, esPrimeraVez)
                + (canal == Canal.COMENTARIO ? SALUDO_ESCALADO_COMENTARIO : SALUDO_ESCALADO_MENSAJE);
        return new Accion(texto, true, true, motivo);
    }

    // Solo en mensajes directos se presenta como asistente automático, y solo la primera vez. En
    // los comentarios públicos no (decisión del dueño, 2026-09-24).
    private static String saludo(Canal canal, boolean esPrimeraVez) {
        boolean presentarse = esPrimeraVez && canal == Canal.MENSAJE_DIRECTO;
        return "¡Hola! 😊 " + (presentarse ? PRESENTACION : "");
    }
}
