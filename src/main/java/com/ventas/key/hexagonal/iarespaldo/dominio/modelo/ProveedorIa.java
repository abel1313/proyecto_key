package com.ventas.key.hexagonal.iarespaldo.dominio.modelo;

/**
 * Un servicio de IA al que se le puede pedir una respuesta (OpenAI, DeepSeek...). Los dos hablan el
 * mismo formato ({@code /chat/completions}), así que solo cambian la dirección, la llave y el modelo.
 * La llave nunca se imprime: {@link #toString()} no la incluye.
 */
public record ProveedorIa(String nombre, String urlBase, String apiKey, String modelo) {

    /** Sin llave no se intenta: así un respaldo que no se configuró simplemente no existe. */
    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank() && urlBase != null && !urlBase.isBlank();
    }

    @Override
    public String toString() {
        return "ProveedorIa[" + nombre + ", modelo=" + modelo + "]";
    }
}
