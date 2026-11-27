package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.text.Normalizer;
import java.util.Set;

/**
 * ¿El cliente dijo «sí» al resumen? Se decide aquí, con reglas fijas, y NO con el modelo de IA: crear un pedido nunca
 * depende de cómo se interprete un mensaje (regla R16). Solo cuenta un «sí» limpio: «sí, pero en talla M» no es un sí, y
 * un texto con signo de interrogación («¿ya está listo?», «ok?») nunca confirma (es una pregunta, no una respuesta).
 */
public final class Confirmacion {

    /** SI: confirma. AMBIGUA: parece un sí pero con pregunta (se pide «responde solo SÍ»). OTRA: cualquier otra cosa. */
    public enum Resultado { SI, AMBIGUA, OTRA }

    private static final Set<String> FUERTES = Set.of(
            "si", "sii", "siii", "sip", "claro", "ok", "okey", "oki", "okay", "va", "dale", "listo", "confirmo",
            "confirmado", "correcto", "perfecto", "adelante", "hazlo", "yes", "simon", "exacto", "afirmativo");
    private static final Set<String> RELLENO = Set.of(
            "por", "favor", "gracias", "de", "acuerdo", "asi", "es", "esta", "bien", "todo", "que", "sea", "el", "pedido",
            "mi", "lo", "quiero", "confirmar", "porfa", "porfavor", "ya", "muchas");
    private static final int MAX_PALABRAS = 8;

    private Confirmacion() {
    }

    public static boolean esAfirmativa(String texto) {
        return evaluar(texto) == Resultado.SI;
    }

    public static Resultado evaluar(String texto) {
        if (texto == null || texto.isBlank()) {
            return Resultado.OTRA;
        }
        boolean pregunta = texto.contains("?") || texto.contains("¿");
        if (!pregunta && (texto.contains("👍") || texto.contains("✅") || texto.contains("👌"))) {
            String sinEmoji = texto.replaceAll("[👍✅👌\\p{So}\\p{Sk}\\s\\uFE0F]", "");
            if (sinEmoji.isEmpty()) {
                return Resultado.SI;
            }
        }
        String[] palabras = normalizar(texto).trim().split("\\s+");
        if (palabras.length == 0 || palabras.length > MAX_PALABRAS || palabras[0].isEmpty()) {
            return Resultado.OTRA;
        }
        boolean hayFuerte = false;
        for (String p : palabras) {
            if (FUERTES.contains(p)) {
                hayFuerte = true;
            } else if (!RELLENO.contains(p)) {
                return Resultado.OTRA;
            }
        }
        if (!hayFuerte) {
            return Resultado.OTRA;
        }
        return pregunta ? Resultado.AMBIGUA : Resultado.SI;
    }

    /** Minúsculas, sin acentos, y solo letras y espacios (fuera signos y emojis). */
    static String normalizar(String t) {
        String sin = Normalizer.normalize(t.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sin.replaceAll("[^a-z\\s]", " ").replaceAll("\\s+", " ");
    }
}
