package com.ventas.key.hexagonal.pagotarjeta.aplicacion.servicio;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.Devolucion;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoIntento;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.entrada.DevolverCobroCasoUso;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.ActualizarPedidoPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.DevolucionesPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.IntentosDePagoPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PasarelaDePagoPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.TransaccionPort;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

/**
 * [Hexagonal: implementacion del puerto de entrada] [Clean: Use Case Interactor]
 *
 * <p>T8. Orden: (1) se valida contra el modelo sin tocar nada, (2) se <b>reserva</b> la devolucion en
 * la base con su clave de idempotencia, (3) se devuelve en la pasarela, (4) en una transaccion se guarda
 * el intento, se confirma la reserva y se avisa al pedido.
 *
 * <p>La clave es {@code dev-<intento>-<lo ya devuelto>} y es unica en la base: dos solicitudes
 * simultaneas por la misma devolucion generan la MISMA clave y la segunda falla al reservar. Si el
 * back se cae entre (3) y (4), la reserva queda PENDIENTE: la siguiente solicitud la termina con la
 * misma clave (la pasarela no devuelve dos veces) antes de aceptar otra devolucion.
 *
 * <p>Un cobro COBRADO_SIN_APLICAR (doble pago) tambien se devuelve, pero el pedido no se toca: nunca
 * se aplico a el.
 *
 * <p>Sin {@code @Service} todavia (ver {@link CobroConTarjetaService}).
 */
public class DevolverCobroService implements DevolverCobroCasoUso {

    private final IntentosDePagoPort intentos;
    private final DevolucionesPort devoluciones;
    private final PasarelaDePagoPort pasarela;
    private final ActualizarPedidoPort actualizarPedido;
    private final TransaccionPort transaccion;
    private final Clock reloj;

    public DevolverCobroService(IntentosDePagoPort intentos, DevolucionesPort devoluciones,
                                PasarelaDePagoPort pasarela, ActualizarPedidoPort actualizarPedido,
                                TransaccionPort transaccion, Clock reloj) {
        this.intentos = intentos;
        this.devoluciones = devoluciones;
        this.pasarela = pasarela;
        this.actualizarPedido = actualizarPedido;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public IntentoDePago devolver(Long intentoId, BigDecimal monto, String hechaPor) {
        if (hechaPor == null || hechaPor.isBlank()) {
            throw PagoConTarjetaException.devolucionInvalida("falta quién hace la devolución");
        }
        IntentoDePago intento = intentos.buscar(intentoId)
                .orElseThrow(() -> PagoConTarjetaException.intentoNoExiste(intentoId));

        List<Devolucion> pendientes = devoluciones.pendientesDelIntento(intentoId);
        if (!pendientes.isEmpty()) {
            Devolucion pendiente = pendientes.get(0);
            terminar(intento, pendiente);
            throw PagoConTarjetaException.devolucionInvalida("había una devolución de "
                    + PagoConTarjetaException.pesos(pendiente.monto()) + " a medias y ya se terminó. "
                    + "Revisa el cobro y vuelve a pedir lo que falte");
        }

        BigDecimal cantidad = monto != null ? monto : intento.restanteADevolver();
        intento.devolviendo(cantidad, reloj.instant());                             // valida todo antes de tocar nada
        String clave = "dev-" + intento.id() + "-" + intento.devuelto().toPlainString();
        Devolucion reservada = devoluciones.reservar(
                Devolucion.reservada(intento.id(), cantidad.setScale(2), hechaPor, clave, reloj.instant()));
        return terminar(intento, reservada);
    }

    private IntentoDePago terminar(IntentoDePago intento, Devolucion reservada) {
        IntentoDePago resultado = intento.devolviendo(reservada.monto(), reloj.instant());
        try {
            pasarela.devolver(intento.idExterno(), reservada.monto(), reservada.clave());
        } catch (RuntimeException e) {
            throw PagoConTarjetaException.pasarelaNoDisponible();                   // la reserva queda PENDIENTE
        }
        boolean tocaElPedido = intento.estado() != EstadoIntento.COBRADO_SIN_APLICAR;
        return transaccion.ejecutar(() -> {
            IntentoDePago guardado = intentos.guardar(resultado);
            devoluciones.confirmar(reservada);
            if (tocaElPedido) {
                actualizarPedido.registrarDevolucion(guardado.pedidoId(), guardado.id(), reservada.monto(),
                        reservada.hechaPor(), reservada.clave());
            }
            return guardado;
        });
    }
}
