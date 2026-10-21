package com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Guarda y busca intentos de pago. Solo persistencia: no decide nada.
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface IntentosDePagoPort {

    /**
     * Guarda (inserta o actualiza) y devuelve el intento ya con su id y su version nueva.
     *
     * @throws com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException.ConflictoDeConcurrencia si el intento ya
     *         cambio desde que se leyo (candado optimista por version)
     * @throws com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException si se intenta insertar un segundo cobro
     *         activo para el mismo pedido (T1, la base lo impide con una restriccion unica)
     */
    IntentoDePago guardar(IntentoDePago intento);

    Optional<IntentoDePago> buscar(Long intentoId);

    /** El intento que sigue esperando respuesta (CREADO o ENVIADO) de ese pedido, si lo hay (T1). */
    Optional<IntentoDePago> activoDelPedido(Integer pedidoId);

    /** Cuantos intentos RECHAZADOS tuvo el pedido desde {@code desde}. Cancelar no cuenta. */
    long rechazadosDesde(Integer pedidoId, Instant desde);

    /** Intentos RECHAZADO, CANCELADO o EXPIRADO que cambiaron desde {@code desde} (para reconciliarlos). */
    List<IntentoDePago> perdidosDesde(Instant desde);

    /** Los intentos que siguen activos y se crearon antes de {@code limite} (para expirarlos, T7). */
    List<IntentoDePago> activosCreadosAntesDe(Instant limite);
}
