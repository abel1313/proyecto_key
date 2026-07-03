package com.ventas.key.mis.productos.models;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class HabilitarLoteRequest {

    @NotEmpty(message = "Debe enviar al menos un id")
    private List<Integer> ids;

    private boolean habilitar;
}
