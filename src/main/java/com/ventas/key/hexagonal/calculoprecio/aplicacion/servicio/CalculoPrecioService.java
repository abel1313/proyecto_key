package com.ventas.key.hexagonal.calculoprecio.aplicacion.servicio;

import com.ventas.key.hexagonal.calculoprecio.dominio.excepcion.ProductoSinCalculoException;
import com.ventas.key.hexagonal.calculoprecio.dominio.modelo.CalculoPrecio;
import com.ventas.key.hexagonal.calculoprecio.dominio.puerto.entrada.CalculoPrecioCasoUso;
import com.ventas.key.hexagonal.calculoprecio.dominio.puerto.salida.CalculosPrecioPort;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** [Clean: Use Case] Orquesta: valida que el producto exista, sella el cálculo y lo guarda. Java puro (se arma en CalculoPrecioBeans). */
public class CalculoPrecioService implements CalculoPrecioCasoUso {

    private static final int MAXIMO_HISTORIAL = 50;

    private final CalculosPrecioPort calculos;
    private final Clock reloj;

    public CalculoPrecioService(CalculosPrecioPort calculos, Clock reloj) {
        this.calculos = calculos;
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
}
