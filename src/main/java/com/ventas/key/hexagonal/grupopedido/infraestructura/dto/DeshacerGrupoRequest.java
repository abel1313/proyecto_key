package com.ventas.key.hexagonal.grupopedido.infraestructura.dto;

import lombok.Data;

/** {@code POST /v1/grupos-pedido/{grupoId}/deshacer} */
@Data
public class DeshacerGrupoRequest {
    private String motivo;
}
