package com.ventas.key.mis.productos.filter;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reglas de deteccion para {@link InputSanitizationFilter}. Cada patron busca una firma
 * COMBINADA/contextual, no un caracter suelto -- por ejemplo un apostrofe solo (nombre "O'Brien")
 * nunca dispara nada, pero "' OR '1'='1" si, porque combina comilla + operador logico +
 * tautologia. Eso evita bloquear datos legitimos con caracteres especiales sueltos.
 */
final class DetectorPatronesMaliciosos {

    private DetectorPatronesMaliciosos() {
    }

    // ── XSS / HTML-JS inyectado ────────────────────────────────────────
    private static final List<Pattern> PATRONES_XSS = List.of(
            Pattern.compile("<\\s*script\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</\\s*script\\s*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript\\s*:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("data\\s*:\\s*text/html", Pattern.CASE_INSENSITIVE),
            Pattern.compile("on(error|load|mouseover|click|focus|blur|change|submit|mouseenter)\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<\\s*iframe\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<\\s*svg\\b[^>]*\\bonload\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<\\s*img\\b[^>]*\\bonerror\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<\\s*object\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<\\s*embed\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<\\s*link\\b[^>]*\\bhref\\s*=\\s*['\"]?javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("document\\s*\\.\\s*cookie", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bexpression\\s*\\(", Pattern.CASE_INSENSITIVE)
    );

    // ── Inyeccion SQL -- combinaciones, nunca una comilla o guion sueltos ──
    private static final List<Pattern> PATRONES_SQLI = List.of(
            Pattern.compile("'\\s*or\\s*'?\\s*[0-9a-zA-Z]+\\s*'?\\s*=\\s*'?\\s*[0-9a-zA-Z]+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bor\\s+1\\s*=\\s*1\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\band\\s+1\\s*=\\s*1\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bunion\\b\\s+(all\\s+)?select\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile(";\\s*drop\\s+table\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile(";\\s*delete\\s+from\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\binsert\\s+into\\b.+\\bvalues\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bupdate\\b\\s+[\\w.\"`]+\\s+set\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bexec(\\s|\\()+\\s*(sp|xp)_", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bxp_cmdshell\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("--\\s*['\"]?\\s*$"),
            Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL),
            Pattern.compile("\\bwaitfor\\s+delay\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsleep\\s*\\(\\s*\\d+\\s*\\)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bpg_sleep\\s*\\(", Pattern.CASE_INSENSITIVE)
    );

    // ── Path traversal (incluye variantes codificadas y doble-codificadas) ──
    private static final List<Pattern> PATRONES_PATH_TRAVERSAL = List.of(
            Pattern.compile("\\.\\./"),
            Pattern.compile("\\.\\.\\\\"),
            Pattern.compile("%2e%2e(%2f|/|%5c|\\\\)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("%252e%252e", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\.\\.%2f", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\.\\.%5c", Pattern.CASE_INSENSITIVE)
    );

    // ── Bytes nulos / caracteres de control -- terminan strings en C, esconden extensiones ──
    private static final List<Pattern> PATRONES_NULL_BYTE = List.of(
            Pattern.compile("%00", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\\\0"),
            Pattern.compile("\u0000")
    );

    /**
     * Revisa un valor contra las 4 categorias. Devuelve la categoria que disparo (para el log),
     * vacio si el valor es limpio.
     */
    static Optional<String> categoriaSospechosa(String valor) {
        if (valor == null || valor.isEmpty()) {
            return Optional.empty();
        }
        if (coincideAlguno(PATRONES_XSS, valor)) {
            return Optional.of("XSS");
        }
        if (coincideAlguno(PATRONES_SQLI, valor)) {
            return Optional.of("SQLi");
        }
        if (coincideAlguno(PATRONES_PATH_TRAVERSAL, valor)) {
            return Optional.of("path-traversal");
        }
        if (coincideAlguno(PATRONES_NULL_BYTE, valor)) {
            return Optional.of("null-byte");
        }
        return Optional.empty();
    }

    private static boolean coincideAlguno(List<Pattern> patrones, String valor) {
        for (Pattern patron : patrones) {
            if (patron.matcher(valor).find()) {
                return true;
            }
        }
        return false;
    }
}
