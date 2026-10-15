package com.ventas.key.hexagonal.precio.aplicacion.servicio;

import com.ventas.key.hexagonal.precio.dominio.excepcion.PrecioInvalidoException;
import com.ventas.key.hexagonal.precio.dominio.modelo.PreciosDeArticulo;
import com.ventas.key.hexagonal.precio.dominio.puerto.entrada.ConsultarDescuentoArticuloCasoUso;
import com.ventas.key.hexagonal.precio.dominio.puerto.salida.PreciosArticuloPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** [Hexagonal: implementacion del puerto de entrada] [Clean: Use Case Interactor] */
@Service
@RequiredArgsConstructor
public class ConsultarDescuentoArticuloService implements ConsultarDescuentoArticuloCasoUso {

    private final PreciosArticuloPort precios;

    @Override
    @Transactional(readOnly = true)
    public PreciosDeArticulo consultar(Integer varianteId) {
        return precios.buscar(varianteId)
                .orElseThrow(() -> PrecioInvalidoException.articuloNoExiste(varianteId));
    }
}
