package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RamoPedidoDetalleResponseDto {
    private Integer id;
    private Integer pedidoId;
    private Integer ramoArmadoId;
    private Integer tipoFlorId;
    private String tipoFlorNombre;
    private Integer cantidadFinal;
    private List<RamoPedidoDetalleColorDto> colores;
    private String fraseListonTexto;
    private String fraseListonEstado;
    private Double fraseListonPrecioAsignado;
    private Boolean anticipoRequerido;
    private Boolean anticipoPagado;
    private Double montoAnticipo;
    private Integer lugarEntregaId;
    private String lugarEntregaNombre;
    private Boolean recogerEnLocal;
    private String telefonoContacto;
    private String correoContacto;
    private String comentarioAccesorioNoDisponible;
    private LocalDateTime fechaCreacion;
    // Pedido APARTADO separado para cobrar la frase ya validada -- el front registra el pago
    // real con POST /v1/abonos/{pedidoAnticipoId} (modulo de abonos existente). Null hasta que
    // el admin apruebe la frase con precio.
    private Integer pedidoAnticipoId;
    // Accesorios elegidos (sin el papel, ver AccesorioSeleccionadoRamoDto) -- para reconstruir
    // el configurador al editar el ramo. Lista vacia si no eligio ninguno.
    private List<AccesorioSeleccionadoRamoDto> accesorios;
    private LocalDateTime fechaHoraEntrega;
    private Boolean esUrgente;
    // Momento limite para pagar con el precio normal antes de que se aplique cargoUrgenteMonto --
    // null si esUrgente=false o si este tamano no tiene cargo_urgente configurado.
    private LocalDateTime fechaLimitePago;
    private Double cargoUrgenteMonto;
}
