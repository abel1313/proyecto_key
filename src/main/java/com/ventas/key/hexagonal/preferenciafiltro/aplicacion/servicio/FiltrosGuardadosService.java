package com.ventas.key.hexagonal.preferenciafiltro.aplicacion.servicio;

import com.ventas.key.hexagonal.preferenciafiltro.dominio.excepcion.SoloPersonalException;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo.FiltrosGuardados;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo.Pantalla;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo.QuienGuarda;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.puerto.entrada.FiltrosGuardadosCasoUso;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.puerto.salida.FiltrosGuardadosPort;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.puerto.salida.QuienGuardaPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * [Hexagonal: Application Service] [Clean: Use Case Interactor]
 */
@Service
@RequiredArgsConstructor
public class FiltrosGuardadosService implements FiltrosGuardadosCasoUso {

    private final FiltrosGuardadosPort filtros;
    private final QuienGuardaPort quienGuarda;

    @Override
    @Transactional(readOnly = true)
    public Optional<FiltrosGuardados> obtener(String clavePantalla) {
        int usuarioId = personal().usuarioId();
        return filtros.buscar(usuarioId, Pantalla.deClave(clavePantalla));
    }

    @Override
    @Transactional
    public Optional<FiltrosGuardados> guardar(String clavePantalla, String filtrosJson) {
        int usuarioId = personal().usuarioId();
        Pantalla pantalla = Pantalla.deClave(clavePantalla);
        if (FiltrosGuardados.estanVacios(filtrosJson)) {
            filtros.borrar(usuarioId, pantalla);
            return Optional.empty();
        }
        return Optional.of(filtros.guardar(
                new FiltrosGuardados(usuarioId, pantalla, filtrosJson, LocalDateTime.now())));
    }

    @Override
    @Transactional
    public void borrar(String clavePantalla) {
        int usuarioId = personal().usuarioId();
        filtros.borrar(usuarioId, Pantalla.deClave(clavePantalla));
    }

    /** R1, antes que cualquier otra validación: a un cliente no se le dice ni qué pantallas hay. */
    private QuienGuarda personal() {
        QuienGuarda quien = quienGuarda.actual();
        if (!quien.esPersonal()) {
            throw new SoloPersonalException();
        }
        return quien;
    }
}
