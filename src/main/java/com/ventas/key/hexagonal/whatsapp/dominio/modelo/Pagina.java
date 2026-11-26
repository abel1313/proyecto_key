package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import java.util.List;
import java.util.function.Function;

/** Una página de resultados, sin depender de Spring Data. */
public record Pagina<T>(List<T> elementos, int pagina, int size, long total) {

    public int totalPaginas() {
        return size <= 0 ? 0 : (int) ((total + size - 1) / size);
    }

    public <R> Pagina<R> mapear(Function<T, R> f) {
        return new Pagina<>(elementos.stream().map(f).toList(), pagina, size, total);
    }
}
