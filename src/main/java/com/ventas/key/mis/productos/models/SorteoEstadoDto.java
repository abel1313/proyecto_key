package com.ventas.key.mis.productos.models;

import com.ventas.key.mis.productos.entity.Concursante;
import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.entity.ConfigurarRifaVariante;
import com.ventas.key.mis.productos.entity.HistorialRifaVariante;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SorteoEstadoDto {

    private ConfigurarRifa configurarRifa;
    private int totalConcursantes;
    private int totalVariantes;
    private int varianteNumeroActual;

    private ConfigurarRifaVariante varianteActual;
    private int giroActual;
    private int giroGanador;

    private List<Concursante> elegibles;
    private List<Concursante> descartados;
    private List<HistorialRifaVariante> historial;

    private boolean rifaTerminada;
}