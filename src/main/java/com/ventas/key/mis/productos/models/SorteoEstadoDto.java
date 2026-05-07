package com.ventas.key.mis.productos.models;

import com.ventas.key.mis.productos.entity.Concursante;
import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.entity.ConfigurarRifaProducto;
import com.ventas.key.mis.productos.entity.GanadorRifa;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SorteoEstadoDto {

    private ConfigurarRifa configurarRifa;
    private int totalConcursantes;

    private ConfigurarRifaProducto productoActual;
    private int giroActual;
    private int giroGanador;

    private List<Concursante> elegibles;
    private List<Concursante> descartados;
    private List<GanadorRifa> historial;

    private boolean rifaTerminada;
}