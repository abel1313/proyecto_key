package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import com.ventas.key.hexagonal.botredes.dominio.modelo.PoliticaDeRespuesta;

import java.util.regex.Pattern;

/**
 * Traduce lo que contestó el chatbot (con sus marcas {@code ##HUMANO##}, {@code ##ESCALAR##},
 * {@code ##FAREWELL##}, {@code ##BUSCAR[..]##}) en lo que se le manda al cliente (regla R9).
 *
 * <p>Reusa el filtro de "algo interno" de {@code botredes}: lo que el bot nunca debe decir al
 * cliente (cómo funciona por dentro, qué IA es, códigos de barras, existencias exactas) es la misma
 * regla en todos los canales.</p>
 */
public final class PoliticaDeRespuestaWhatsapp {

    static final String PRESENTACION = "Soy el asistente automático de Novedades Jade. ";

    // Cualquier marca ##ALGO## o ##ALGO[...]## que el modelo haya dejado.
    private static final Pattern MARCAS = Pattern.compile("##[A-Z]+(\\[[^\\]]*\\])?##");

    // Si el modelo acompañó la marca con un texto largo, no se manda: se usa el saludo de siempre.
    private static final int MAX_LARGO_AL_ESCALAR = 300;

    private PoliticaDeRespuestaWhatsapp() {
    }

    public static RespuestaDelBot interpretar(String respuestaCruda, boolean esPrimeraVez) {
        String cruda = respuestaCruda == null ? "" : respuestaCruda;
        boolean pideUnaPersona = cruda.contains("##HUMANO##") || cruda.contains("##ESCALAR##");
        String limpia = MARCAS.matcher(cruda).replaceAll("").trim();

        if (pideUnaPersona) {
            boolean sirve = !limpia.isBlank() && limpia.length() <= MAX_LARGO_AL_ESCALAR
                    && !PoliticaDeRespuesta.mencionaAlgoInterno(limpia);
            return new RespuestaDelBot.Escalar(sirve ? limpia : saludoEscalado(esPrimeraVez));
        }
        if (limpia.isBlank() || PoliticaDeRespuesta.mencionaAlgoInterno(limpia)) {
            return new RespuestaDelBot.Escalar(saludoEscalado(esPrimeraVez));
        }
        if (PoliticaDeRespuesta.soloSaluda(limpia)) {
            // Un "Hola" pelón no es una respuesta cordial (regla de respeto de todos los bots).
            return new RespuestaDelBot.Contestar(limpia + " ¿En qué te puedo ayudar? 😊");
        }
        return new RespuestaDelBot.Contestar(limpia);
    }

    public static String saludoEscalado(boolean esPrimeraVez) {
        return "¡Hola! 😊 " + (esPrimeraVez ? PRESENTACION : "") + "En un momento te atendemos por aquí mismo 💖";
    }

    /** Foto, audio, sticker, ubicación o contacto sin texto: el bot no los puede leer, los ve una persona. */
    public static String saludoAdjunto(boolean esPrimeraVez) {
        return "¡Hola! 😊 " + (esPrimeraVez ? PRESENTACION : "")
                + "Recibimos tu mensaje. En un momento te atendemos por aquí mismo 💖";
    }
}
