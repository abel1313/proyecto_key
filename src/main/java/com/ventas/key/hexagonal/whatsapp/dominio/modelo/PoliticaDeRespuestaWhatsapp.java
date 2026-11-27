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
        // Las marcas del pedido ({@code ##PEDIDO{..}##}, {@code ##CANCELAR[..]##}) y las «ref» del catálogo nunca llegan al
        // cliente, aunque el bot no tome pedidos (regla R16).
        String limpia = MARCAS.matcher(InterpreteDeMarcas.limpiar(cruda)).replaceAll("").trim();

        if (pideUnaPersona) {
            boolean sirve = !limpia.isBlank() && limpia.length() <= MAX_LARGO_AL_ESCALAR
                    && !PoliticaDeRespuesta.mencionaAlgoInterno(limpia);
            return new RespuestaDelBot.Escalar(sirve ? limpia : saludoEscalado(esPrimeraVez), MotivoDeEscalada.PIDIO_UNA_PERSONA);
        }
        if (limpia.isBlank() || PoliticaDeRespuesta.mencionaAlgoInterno(limpia)) {
            return new RespuestaDelBot.Escalar(saludoEscalado(esPrimeraVez), MotivoDeEscalada.FALLO_EL_ASISTENTE);
        }
        if (PoliticaDeRespuesta.soloSaluda(limpia)) {
            // Un "Hola" pelón no es una respuesta cordial (regla de respeto de todos los bots).
            return new RespuestaDelBot.Contestar(limpia + " ¿En qué te puedo ayudar? 😊");
        }
        return new RespuestaDelBot.Contestar(limpia);
    }

    public static String saludoEscalado(boolean esPrimeraVez) {
        return saludo(esPrimeraVez) + "En un momento te atendemos por aquí mismo 💖";
    }

    /** Foto, audio, sticker, ubicación o contacto sin texto: el bot no los puede leer, los ve una persona. */
    public static String saludoAdjunto(boolean esPrimeraVez) {
        return saludo(esPrimeraVez) + "Recibimos tu mensaje. En un momento te atendemos por aquí mismo 💖";
    }

    /** «¡Hola!» y la presentación solo en el primer mensaje; a mitad de la conversación se contesta directo. */
    static String saludo(boolean esPrimeraVez) {
        return esPrimeraVez ? "¡Hola! 😊 " + PRESENTACION : "";
    }

    /** Marca del primer aviso de fuera de horario: sirve para no repetirlo si el cliente insiste (regla R12). */
    public static final String INICIO_AVISO_FUERA_DE_HORARIO = "Ahorita no hay una persona disponible";

    /**
     * El cliente pidió una persona y ahorita no hay: se le dice el horario y se le ofrece seguir con el asistente.
     * No se aparta al bot ni se promete que "en un momento" lo atienden (regla R12).
     */
    public static String fueraDeHorario(boolean esPrimeraVez, HorarioDeAtencion horario) {
        return saludo(esPrimeraVez) + INICIO_AVISO_FUERA_DE_HORARIO + ": nuestro horario de atención es de "
                + horario.aTexto() + ". Si quieres, yo te sigo ayudando por aquí, o puedes esperar a ese horario "
                + "para que te atienda una persona 💖 ¿Qué prefieres?";
    }

    /** Marca del aviso corto (cuando insiste): también cuenta como «ya se le dijo el horario» (hallazgo M6). */
    public static final String INICIO_AVISO_REPETIDO = "Una persona te atiende desde las";

    /** Si el cliente insiste y ya se le dijo el horario: no se repite el aviso, se confirma y se queda disponible. */
    public static String fueraDeHorarioRepetido(HorarioDeAtencion horario) {
        return "¡Claro! 💖 " + INICIO_AVISO_REPETIDO + " " + horario.apertura12()
                + " (nuestro horario es de " + horario.aTexto() + "). Mientras tanto, si necesitas algo más, aquí estoy 😊";
    }

    /** El asistente no pudo contestar (el cliente no pidió una persona) y ahorita no hay nadie. */
    public static String fallaFueraDeHorario(boolean esPrimeraVez, HorarioDeAtencion horario) {
        return saludo(esPrimeraVez) + "Gracias por escribirnos 💖 Con eso no te puedo ayudar yo por aquí; una persona lo "
                + "revisa en nuestro horario de atención, de " + horario.aTexto() + ".";
    }

    /** Foto, audio u otro archivo cuando no hay nadie: se pide que lo escriba y se le dice cuándo lo ve una persona. */
    public static String adjuntoFueraDeHorario(boolean esPrimeraVez, HorarioDeAtencion horario) {
        return saludo(esPrimeraVez) + "Recibimos tu mensaje, pero por aquí no puedo ver fotos ni audios. Si me escribes "
                + "lo que necesitas te ayudo con gusto; si prefieres, una persona lo revisa en nuestro horario de "
                + "atención, de " + horario.aTexto() + " 💖";
    }
}
