package com.ventas.key.mis.productos.models;

import lombok.Getter;
import lombok.Setter;

// Resultado de un giro en una rifa PLATAFORMAS. El boleto es el que salió en la
// ruleta; si no es ganador, ese boleto (y solo ese) queda descartado.
@Getter
@Setter
public class SorteoPlataformasResultadoDto {
    private BoletoRifaDto boleto;
    private boolean esGanador;
    private ConfigurarRifaVarianteDto varianteActual;
    private int giroActual;
    private int giroGanador;
    private boolean rifaTerminada;
}
