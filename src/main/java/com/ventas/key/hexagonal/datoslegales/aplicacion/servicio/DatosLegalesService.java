package com.ventas.key.hexagonal.datoslegales.aplicacion.servicio;

import com.ventas.key.hexagonal.datoslegales.dominio.modelo.DatosLegales;
import com.ventas.key.hexagonal.datoslegales.dominio.puerto.entrada.DatosLegalesCasoUso;
import com.ventas.key.hexagonal.datoslegales.dominio.puerto.salida.DatosLegalesPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** [Hexagonal: Application Service] [Clean: Use Case Interactor] */
@Service
@RequiredArgsConstructor
public class DatosLegalesService implements DatosLegalesCasoUso {

    private final DatosLegalesPort datos;

    @Override
    @Transactional(readOnly = true)
    public DatosLegales consultar() {
        return datos.leer().orElseGet(DatosLegales::vacios);
    }

    @Override
    @Transactional
    public DatosLegales guardar(DatosLegales nuevos) {
        datos.guardar(nuevos);
        return nuevos;
    }
}
