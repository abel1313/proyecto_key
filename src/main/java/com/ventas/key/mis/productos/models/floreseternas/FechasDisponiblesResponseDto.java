package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FechasDisponiblesResponseDto {
    // Null cuando no hay ningun tamano configurado igual o mayor a "cantidad" (bloqueo real, ver
    // mensaje) o cuando pidieron urgente:true y este tamano no ofrece esa opcion.
    private LocalDateTime primeraFechaValida;
    // Por ahora siempre 1 elemento (la hora de primeraFechaValida) -- lista para que el front no
    // tenga que cambiar de tipo si mas adelante se permiten varias horas de entrega por dia.
    private List<String> horasDisponibles;
    // Tamano de CantidadFlorValida que realmente se uso -- puede ser mayor al "cantidad" pedido
    // (redondeo hacia arriba). El front debe avisarle al cliente cuando difiera.
    private Integer cantidadAplicada;
    private Double cargoUrgencia;
    // Si este tamano (el aplicado) tiene plazo urgente configurado -- el front solo debe mostrar
    // el boton de "urgente" cuando esto es true, sin importar que se haya pedido urgente:true o
    // false en el request.
    private Boolean ofreceUrgente;
    // Presente solo en los casos de bloqueo/rechazo (sin tamano configurado que alcance, o
    // urgente pedido para un tamano que no lo ofrece). primeraFechaValida viene null en esos casos.
    private String mensaje;
}
