package com.ventas.key.hexagonal.codigopostal.infraestructura.salida.cliente;

import com.ventas.key.hexagonal.codigopostal.dominio.excepcion.CodigoPostalException;
import com.ventas.key.hexagonal.codigopostal.dominio.modelo.CodigoPostalInfo;
import com.ventas.key.hexagonal.codigopostal.dominio.puerto.salida.CodigoPostalExternoPort;
import com.ventas.key.mis.productos.dipomex.CodigoPostal;
import com.ventas.key.mis.productos.dipomex.CodigoPostalResponse;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.service.api.IDopoMexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers — servicio externo]
 *
 * <p>Usa {@link IDopoMexService}, que ya trae el tiempo límite de 8 segundos y la llave. Un
 * "no existe" del servicio se vuelve vacío (R3); cualquier otra falla, "servicio no disponible" (R4).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodigoPostalExternoAdapter implements CodigoPostalExternoPort {

    private final IDopoMexService servicio;

    @Override
    public Optional<CodigoPostalInfo> consultar(String codigo) {
        CodigoPostalResponse respuesta;
        try {
            respuesta = servicio.getCodigoPostal(codigo).block();
        } catch (ExceptionDataNotFound e) {
            return Optional.empty();
        } catch (RuntimeException e) {
            throw CodigoPostalException.servicioNoDisponible();
        }
        CodigoPostal cp = respuesta == null ? null : respuesta.getCodigo_postal();
        if (cp == null || cp.getMunicipio() == null || cp.getMunicipio().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new CodigoPostalInfo(codigo, cp.getMunicipio(), cp.getEstado(),
                cp.getColonias(), LocalDateTime.now()));
    }
}
