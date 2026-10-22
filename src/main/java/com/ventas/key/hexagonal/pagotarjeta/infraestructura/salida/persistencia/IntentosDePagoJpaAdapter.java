package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CanalDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoIntento;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.IntentosDePagoPort;
import com.ventas.key.mis.productos.entity.PagoTarjetaIntento;
import com.ventas.key.mis.productos.repository.IPagoTarjetaIntentoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * [Hexagonal: Driven Adapter] [Clean: Interface Adapter]
 *
 * <p>Traduce entre {@link IntentoDePago} (dominio) y la fila de {@code pago_tarjeta_intento}.
 * Solo persistencia: no decide nada, pero hace cumplir los dos candados de la base:
 * <ul>
 *   <li><b>version</b> (candado optimista): si el intento ya cambio desde que se leyo, lanza
 *       {@link PagoConTarjetaException.ConflictoDeConcurrencia};</li>
 *   <li><b>un activo por pedido</b> (indice unico {@code uq_pti_activo}): si se inserta un segundo cobro
 *       activo para el mismo pedido, lanza {@link PagoConTarjetaException#intentoActivo(Integer)}.</li>
 * </ul>
 * Las fechas viajan en UTC.
 */
@Component
public class IntentosDePagoJpaAdapter implements IntentosDePagoPort {

    private static final List<String> ACTIVOS =
            List.of(EstadoIntento.CREADO.name(), EstadoIntento.ENVIADO.name());
    private static final List<String> PERDIDOS = List.of(EstadoIntento.RECHAZADO.name(),
            EstadoIntento.CANCELADO.name(), EstadoIntento.EXPIRADO.name());

    private final IPagoTarjetaIntentoRepository repositorio;

    public IntentosDePagoJpaAdapter(IPagoTarjetaIntentoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public IntentoDePago guardar(IntentoDePago intento) {
        PagoTarjetaIntento fila;
        if (intento.id() == null) {
            fila = new PagoTarjetaIntento();
            fila.setCreadoEn(aLocal(intento.creadoEn()));
            fila.setClienteId(intento.clienteId());
        } else {
            fila = repositorio.findById(aId(intento.id()))
                    .orElseThrow(() -> PagoConTarjetaException.intentoNoExiste(intento.id()));
            if (fila.getVersion() != intento.version()) {              // alguien lo cambio despues de leerlo
                throw new PagoConTarjetaException.ConflictoDeConcurrencia();
            }
        }
        fila.setPedidoId(intento.pedidoId());
        fila.setCanal(intento.canal().name());
        fila.setMonto(intento.monto());
        fila.setCuotas(intento.cuotas());
        fila.setEstado(intento.estado().name());
        fila.setIdExterno(intento.idExterno());
        fila.setIniciadoPor(intento.iniciadoPor());
        fila.setDevuelto(intento.devuelto());
        fila.setActualizadoEn(aLocal(intento.actualizadoEn()));
        try {
            return aDominio(repositorio.saveAndFlush(fila));
        } catch (OptimisticLockingFailureException e) {
            throw new PagoConTarjetaException.ConflictoDeConcurrencia();
        } catch (DataIntegrityViolationException e) {
            if (intento.id() == null && intento.estado().estaActivo() && violaRestriccion(e, "uq_pti_activo")) {
                throw PagoConTarjetaException.intentoActivo(intento.pedidoId());   // T1 en la base
            }
            throw e;
        }
    }

    @Override
    public Optional<IntentoDePago> buscar(Long intentoId) {
        return repositorio.findById(aId(intentoId)).map(IntentosDePagoJpaAdapter::aDominio);
    }

    @Override
    public Optional<IntentoDePago> activoDelPedido(Integer pedidoId) {
        return repositorio.findFirstByPedidoIdAndEstadoInOrderByCreadoEnDescIdDesc(pedidoId, ACTIVOS)
                .map(IntentosDePagoJpaAdapter::aDominio);
    }

    @Override
    public long rechazadosDesde(Integer pedidoId, Instant desde) {
        return repositorio.countByPedidoIdAndEstadoAndActualizadoEnGreaterThanEqual(
                pedidoId, EstadoIntento.RECHAZADO.name(), aLocal(desde));
    }

    @Override
    public List<IntentoDePago> perdidosDesde(Instant desde) {
        return repositorio.findByEstadoInAndActualizadoEnGreaterThanEqualOrderByActualizadoEnAsc(PERDIDOS, aLocal(desde))
                .stream().map(IntentosDePagoJpaAdapter::aDominio).toList();
    }

    @Override
    public List<IntentoDePago> activosCreadosAntesDe(Instant limite) {
        return repositorio.findByEstadoInAndCreadoEnBeforeOrderByCreadoEnAsc(ACTIVOS, aLocal(limite))
                .stream().map(IntentosDePagoJpaAdapter::aDominio).toList();
    }

    static IntentoDePago aDominio(PagoTarjetaIntento f) {
        return IntentoDePago.reconstruir(
                f.getId().longValue(), f.getPedidoId(), f.getClienteId(), CanalDePago.valueOf(f.getCanal()),
                f.getMonto(), f.getCuotas(), EstadoIntento.valueOf(f.getEstado()), f.getIdExterno(),
                f.getIniciadoPor(), aInstante(f.getCreadoEn()), aInstante(f.getActualizadoEn()),
                f.getDevuelto(), f.getVersion());
    }

    /**
     * Solo la restriccion nombrada cuenta: cualquier otra violacion (un NOT NULL, otro indice unico como el
     * del id externo) es un error real y no debe disfrazarse de "ya hay un cobro activo".
     */
    static boolean violaRestriccion(Throwable e, String nombre) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().toLowerCase(Locale.ROOT).contains(nombre)) {
                return true;
            }
        }
        return false;
    }

    /** Ids de la base son INT; si un id del dominio no cabe, es un error, no se trunca en silencio. */
    static int aId(Long id) {
        return Math.toIntExact(id);
    }

    static LocalDateTime aLocal(Instant i) {
        return LocalDateTime.ofInstant(i, ZoneOffset.UTC);
    }

    static Instant aInstante(LocalDateTime l) {
        return l.toInstant(ZoneOffset.UTC);
    }
}
