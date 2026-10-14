package com.ventas.key.hexagonal.codigopostal.aplicacion.servicio;

import com.ventas.key.hexagonal.codigopostal.dominio.excepcion.CodigoPostalException;
import com.ventas.key.hexagonal.codigopostal.dominio.modelo.CodigoPostalInfo;
import com.ventas.key.hexagonal.codigopostal.dominio.puerto.entrada.BuscarCodigoPostalCasoUso;
import com.ventas.key.hexagonal.codigopostal.dominio.puerto.salida.CodigoPostalExternoPort;
import com.ventas.key.hexagonal.codigopostal.dominio.puerto.salida.CodigoPostalGuardadoPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * [Hexagonal: Application Service] [Clean: Use Case Interactor]
 *
 * <p>Reglas R1 a R7 (acordadas con el dueño el 2026-10-14): primero nuestra tabla; si no está (o
 * tiene más de un año) se pregunta al servicio externo y se guarda; si el servicio falla, se usa lo
 * guardado y, si no hay nada, la pantalla deja escribir el municipio a mano.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuscarCodigoPostalService implements BuscarCodigoPostalCasoUso {

    private final CodigoPostalGuardadoPort guardados;
    private final CodigoPostalExternoPort externo;

    @Override
    public CodigoPostalInfo buscar(String codigo) {
        if (!CodigoPostalInfo.esValido(codigo)) {
            throw CodigoPostalException.noValido();
        }

        Optional<CodigoPostalInfo> guardado = leerGuardado(codigo);
        if (guardado.isPresent() && !guardado.get().caducado(LocalDateTime.now())) {
            return guardado.get();                                   // R1
        }

        Optional<CodigoPostalInfo> deAfuera;
        try {
            deAfuera = externo.consultar(codigo);
        } catch (CodigoPostalException e) {
            if (guardado.isPresent()) {
                return guardado.get();                               // R7: caducado, pero mejor que nada
            }
            throw e;                                                 // R4
        }

        if (deAfuera.isEmpty()) {
            if (guardado.isPresent()) {
                return guardado.get();
            }
            throw CodigoPostalException.noExiste(codigo);            // R3: no se guarda
        }

        guardarSinEstorbar(deAfuera.get());                           // R2
        return deAfuera.get();
    }

    /** Si nuestra tabla falla (no existe todavía, base caída), se sigue con el servicio externo. */
    private Optional<CodigoPostalInfo> leerGuardado(String codigo) {
        try {
            return guardados.buscar(codigo);
        } catch (RuntimeException e) {
            log.warn("No se pudo leer el código postal {} de nuestra tabla: {}", codigo, e.toString());
            return Optional.empty();
        }
    }

    /** Guardar es un extra: si falla, el cliente igual recibe su municipio. */
    private void guardarSinEstorbar(CodigoPostalInfo info) {
        try {
            guardados.guardar(info);
        } catch (RuntimeException e) {
            log.warn("No se pudo guardar el código postal {}: {}", info.codigo(), e.toString());
        }
    }
}
