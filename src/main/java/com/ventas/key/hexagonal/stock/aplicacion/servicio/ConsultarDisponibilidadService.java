package com.ventas.key.hexagonal.stock.aplicacion.servicio;

import com.ventas.key.hexagonal.stock.dominio.excepcion.ProductoSinStockConocidoException;
import com.ventas.key.hexagonal.stock.dominio.modelo.DisponibilidadStock;
import com.ventas.key.hexagonal.stock.dominio.puerto.entrada.ConsultarDisponibilidadCasoUso;
import com.ventas.key.hexagonal.stock.dominio.puerto.salida.ConsultarStockPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * [Hexagonal: implementacion del puerto de entrada] [Clean: Use Case Interactor]
 *
 * <p>Delgado a proposito: el calculo de "cuanto queda libre" vive en
 * {@link DisponibilidadStock#disponible()}, que es donde pertenece. Aca solo esta el paso de
 * buscar y el que hacer si no aparece.
 */
@Service
@RequiredArgsConstructor
public class ConsultarDisponibilidadService implements ConsultarDisponibilidadCasoUso {

    private final ConsultarStockPort consultarStock;

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadStock de(Integer productoId) {
        return consultarStock.disponibilidadDe(productoId)
                .orElseThrow(() -> new ProductoSinStockConocidoException(productoId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisponibilidadStock> descuadrados() {
        return consultarStock.productosDescuadrados();
    }
}
