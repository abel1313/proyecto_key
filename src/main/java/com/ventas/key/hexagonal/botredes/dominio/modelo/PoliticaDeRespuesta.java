package com.ventas.key.hexagonal.botredes.dominio.modelo;

/**
 * Reglas de qué contesta el bot (ver {@code botredes/README.md}). El bot nunca deja a la persona
 * sin respuesta: si no puede contestar él, saluda, avisa al admin y se pausa.
 */
public final class PoliticaDeRespuesta {

    static final String PRESENTACION = "Soy el asistente automático de Novedades Jade. ";
    static final String SALUDO_ESCALADO_COMENTARIO = "En un momento te compartimos la información 💖";
    static final String SALUDO_ESCALADO_MENSAJE = "En un momento te atendemos 💖";
    static final String SALUDO_CORDIAL_COMENTARIO = "Gracias por tu comentario 💖";

    private PoliticaDeRespuesta() {
    }

    /** Traduce la respuesta cruda del chatbot, con sus marcas ##ESCALAR##, ##FAREWELL## y ##BUSCAR[..]##. */
    public static ResultadoDelCerebro interpretar(String respuesta) {
        if (respuesta == null) {
            return new ResultadoDelCerebro.NoEntendido();
        }
        if (respuesta.contains("##ESCALAR##")) {
            return new ResultadoDelCerebro.Escalar();
        }
        if (respuesta.contains("##FAREWELL##")) {
            return new ResultadoDelCerebro.NoEntendido();
        }
        String limpia = respuesta.replaceAll("##BUSCAR\\[[^\\]]*\\]##", "").trim();
        return limpia.isBlank() ? new ResultadoDelCerebro.NoEntendido() : new ResultadoDelCerebro.Contestar(limpia);
    }

    public static Accion decidir(Canal canal, ResultadoDelCerebro resultado, boolean esPrimeraVez) {
        if (resultado instanceof ResultadoDelCerebro.Contestar contestar) {
            return new Accion(contestar.texto(), false, false, null);
        }
        if (resultado instanceof ResultadoDelCerebro.Escalar) {
            return escalar(canal, esPrimeraVez, "el bot no tenía el dato");
        }
        // No entendió. En un comentario público basta con un saludo; en un mensaje directo la
        // persona espera respuesta, así que lo atiende el admin.
        return canal == Canal.COMENTARIO
                ? new Accion(saludo(esPrimeraVez) + SALUDO_CORDIAL_COMENTARIO, false, false, null)
                : escalar(canal, esPrimeraVez, "el bot no entendió el mensaje");
    }

    /** Foto, audio o sticker sin texto: el bot no los puede leer. */
    public static Accion paraAdjunto(Canal canal, boolean esPrimeraVez) {
        return escalar(canal, esPrimeraVez, "mandó una foto, audio o sticker");
    }

    public static Accion escalar(Canal canal, boolean esPrimeraVez, String motivo) {
        String texto = saludo(esPrimeraVez)
                + (canal == Canal.COMENTARIO ? SALUDO_ESCALADO_COMENTARIO : SALUDO_ESCALADO_MENSAJE);
        return new Accion(texto, true, true, motivo);
    }

    private static String saludo(boolean esPrimeraVez) {
        return "¡Hola! 😊 " + (esPrimeraVez ? PRESENTACION : "");
    }
}
