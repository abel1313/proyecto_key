package com.ventas.key.mis.productos.models;

import com.ventas.key.mis.productos.entity.Concursante;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SorteoResultadoDto {
    private boolean descartado;
    private Concursante concursante;
    private ConfigurarRifaVarianteDto varianteActual;
    private boolean rifaTerminada;
}