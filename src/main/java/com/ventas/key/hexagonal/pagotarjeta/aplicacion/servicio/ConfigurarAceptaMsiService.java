package com.ventas.key.hexagonal.pagotarjeta.aplicacion.servicio;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.ConfigMsiArticulo;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.ConfigurarAceptaMsiCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.AceptaMsiPort;

import java.util.Collection;
import java.util.Map;

/**
 * [Hexagonal: implementacion del puerto de entrada] [Clean: Use Case Interactor]
 *
 * <p>Orquesta: pregunta si existe el producto o artículo y lo guarda. La regla de qué cuenta al cobrar
 * (todos los artículos del pedido deben aceptar) vive en {@code ReglasMsi}, no aquí.
 */
public class ConfigurarAceptaMsiService implements ConfigurarAceptaMsiCasoUso {

    private final AceptaMsiPort casillas;

    public ConfigurarAceptaMsiService(AceptaMsiPort casillas) {
        this.casillas = casillas;
    }

    @Override
    public boolean aceptaProducto(Integer productoId) {
        return casillas.productoAcepta(exigirId(productoId, "producto"))
                .orElseThrow(() -> PagoConTarjetaException.productoNoExiste(productoId));
    }

    @Override
    public void cambiarProducto(Integer productoId, boolean acepta) {
        if (!casillas.fijarProducto(exigirId(productoId, "producto"), acepta)) {
            throw PagoConTarjetaException.productoNoExiste(productoId);
        }
    }

    @Override
    public ConfigMsiArticulo articulo(Integer varianteId) {
        return casillas.articulo(exigirId(varianteId, "artículo"))
                .orElseThrow(() -> PagoConTarjetaException.articuloNoExiste(varianteId));
    }

    @Override
    public void cambiarArticulo(Integer varianteId, Boolean acepta) {
        if (!casillas.fijarArticulo(exigirId(varianteId, "artículo"), acepta)) {
            throw PagoConTarjetaException.articuloNoExiste(varianteId);
        }
    }

    @Override
    public Map<Integer, Boolean> efectivos(Collection<Integer> varianteIds) {
        if (varianteIds == null || varianteIds.isEmpty()) {
            return Map.of();
        }
        if (varianteIds.size() > 500) {
            throw PagoConTarjetaException.datosInvalidos("se pueden consultar hasta 500 artículos a la vez");
        }
        return casillas.efectivos(varianteIds);
    }

    private static Integer exigirId(Integer id, String que) {
        if (id == null || id < 1) {
            throw PagoConTarjetaException.datosInvalidos("falta el id del " + que);
        }
        return id;
    }
}
