package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RamoPedidoDetalleRequestDto {
    private Integer ramoArmadoId;
    // Mismo desglose que se mando a /v1/flores/calcular-precio -- para produccion, no para precio.
    private List<ColorSeleccionadoDto> colores;
    private Integer fraseListonPredefinidaId;
    private String fraseListonPersonalizada;
    private Integer lugarEntregaId;
    private Boolean recogerEnLocal;
    private String telefonoContacto;
    private String correoContacto;
    private String comentarioAccesorioNoDisponible;
}
