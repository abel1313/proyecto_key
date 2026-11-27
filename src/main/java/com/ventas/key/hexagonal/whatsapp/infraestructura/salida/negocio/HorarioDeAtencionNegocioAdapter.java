package com.ventas.key.hexagonal.whatsapp.infraestructura.salida.negocio;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.HorarioDeAtencion;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.HorarioDeAtencionPort;
import com.ventas.key.mis.productos.dto.negocio.NegocioEstadoDto;
import com.ventas.key.mis.productos.service.NegocioService;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * El horario de atención ES el del negocio (Sistema → Negocio y contactos), decisión del dueño 2026-11-27:
 * se cambia en un solo lugar. Solo se LEE; no se toca nada del negocio.
 *
 * <p>[Hexagonal: Driven Adapter] [Clean: Gateway]</p>
 */
@Component
public class HorarioDeAtencionNegocioAdapter implements HorarioDeAtencionPort {

    /** Se lee una vez por minuto: leerlo en cada mensaje podía crear la configuración del negocio varias veces a la vez. */
    private static final long VIGENCIA_NANOS = 60_000_000_000L;

    private final NegocioService negocio;
    private volatile Optional<HorarioDeAtencion> guardado;
    private volatile long leidoEn;

    public HorarioDeAtencionNegocioAdapter(NegocioService negocio) {
        this.negocio = negocio;
    }

    @Override
    public Optional<HorarioDeAtencion> actual() {
        Optional<HorarioDeAtencion> vigente = guardado;
        if (vigente != null && System.nanoTime() - leidoEn < VIGENCIA_NANOS) {
            return vigente;
        }
        Optional<HorarioDeAtencion> nuevo = leer();
        guardado = nuevo;
        leidoEn = System.nanoTime();
        return nuevo;
    }

    private Optional<HorarioDeAtencion> leer() {
        NegocioEstadoDto estado = negocio.getEstado();
        if (estado == null) {
            return Optional.empty();
        }
        try {
            return HorarioDeAtencion.de(hora(estado.getHoraApertura()), hora(estado.getHoraCierre()),
                    hora(estado.getHoraAperturaFinSemana()), hora(estado.getHoraCierreFinSemana()));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static LocalTime hora(String texto) {
        return texto == null || texto.isBlank() ? null : LocalTime.parse(texto.trim());
    }
}
