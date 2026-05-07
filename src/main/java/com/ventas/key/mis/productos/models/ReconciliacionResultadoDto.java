package com.ventas.key.mis.productos.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ReconciliacionResultadoDto {
    private LocalDateTime ejecutadoEn = LocalDateTime.now();
    private boolean enProceso = true;
    private int productosRevisados;
    private int variantesRevisadas;
    private List<String> reparados = new ArrayList<>();
    private List<String> faltantesEnDisco = new ArrayList<>();
    private int archivosEliminadosDisco;
    private long bytesLiberados;
    private int relacionesProductoEliminadas;
    private int relacionesVarianteEliminadas;
    private int imagenesEliminadas;
}