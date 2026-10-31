package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.pasarela;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePaymentMethodsRequest;
import com.mercadopago.client.preference.PreferencePaymentTypeRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.MPSearchRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.mercadopago.resources.preference.PreferenceSearch;
import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * El unico lugar que habla con Mercado Pago para el cobro en linea (SDK oficial {@code sdk-java}).
 * Usa el access token de {@code mercadopago.access-token} en cada llamada, sin depender del estado global del SDK.
 * Nunca registra el token ni datos de tarjeta (no los recibimos).
 *
 * <p>Existe solo con {@code pagos.tarjeta.pasarela-en-linea=checkout}.
 */
@Component
@ConditionalOnProperty(name = "pagos.tarjeta.pasarela-en-linea", havingValue = "checkout")
public class MercadoPagoCheckoutSdk implements MercadoPagoCheckoutApi {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoCheckoutSdk.class);

    /** Solo tarjeta: se excluye lo que no es tarjeta (efectivo en tienda, cajero, transferencia). */
    private static final List<String> TIPOS_EXCLUIDOS = List.of("ticket", "atm", "bank_transfer");

    private final String accessToken;

    public MercadoPagoCheckoutSdk(@Value("${mercadopago.access-token}") String accessToken) {
        this.accessToken = accessToken;
    }

    private MPRequestOptions opciones() {
        return MPRequestOptions.builder().accessToken(accessToken).connectionTimeout(10_000).socketTimeout(15_000).build();
    }

    private MPRequestOptions opciones(Map<String, String> encabezados) {
        return MPRequestOptions.builder().accessToken(accessToken).connectionTimeout(10_000).socketTimeout(15_000)
                .customHeaders(encabezados).build();
    }

    @Override
    public String crearPreferencia(Preferencia p) {
        PreferenceRequest request = PreferenceRequest.builder()
                .items(List.of(PreferenceItemRequest.builder()
                        .id(p.referencia()).title(p.titulo()).quantity(1).currencyId("MXN").unitPrice(p.monto()).build()))
                .externalReference(p.referencia())
                .backUrls(PreferenceBackUrlsRequest.builder()
                        .success(p.urlRegreso()).pending(p.urlRegreso()).failure(p.urlRegreso()).build())
                .autoReturn("approved")
                .notificationUrl(p.urlNotificacion())
                .binaryMode(true)                              // solo aprobado o rechazado, sin "pendiente"
                .expires(true)
                .expirationDateFrom(OffsetDateTime.now())
                .expirationDateTo(p.venceEn())
                .paymentMethods(PreferencePaymentMethodsRequest.builder()
                        .installments(Math.max(1, p.cuotasMaximas()))
                        .excludedPaymentTypes(TIPOS_EXCLUIDOS.stream()
                                .map(id -> PreferencePaymentTypeRequest.builder().id(id).build()).toList())
                        .build())
                .build();
        try {
            Preference creada = new PreferenceClient().create(request, opciones());
            return creada.getId();
        } catch (MPApiException e) {
            log.warn("Mercado Pago rechazó crear la preferencia (HTTP {}): {}", e.getStatusCode(), cuerpo(e));
            if (e.getStatusCode() >= 400 && e.getStatusCode() < 500 && e.getStatusCode() != 408 && e.getStatusCode() != 429) {
                throw new PagoConTarjetaException.CobroNoCreado("Mercado Pago no aceptó el cobro (" + e.getStatusCode() + ")");
            }
            throw new IllegalStateException("Mercado Pago respondió " + e.getStatusCode() + " al crear el cobro", e);
        } catch (MPException e) {
            throw new IllegalStateException("No se pudo hablar con Mercado Pago al crear el cobro", e);
        }
    }

    @Override
    public Optional<String> urlDePago(String preferenciaId, boolean sandbox) {
        try {
            Preference p = new PreferenceClient().get(preferenciaId, opciones());
            log.info("Preferencia {}: init_point={} sandbox_init_point={}", preferenciaId, p.getInitPoint(), p.getSandboxInitPoint());
            String url = sandbox ? p.getSandboxInitPoint() : p.getInitPoint();
            return url == null || url.isBlank() ? Optional.empty() : Optional.of(url);
        } catch (MPApiException | MPException e) {
            log.warn("No se pudo obtener la dirección de pago de la preferencia {}", preferenciaId, e);
            return Optional.empty();
        }
    }

    @Override
    public List<Pago> pagosPorReferencia(String referencia) {
        try {
            MPSearchRequest busqueda = MPSearchRequest.builder().limit(30).offset(0)
                    .filters(Map.of("external_reference", referencia, "sort", "date_created", "criteria", "desc")).build();
            return new PaymentClient().search(busqueda, opciones()).getResults().stream().map(MercadoPagoCheckoutSdk::aPago).toList();
        } catch (MPApiException | MPException e) {
            throw new IllegalStateException("No se pudo consultar el pago en Mercado Pago", e);
        }
    }

    @Override
    public Optional<Pago> pago(long pagoId) {
        try {
            return Optional.of(aPago(new PaymentClient().get(pagoId, opciones())));
        } catch (MPApiException e) {
            if (e.getStatusCode() == 404) return Optional.empty();
            throw new IllegalStateException("Mercado Pago respondió " + e.getStatusCode() + " al consultar el pago", e);
        } catch (MPException e) {
            throw new IllegalStateException("No se pudo consultar el pago en Mercado Pago", e);
        }
    }

    @Override
    public Optional<String> preferenciaPorReferencia(String referencia) {
        try {
            MPSearchRequest busqueda = MPSearchRequest.builder().limit(5).offset(0)
                    .filters(Map.of("external_reference", referencia)).build();
            return new PreferenceClient().search(busqueda, opciones()).getElements().stream()
                    .filter(x -> referencia.equals(x.getExternalReference()))
                    .map(PreferenceSearch::getId).findFirst();
        } catch (MPApiException | MPException e) {
            throw new IllegalStateException("No se pudo buscar el cobro en Mercado Pago", e);
        }
    }

    @Override
    public void vencerPreferencia(String preferenciaId) {
        try {
            PreferenceRequest vencida = PreferenceRequest.builder().expires(true)
                    .expirationDateTo(OffsetDateTime.now().minusSeconds(1)).build();
            new PreferenceClient().update(preferenciaId, vencida, opciones());
        } catch (MPApiException | MPException e) {
            throw new IllegalStateException("No se pudo cancelar el cobro en Mercado Pago", e);
        }
    }

    @Override
    public void reembolsar(long pagoId, BigDecimal monto, String clave) {
        try {
            new PaymentClient().refund(pagoId, monto, opciones(Map.of("X-Idempotency-Key", clave)));
        } catch (MPApiException e) {
            log.warn("Mercado Pago rechazó el reembolso del pago {} (HTTP {}): {}", pagoId, e.getStatusCode(), cuerpo(e));
            throw new IllegalStateException("Mercado Pago respondió " + e.getStatusCode() + " al reembolsar", e);
        } catch (MPException e) {
            throw new IllegalStateException("No se pudo hablar con Mercado Pago al reembolsar", e);
        }
    }

    private static Pago aPago(Payment p) {
        return new Pago(p.getId(), p.getStatus(), p.getExternalReference(), p.getTransactionAmount());
    }

    private static String cuerpo(MPApiException e) {
        return e.getApiResponse() != null ? e.getApiResponse().getContent() : "";
    }
}
