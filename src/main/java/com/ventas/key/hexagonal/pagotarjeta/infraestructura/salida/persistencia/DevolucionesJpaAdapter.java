package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.Devolucion;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.DevolucionesPort;
import com.ventas.key.mis.productos.entity.PagoTarjetaDevolucion;
import com.ventas.key.mis.productos.repository.IPagoTarjetaDevolucionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * [Hexagonal: Driven Adapter] [Clean: Interface Adapter]
 *
 * <p>Traduce entre {@link Devolucion} y la fila de {@code pago_tarjeta_devolucion}. La clave unica de la
 * base es lo que impide que dos solicitudes reserven la misma devolucion.
 */
@Component
public class DevolucionesJpaAdapter implements DevolucionesPort {

    static final String PENDIENTE = "PENDIENTE";
    static final String CONFIRMADA = "CONFIRMADA";

    private final IPagoTarjetaDevolucionRepository repositorio;

    public DevolucionesJpaAdapter(IPagoTarjetaDevolucionRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Devolucion reservar(Devolucion d) {
        PagoTarjetaDevolucion fila = new PagoTarjetaDevolucion();
        fila.setIntentoId(IntentosDePagoJpaAdapter.aId(d.intentoId()));
        fila.setMonto(d.monto());
        fila.setHechaPor(d.hechaPor());
        fila.setClave(d.clave());
        fila.setEstado(PENDIENTE);
        fila.setCreadoEn(IntentosDePagoJpaAdapter.aLocal(d.creadoEn()));
        try {
            return aDominio(repositorio.saveAndFlush(fila));
        } catch (DataIntegrityViolationException e) {
            if (IntentosDePagoJpaAdapter.violaRestriccion(e, "uq_ptd_clave")) {
                throw new PagoConTarjetaException.ConflictoDeConcurrencia();     // otra solicitud ya la tomo
            }
            throw e;
        }
    }

    @Override
    public Devolucion confirmar(Devolucion d) {
        PagoTarjetaDevolucion fila = repositorio.findById(IntentosDePagoJpaAdapter.aId(d.id()))
                .orElseThrow(() -> PagoConTarjetaException.datosInvalidos("no existe la devolución " + d.id()));
        if (!CONFIRMADA.equals(fila.getEstado())) {
            fila.setEstado(CONFIRMADA);
            fila.setConfirmadaEn(LocalDateTime.now(ZoneOffset.UTC));
            repositorio.saveAndFlush(fila);
        }
        return aDominio(fila);
    }

    @Override
    public List<Devolucion> pendientesDelIntento(Long intentoId) {
        return repositorio.findByIntentoIdAndEstadoOrderByIdAsc(IntentosDePagoJpaAdapter.aId(intentoId), PENDIENTE)
                .stream().map(DevolucionesJpaAdapter::aDominio).toList();
    }

    static Devolucion aDominio(PagoTarjetaDevolucion f) {
        return new Devolucion(f.getId().longValue(), f.getIntentoId().longValue(), f.getMonto(), f.getHechaPor(),
                f.getClave(), CONFIRMADA.equals(f.getEstado()), IntentosDePagoJpaAdapter.aInstante(f.getCreadoEn()));
    }
}
