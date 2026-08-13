package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RamoPedidoDetalleRequestDto {
    private Integer tipoFlorId;
    private Integer ramoArmadoId;
    private Integer cantidadFinal;
    private Integer fraseListonPredefinidaId;
    private String fraseListonPersonalizada;
    // Monto de anticipo ya calculado por /v1/flores/calcular-precio (montoAnticipoSugerido) --
    // se guarda tal cual, no se recalcula aqui.
    private Double montoAnticipo;
    private Integer lugarEntregaId;
    private Boolean recogerEnLocal;
    private String telefonoContacto;
    private String correoContacto;
    private String comentarioAccesorioNoDisponible;
}
