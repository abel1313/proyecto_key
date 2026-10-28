package com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoIntento;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.TipoDeCobro;

import java.util.Locale;

/**
 * Una regla del cobro con tarjeta que se rompio. Los mensajes estan en palabras del dueño: nunca
 * se imprime el nombre de un enum (APARTADO, ENVIADO...), sino su etiqueta.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>RuntimeException. Las subclases {@link CobroNoCreado} y {@link ConflictoDeConcurrencia}
 * permiten a la capa web responder 502 y 409 en vez del 400 generico.
 */
public class PagoConTarjetaException extends RuntimeException {

    protected PagoConTarjetaException(String mensaje) {
        super(mensaje);
    }

    /** La pasarela confirma que NO creo el cobro (rechazo claro antes de crearlo): se puede reintentar. */
    public static final class CobroNoCreado extends PagoConTarjetaException {
        public CobroNoCreado(String mensaje) {
            super(mensaje);
        }
    }

    /**
     * Faltan datos para registrar el pago en el pedido (el usuario que cobro, el catalogo de formas de pago).
     * Es un problema de datos, no un fallo pasajero: el dinero ya se cobro y queda por devolver.
     */
    public static final class NoSePudoAplicar extends PagoConTarjetaException {
        public NoSePudoAplicar(String mensaje) {
            super(mensaje);
        }
    }

    /** Otro proceso modifico el mismo cobro a la vez (polling, webhook, otro admin). */
    public static final class ConflictoDeConcurrencia extends PagoConTarjetaException {
        public ConflictoDeConcurrencia() {
            super("Este cobro acaba de cambiar por otra operación. Revisa su estado antes de continuar");
        }
    }

    public static PagoConTarjetaException pedidoNoExiste(Integer pedidoId) {
        return new PagoConTarjetaException("No existe el pedido " + pedidoId);
    }

    /** El producto o artículo no existe (el controller responde 404). */
    public static final class NoExiste extends PagoConTarjetaException {
        NoExiste(String mensaje) {
            super(mensaje);
        }
    }

    /** La base de ese ambiente todavía no tiene las columnas de migration_pago_tarjeta.sql (el controller responde 409). */
    public static final class CasillaMsiNoDisponible extends PagoConTarjetaException {
        CasillaMsiNoDisponible() {
            super("La casilla de meses sin intereses todavía no está disponible en este ambiente "
                    + "(falta correr migration_pago_tarjeta.sql)");
        }
    }

    public static PagoConTarjetaException productoNoExiste(Integer productoId) {
        return new NoExiste("No existe el producto " + productoId);
    }

    public static PagoConTarjetaException articuloNoExiste(Integer varianteId) {
        return new NoExiste("No existe el artículo " + varianteId);
    }

    public static PagoConTarjetaException casillaMsiNoDisponible() {
        return new CasillaMsiNoDisponible();
    }

    /** Un cobro de la terminal por el camino anterior (/v1/mp) sigue en proceso: no se cobra por los dos a la vez. */
    public static PagoConTarjetaException cobroPorElCaminoAnterior(Integer pedidoId) {
        return new PagoConTarjetaException("El pedido " + pedidoId + " tiene un cobro en la terminal todavía en proceso "
                + "por el camino anterior. Cancélalo o espera a que termine antes de cobrar con tarjeta aquí");
    }

    public static PagoConTarjetaException pedidoYaPagado(Integer pedidoId) {
        return new PagoConTarjetaException("El pedido " + pedidoId + " ya está pagado");
    }

    public static PagoConTarjetaException pedidoCancelado(Integer pedidoId) {
        return new PagoConTarjetaException("El pedido " + pedidoId + " está cancelado y no se puede cobrar");
    }

    public static PagoConTarjetaException pedidoEnGrupo(Integer pedidoId) {
        return new PagoConTarjetaException("El pedido " + pedidoId + " está unido a otros en un grupo. "
                + "El grupo no se cobra con tarjeta todavía: sepáralo o cóbralo en efectivo");
    }

    /** T3 */
    public static PagoConTarjetaException tarjetaNoPermitida(TipoDeCobro tipo) {
        String motivo = switch (tipo) {
            case APARTADO -> "Un Apartado es sin dinero. Para dar un abono, cámbialo a Ir pagando";
            case IR_PAGANDO -> "Ir pagando solo se paga con abonos en efectivo o transferencia";
            default -> "Esta forma de cobro no admite tarjeta";
        };
        return new PagoConTarjetaException("No se puede pagar con tarjeta. " + motivo);
    }

    /** T1 */
    public static PagoConTarjetaException intentoActivo(Integer pedidoId) {
        return new PagoConTarjetaException("El pedido " + pedidoId + " ya tiene un cobro con tarjeta "
                + "en proceso. Espera a que termine o cancélalo antes de intentar otro");
    }

    public static PagoConTarjetaException demasiadosRechazos() {
        return new PagoConTarjetaException("Se rechazaron varios intentos de pago de este pedido. "
                + "Por seguridad, espera unas horas antes de intentar de nuevo");
    }

    public static PagoConTarjetaException msiNoPermitido(String motivo) {
        return new PagoConTarjetaException("No se pueden usar meses sin intereses: " + motivo);
    }

    public static PagoConTarjetaException cuotasInvalidas(int cuotas) {
        return new PagoConTarjetaException("Las cuotas deben ser 1 (de contado) o un plan de meses "
                + "sin intereses; llegó " + cuotas);
    }

    public static PagoConTarjetaException datosInvalidos(String motivo) {
        return new PagoConTarjetaException("Datos del cobro inválidos: " + motivo);
    }

    public static PagoConTarjetaException transicionInvalida(EstadoIntento desde, EstadoIntento hacia) {
        return new PagoConTarjetaException("Un cobro " + desde.etiqueta() + " no puede pasar a "
                + hacia.etiqueta());
    }

    public static PagoConTarjetaException intentoNoExiste(Long intentoId) {
        return new PagoConTarjetaException("No existe el cobro " + intentoId);
    }

    /** No se pudo hablar con Mercado Pago y NO se sabe si cobro: nunca se dice "no se cobro nada". */
    public static PagoConTarjetaException pasarelaNoDisponible() {
        return new PagoConTarjetaException("No se pudo comunicar con Mercado Pago. "
                + "Espera un momento y revisa el estado del cobro antes de volver a intentarlo");
    }

    /** El cobro pudo haberse creado en Mercado Pago aunque aqui no se alcanzo a confirmar. */
    public static PagoConTarjetaException cobroIncierto() {
        return new PagoConTarjetaException("No se pudo confirmar si el cobro llegó a Mercado Pago. "
                + "NO lo vuelvas a intentar todavía: revisa la terminal o espera un minuto, el sistema "
                + "lo vuelve a buscar solo");
    }

    /** Se quiso cancelar pero la pasarela ya lo habia cobrado. */
    public static PagoConTarjetaException yaCobrado() {
        return new PagoConTarjetaException("El cobro ya se aprobó en Mercado Pago, así que no se canceló. "
                + "El pedido quedó pagado. Si hay que regresar el dinero, usa devolver");
    }

    /** T8 */
    public static PagoConTarjetaException devolucionInvalida(String motivo) {
        return new PagoConTarjetaException("No se puede devolver: " + motivo);
    }

    /** Dinero con formato fijo (punto decimal) sin depender del idioma del servidor. */
    public static String pesos(java.math.BigDecimal monto) {
        return String.format(Locale.US, "$%.2f", monto);
    }
}
