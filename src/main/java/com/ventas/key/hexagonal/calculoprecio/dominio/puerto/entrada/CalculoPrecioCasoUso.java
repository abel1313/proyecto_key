package com.ventas.key.hexagonal.calculoprecio.dominio.puerto.entrada;

import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.CalculoPrecio;
import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.ComisionTerminal;
import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.FiltroCalculos;
import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.PaginaCalculos;

import java.util.List;
import java.util.Optional;

/** [Hexagonal: Driving Port] Guardar y consultar los cálculos de precio de un producto. */
public interface CalculoPrecioCasoUso {
    /** Guarda el cálculo (nuevo registro del historial) sellado con quién lo hizo y cuándo. */
    CalculoPrecio guardar(CalculoPrecio calculo, String usuario);

    Optional<CalculoPrecio> ultimo(int productoId);

    List<CalculoPrecio> historial(int productoId, int maximo);

    /** Comisiones de terminal ya dadas de alta (para elegir una en la calculadora). */
    List<ComisionTerminal> comisiones();

    /** Lo calculado en todos los productos: el último cálculo de cada uno, con filtros y páginas. */
    PaginaCalculos listarUltimos(FiltroCalculos filtro, int pagina, int tamano);
}
