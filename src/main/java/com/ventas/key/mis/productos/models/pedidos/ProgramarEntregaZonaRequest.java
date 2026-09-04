package com.ventas.key.mis.productos.models.pedidos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class ProgramarEntregaZonaRequest {
    private LocalDate fecha;
    private String hora;
    private String puntoEncuentro;
}
