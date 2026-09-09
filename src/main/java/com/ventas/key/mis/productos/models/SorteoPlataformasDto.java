package com.ventas.key.mis.productos.models;

import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.entity.GanadorRifa;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Estado completo de una rifa PLATAFORMAS -- con esto el front puede recargar la
// pantalla y reconstruir todo (configuración, premios, giro actual, boletos en
// juego y descartados, ganadores ya declarados).
@Getter
@Setter
public class SorteoPlataformasDto {
    private ConfigurarRifa configurarRifa;
    private List<ConfigurarRifaVarianteDto> variantes;
    private ConfigurarRifaVarianteDto varianteActual;
    private int varianteNumeroActual;
    private int totalVariantes;
    private int giroActual;
    private int giroGanador;
    private List<BoletoRifaDto> boletosEnJuego;
    private List<BoletoRifaDto> boletosDescartados;
    private List<GanadorRifa> ganadores;
    private boolean rifaTerminada;
}
