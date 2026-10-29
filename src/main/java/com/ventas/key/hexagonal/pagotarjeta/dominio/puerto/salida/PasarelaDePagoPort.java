package com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CobroEnPasarela;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoExterno;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * La pasarela de pago (Mercado Pago). El dominio solo conoce estas operaciones; el adaptador decide
 * si usa la terminal o el Checkout y como traduce los estados.
 *
 * <p>Contrato que TODO adaptador debe cumplir:
 * <ul>
 *   <li>{@link #crearCobro}: manda el <b>id del intento como referencia externa</b>
 *       ({@code external_reference}) y como clave de idempotencia, para poder reencontrar el cobro con
 *       {@link #buscarPorReferencia} si la respuesta se pierde. Si la pasarela confirma que NO creo el
 *       cobro (un 4xx de validacion), lanza {@code PagoConTarjetaException.CobroNoCreado}; cualquier otro
 *       fallo (tiempo agotado, 5xx, red) lanza una RuntimeException cualquiera: el servicio entonces NO
 *       asume que no se cobro.</li>
 *   <li>{@link #consultar}: en Checkout Pro una preferencia puede tener varios pagos; si alguno esta
 *       aprobado, devuelve APROBADO aunque otro se haya rechazado.</li>
 *   <li>{@link #devolver}: la clave de idempotencia garantiza que repetir la misma devolucion no
 *       devuelve dos veces.</li>
 * </ul>
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface PasarelaDePagoPort {

    /** Crea el cobro en la pasarela y devuelve su identificador externo. */
    String crearCobro(IntentoDePago intento);

    EstadoExterno consultar(String idExterno);

    /** Busca un cobro por el id del intento (la referencia externa). Vacio = la pasarela no lo conoce. */
    Optional<CobroEnPasarela> buscarPorReferencia(Long intentoId);

    void cancelar(String idExterno);

    void devolver(String idExterno, BigDecimal monto, String claveIdempotencia);

    /**
     * Donde debe ir el cliente a pagar (Checkout Pro). Vacio = el cobro no se paga en una pagina externa
     * (terminal, pasarela simulada).
     */
    default Optional<String> urlDePago(String idExterno) {
        return Optional.empty();
    }
}
