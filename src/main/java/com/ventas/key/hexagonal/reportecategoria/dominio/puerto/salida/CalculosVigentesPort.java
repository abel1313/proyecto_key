package com.ventas.key.hexagonal.reportecategoria.dominio.puerto.salida;

import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.CalculoVigente;
import com.ventas.key.hexagonal.reportecategoria.dominio.modelo.NuevoCalculo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** [Hexagonal: Driven Port] Los cálculos de precio guardados (el último de cada producto) y dónde guardar los que el reporte genera. */
public interface CalculosVigentesPort {
    Map<Integer, CalculoVigente> ultimos(Collection<Integer> productoIds);

    void guardarGenerados(List<NuevoCalculo> nuevos, String usuario);
}
