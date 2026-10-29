package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.pasarela;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CobroEnPasarela;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoExterno;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PasarelaDePagoPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * [Hexagonal: Driven Adapter] [Clean: Interface Adapter]
 *
 * <p>Cobro en linea con Checkout Pro de Mercado Pago: el cliente paga en la pagina segura de Mercado Pago, asi
 * que aqui nunca pasan datos de tarjeta. El id del intento viaja como {@code external_reference} y es lo que
 * permite reencontrar el pago aunque se pierda una respuesta.
 *
 * <p>{@code idExterno} que se guarda: {@code co:<intentoId>:<preferenciaId>} (cabe en la columna de 100). El
 * prefijo {@code co:} distingue estos cobros de los simulados cuando conviven (ver {@link PasarelaPorCanalAdapter}).
 *
 * <p>Estados (preferencia con {@code binary_mode}): algun pago aprobado = APROBADO (aunque otro intento se haya
 * rechazado); sin pagos = PENDIENTE; pagos y ninguno aprobado ni en proceso = RECHAZADO (si luego el cliente
 * reintenta en la misma pagina y aprueba, el dominio admite la aprobacion tardia).
 */
@Component
@ConditionalOnProperty(name = "pagos.tarjeta.pasarela-en-linea", havingValue = "checkout")
public class PasarelaCheckoutProAdapter implements PasarelaDePagoPort {

    static final String PREFIJO = "co:";

    private final MercadoPagoCheckoutApi api;
    private final Clock reloj;
    private final String urlRegreso;
    private final String urlNotificacion;
    private final String urlRedireccion;

    @Autowired
    public PasarelaCheckoutProAdapter(
            MercadoPagoCheckoutApi api, Clock reloj,
            @Value("${pagos.tarjeta.checkout.front-url}") String frontUrl,
            @Value("${pagos.tarjeta.checkout.notificacion-url}") String urlNotificacion,
            @Value("${pagos.tarjeta.checkout.redirect-url:https://www.mercadopago.com.mx/checkout/v1/redirect?pref_id=}")
            String urlRedireccion) {
        this.api = api;
        this.reloj = reloj;
        this.urlRegreso = frontUrl.replaceAll("/+$", "") + "/pedidos/pago-resultado";
        this.urlNotificacion = urlNotificacion;
        this.urlRedireccion = urlRedireccion;
    }

    static boolean esDeEstaPasarela(String idExterno) {
        return idExterno != null && idExterno.startsWith(PREFIJO);
    }

    @Override
    public String crearCobro(IntentoDePago intento) {
        String referencia = String.valueOf(intento.id());
        OffsetDateTime vence = intento.creadoEn().plus(intento.canal().vigencia()).atOffset(ZoneOffset.UTC);
        String preferenciaId = api.crearPreferencia(new MercadoPagoCheckoutApi.Preferencia(
                referencia, "Pedido #" + intento.pedidoId() + " — Novedades Jade", intento.monto(), intento.cuotas(),
                vence, urlRegreso, urlNotificacion));
        if (preferenciaId == null || preferenciaId.isBlank()) {
            throw new IllegalStateException("Mercado Pago no devolvió el identificador del cobro");
        }
        return PREFIJO + intento.id() + ":" + preferenciaId;
    }

    @Override
    public EstadoExterno consultar(String idExterno) {
        return estadoDe(api.pagosPorReferencia(referencia(idExterno)));
    }

    @Override
    public Optional<CobroEnPasarela> buscarPorReferencia(Long intentoId) {
        String referencia = String.valueOf(intentoId);
        List<MercadoPagoCheckoutApi.Pago> pagos = api.pagosPorReferencia(referencia);
        Optional<String> preferencia = api.preferenciaPorReferencia(referencia);
        if (preferencia.isEmpty() && pagos.isEmpty()) {
            return Optional.empty();
        }
        String id = PREFIJO + intentoId + ":" + preferencia.orElse("sin-preferencia");
        return Optional.of(new CobroEnPasarela(id, estadoDe(pagos)));
    }

    @Override
    public void cancelar(String idExterno) {
        if (consultar(idExterno) == EstadoExterno.APROBADO) {
            throw new IllegalStateException("el cobro ya está aprobado y no se puede cancelar");
        }
        // Una preferencia no se borra: se vence para que la pagina ya no acepte pagos.
        api.vencerPreferencia(preferenciaId(idExterno));
    }

    @Override
    public void devolver(String idExterno, BigDecimal monto, String claveIdempotencia) {
        MercadoPagoCheckoutApi.Pago aprobado = api.pagosPorReferencia(referencia(idExterno)).stream()
                .filter(p -> "approved".equalsIgnoreCase(p.estado())).findFirst()
                .orElseThrow(() -> new IllegalStateException("no hay un pago aprobado que devolver"));
        api.reembolsar(aprobado.id(), monto, claveIdempotencia);
    }

    @Override
    public Optional<String> urlDePago(String idExterno) {
        if (!esDeEstaPasarela(idExterno)) return Optional.empty();
        String preferencia = preferenciaId(idExterno);
        return "sin-preferencia".equals(preferencia) ? Optional.empty() : Optional.of(urlRedireccion + preferencia);
    }

    static EstadoExterno estadoDe(List<MercadoPagoCheckoutApi.Pago> pagos) {
        if (pagos.stream().anyMatch(p -> "approved".equalsIgnoreCase(p.estado()))) return EstadoExterno.APROBADO;
        if (pagos.isEmpty()) return EstadoExterno.PENDIENTE;
        boolean enProceso = pagos.stream().anyMatch(p -> {
            String e = p.estado() == null ? "" : p.estado().toLowerCase();
            return e.equals("pending") || e.equals("in_process") || e.equals("authorized") || e.equals("in_mediation");
        });
        return enProceso ? EstadoExterno.PENDIENTE : EstadoExterno.RECHAZADO;
    }

    private static String referencia(String idExterno) {
        return partes(idExterno)[1];
    }

    private static String preferenciaId(String idExterno) {
        return partes(idExterno)[2];
    }

    private static String[] partes(String idExterno) {
        if (!esDeEstaPasarela(idExterno)) {
            throw new IllegalStateException("el cobro " + idExterno + " no es de Mercado Pago Checkout");
        }
        String[] p = idExterno.split(":", 3);
        if (p.length != 3) {
            throw new PagoConTarjetaException.CobroNoCreado("identificador de cobro inválido");
        }
        return p;
    }
}
