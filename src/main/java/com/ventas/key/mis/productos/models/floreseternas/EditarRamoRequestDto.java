package com.ventas.key.mis.productos.models.floreseternas;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

// Reemplaza la composicion del ramo (colores y accesorios) de un pedido ya existente, y
// opcionalmente su fecha de entrega -- ver RamoPedidoDetalleServiceImpl.editarRamo(). Envio y
// liston siguen fuera de esta version. Solo ADMIN.
@Data
public class EditarRamoRequestDto {
    private List<ColorSeleccionadoDto> colores;
    private List<AccesorioSeleccionadoDto> accesorios;
    // Opcional -- si viene null, la fecha/urgencia del pedido no se toca (comportamiento identico
    // a la primera version). Si viene, reemplaza fechaHoraEntrega/esUrgente y recalcula
    // fechaLimitePago/cargoUrgenteMonto para el nuevo tamano de ramo -- el admin ya debio validar
    // la fecha contra fechas-disponibles antes de mandarla aqui, este endpoint no vuelve a
    // preguntar el calendario completo.
    private LocalDateTime fechaHoraEntrega;
    private Boolean urgente;
}
