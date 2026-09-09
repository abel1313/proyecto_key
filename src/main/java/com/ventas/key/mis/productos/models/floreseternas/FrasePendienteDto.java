package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// Fila de la bandeja "frases pendientes de validar" -- lo minimo para que el admin decida y
// mande a validar-frase sin tener que abrir el pedido completo.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FrasePendienteDto {
    private Integer detalleId;
    private Integer pedidoId;
    private String fraseTexto;
    private String clienteNombre;
    private LocalDate fechaPedido;
}
