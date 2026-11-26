package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Una plantilla aprobada por Meta, con los valores de sus variables ({{1}}, {{2}}…) en orden.
 * Valida lo que Meta exige de entrada para no gastar una llamada que se sabe que va a rechazar.
 */
public record PlantillaWhatsapp(String nombre, String idioma, List<String> parametros) {

    private static final Pattern NOMBRE = Pattern.compile("^[a-z0-9_]{1,100}$");
    private static final Pattern IDIOMA = Pattern.compile("^[a-z]{2,3}(_[A-Z]{2})?$");
    private static final int MAX_PARAMETROS = 10;
    private static final int MAX_LARGO_PARAMETRO = 1024;

    public PlantillaWhatsapp {
        if (nombre == null || !NOMBRE.matcher(nombre).matches()) {
            throw new IllegalArgumentException("El nombre de la plantilla solo lleva minúsculas, números y guion bajo");
        }
        if (idioma == null || idioma.isBlank()) {
            idioma = "es_MX";
        }
        if (!IDIOMA.matcher(idioma).matches()) {
            throw new IllegalArgumentException("El idioma de la plantilla no es válido (ej. es_MX)");
        }
        List<String> limpios = new ArrayList<>();
        if (parametros != null) {
            for (String p : parametros) {
                // Meta rechaza saltos de línea y tabuladores dentro de una variable.
                String valor = p == null ? "" : p.replaceAll("[\\r\\n\\t]+", " ").trim();
                if (valor.isEmpty()) {
                    throw new IllegalArgumentException("Los valores de la plantilla no pueden ir vacíos");
                }
                if (valor.length() > MAX_LARGO_PARAMETRO) {
                    throw new IllegalArgumentException("Un valor de la plantilla es demasiado largo");
                }
                limpios.add(valor);
            }
        }
        if (limpios.size() > MAX_PARAMETROS) {
            throw new IllegalArgumentException("La plantilla lleva demasiados valores");
        }
        parametros = List.copyOf(limpios);
    }
}
