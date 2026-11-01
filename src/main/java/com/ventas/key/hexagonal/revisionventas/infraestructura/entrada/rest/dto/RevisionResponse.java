package com.ventas.key.hexagonal.revisionventas.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.revisionventas.dominio.modelo.Hallazgo;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.HallazgosDeUnTipo;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.ResultadoRevision;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.TipoHallazgo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que el front necesita de la revisión, sin datos internos.
 *
 * @param todoEnOrden true si no hay nada que revisar
 * @param tipos       un renglón por cada tipo que se pudo revisar (con {@code total} 0 si está bien)
 * @param noRevisados códigos de los tipos que no se pudieron revisar en este ambiente
 */
public record RevisionResponse(LocalDate desde, LocalDate hasta, Instant generadoEn, int totalHallazgos,
                               boolean todoEnOrden, List<TipoResponse> tipos, List<String> noRevisados) {

    public record TipoResponse(String codigo, String titulo, String explicacion, String gravedad, int total,
                               List<EjemploResponse> ejemplos) {
    }

    public record EjemploResponse(Integer pedidoId, Integer ventaId, Integer articuloId, String detalle) {
    }

    public static RevisionResponse de(ResultadoRevision r) {
        List<TipoResponse> tipos = r.revisados().stream().map(RevisionResponse::tipo).toList();
        List<String> no = r.noRevisados().stream().map(TipoHallazgo::name).toList();
        return new RevisionResponse(r.desde(), r.hasta(), r.generadoEn(), r.totalHallazgos(), r.todoEnOrden(), tipos, no);
    }

    private static TipoResponse tipo(HallazgosDeUnTipo h) {
        List<EjemploResponse> ejemplos = h.ejemplos().stream().map(RevisionResponse::ejemplo).toList();
        return new TipoResponse(h.tipo().name(), h.tipo().titulo(), h.tipo().explicacion(), h.tipo().gravedad().name(),
                h.total(), ejemplos);
    }

    private static EjemploResponse ejemplo(Hallazgo h) {
        return new EjemploResponse(h.pedidoId(), h.ventaId(), h.articuloId(), h.detalle());
    }
}
