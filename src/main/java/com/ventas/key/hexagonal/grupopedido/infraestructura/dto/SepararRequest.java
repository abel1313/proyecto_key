package com.ventas.key.hexagonal.grupopedido.infraestructura.dto;

import lombok.Data;

import java.util.List;

/** {@code POST /v1/grupos-pedido/{grupoId}/separar} */
@Data
public class SepararRequest {
    /** Los que se separan. Todos = separar el grupo entero. */
    private List<Integer> pedidosQueSalen;
    /** Cuanto de lo abonado se queda cada pedido que sale. Solo en grupos a credito. */
    private List<Parte> reparto;
    /** Quien recoge a los que siguen unidos. Obligatorio si el que recogia se separa. */
    private Integer nuevoTitularId;
    private String motivo;

    @Data
    public static class Parte {
        private Integer pedidoId;
        private Double monto;
    }
}
