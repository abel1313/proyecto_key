package com.ventas.key.mis.productos.models;

import com.ventas.key.mis.productos.entity.Concursante;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImportarDePedidosResponseDto {
    private List<Concursante> importados;
    private List<ImportarDePedidosRequest.ClientePedidoDto> omitidosYaRegistrados;
    private List<ImportarDePedidosRequest.ClientePedidoDto> omitidosSinNombre;
}
