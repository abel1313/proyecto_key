package com.ventas.key.hexagonal.calculoprecio.dominio.puerto.salida;

import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.ComisionTerminal;

import java.util.List;

/** [Hexagonal: Driven Port] Las comisiones de terminal que ya están dadas de alta para el cobro con tarjeta. */
public interface ComisionesTerminalPort {
    List<ComisionTerminal> listar();
}
