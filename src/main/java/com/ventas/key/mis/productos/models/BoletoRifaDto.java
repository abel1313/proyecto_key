package com.ventas.key.mis.productos.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

// Un boleto tal como lo consume el front: ya trae el nombre del concursante
// resuelto para no tener que cruzarlo contra otra lista.
@Getter
@Setter
public class BoletoRifaDto {
    private Integer id;
    private Integer concursanteId;
    private String nombreCompleto;
    private String plataforma;
    private String motivo;
    private LocalDate fecha;
    private String urlPerfilRedSocial;
    private String urlSeguimiento;
    private List<String> urlsCompartido;
    private boolean descartado;
}
