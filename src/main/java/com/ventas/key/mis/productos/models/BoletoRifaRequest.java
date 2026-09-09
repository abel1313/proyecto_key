package com.ventas.key.mis.productos.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class BoletoRifaRequest {
    private Integer concursanteId;
    private String motivo;
    private LocalDate fecha;
    private String urlPerfilRedSocial;
    private String urlSeguimiento;
    private List<String> urlsCompartido;
}
