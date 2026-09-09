package com.ventas.key.mis.productos.models.floreseternas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Cada entrada representa UNA unidad del accesorio elegido. Si el cliente quiere 2 coronas,
// vienen 2 entradas con el mismo accesorioId -- asi cada unidad se cobra por separado.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccesorioSeleccionadoDto {
    private Integer accesorioId;
}
