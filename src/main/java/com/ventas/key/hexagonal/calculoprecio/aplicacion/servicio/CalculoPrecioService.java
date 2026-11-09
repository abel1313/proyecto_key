package com.ventas.key.hexagonal.calculoprecio.aplicacion.servicio;

import com.ventas.key.hexagonal.calculoprecio.dominio.excepcion.ProductoSinCalculoException;
import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.CalculoPrecio;
import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.ComisionTerminal;
import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.FiltroCalculos;
import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.PaginaCalculos;
import com.ventas.key.hexagonal.calculoprecio.dominio.puerto.salida.ComisionesTerminalPort;
import com.ventas.key.hexagonal.calculoprecio.dominio.puerto.entrada.CalculoPrecioCasoUso;
import com.ventas.key.hexagonal.calculoprecio.dominio.puerto.salida.CalculosPrecioPort;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** [Clean: Use Case] Orquesta: valida que el producto exista, sella el cálculo y lo guarda. Java puro (se arma en CalculoPrecioBeans). */
public class CalculoPrecioService implements CalculoPrecioCasoUso {

    private static final int MAXIMO_HISTORIAL = 50;
    private static final int MAXIMO_POR_PAGINA = 100;

    private final CalculosPrecioPort calculos;
    private final ComisionesTerminalPort comisiones;
    private final Clock reloj;

    public CalculoPrecioService(CalculosPrecioPort calculos, ComisionesTerminalPort comisiones, Clock reloj) {
        this.calculos = calculos;
        this.comisiones = comisiones;
        this.reloj = reloj;
    }

    @Override
    public CalculoPrecio guardar(CalculoPrecio calculo, String usuario) {
        if (!calculos.existeProducto(calculo.productoId())) throw new ProductoSinCalculoException(calculo.productoId());
        return calculos.guardar(calculo.registradoPor(usuario, LocalDateTime.now(reloj)));
    }

    @Override
    public Optional<CalculoPrecio> ultimo(int productoId) {
        return calculos.ultimo(productoId);
    }

    @Override
    public List<CalculoPrecio> historial(int productoId, int maximo) {
        return calculos.historial(productoId, Math.max(1, Math.min(maximo, MAXIMO_HISTORIAL)));
    }

    @Override
    public List<ComisionTerminal> comisiones() {
        return comisiones.listar();
    }

    @Override
    public PaginaCalculos listarUltimos(FiltroCalculos filtro, int pagina, int tamano) {
        return calculos.ultimosPorProducto(filtro == null ? new FiltroCalculos(null, null, null) : filtro, Math.max(1, pagina), Math.max(1, Math.min(tamano, MAXIMO_POR_PAGINA)));
    }
}
