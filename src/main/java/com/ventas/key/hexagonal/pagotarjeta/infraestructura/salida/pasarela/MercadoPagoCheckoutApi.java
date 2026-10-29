package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.pasarela;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Lo poco que el cobro en linea necesita de Mercado Pago, sin el SDK de por medio: el adaptador se prueba con una
 * implementacion falsa y solo {@link MercadoPagoCheckoutSdk} habla con la red.
 *
 * <p>[Hexagonal: Driven Adapter helper] [Clean: Frameworks & Drivers]
 */
public interface MercadoPagoCheckoutApi {

    /**
     * Crea la preferencia (la pagina de pago). Un 4xx de validacion lanza
     * {@code PagoConTarjetaException.CobroNoCreado}; cualquier otro fallo, una RuntimeException (no se sabe si se creo).
     *
     * @return id de la preferencia
     */
    String crearPreferencia(Preferencia preferencia);

    /** Pagos hechos contra esa referencia externa (el id del intento), del mas reciente al mas viejo. */
    List<Pago> pagosPorReferencia(String referencia);

    /** Un pago por su id (lo usa el webhook para saber de que intento es). */
    Optional<Pago> pago(long pagoId);

    /** Id de la preferencia creada con esa referencia externa, si existe (por si se perdio la respuesta al crearla). */
    Optional<String> preferenciaPorReferencia(String referencia);

    /** Hace que la pagina de pago deje de aceptar pagos. */
    void vencerPreferencia(String preferenciaId);

    /** Reembolsa {@code monto} del pago; repetir la misma {@code clave} no reembolsa dos veces. */
    void reembolsar(long pagoId, BigDecimal monto, String clave);

    record Preferencia(String referencia, String titulo, BigDecimal monto, int cuotasMaximas,
                       OffsetDateTime venceEn, String urlRegreso, String urlNotificacion) {
    }

    /** {@code estado}: el status de Mercado Pago tal cual (approved, rejected, in_process, pending, cancelled…). */
    record Pago(long id, String estado, String referencia, BigDecimal monto) {
    }
}
