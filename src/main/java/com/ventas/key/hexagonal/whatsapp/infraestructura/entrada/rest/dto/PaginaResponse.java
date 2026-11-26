package com.ventas.key.hexagonal.whatsapp.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.Pagina;

import java.util.List;
import java.util.function.Function;

/** Una página de resultados. {@code pagina} empieza en 0. */
public record PaginaResponse<T>(List<T> elementos, int pagina, int size, long total, int totalPaginas) {

    public static <D, T> PaginaResponse<T> de(Pagina<D> p, Function<D, T> mapa) {
        return new PaginaResponse<>(p.elementos().stream().map(mapa).toList(), p.pagina(), p.size(), p.total(),
                p.totalPaginas());
    }
}
