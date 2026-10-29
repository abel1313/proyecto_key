package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.pasarela;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CanalDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.CobroEnPasarela;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.EstadoExterno;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.IntentoDePago;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PasarelaDePagoPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * [Hexagonal: Driven Adapter] [Clean: Interface Adapter]
 *
 * <p>Cuando el cobro en linea es real (Checkout Pro) y la terminal sigue siendo otra pasarela (hoy la simulada),
 * esta es LA pasarela del dominio y reparte cada operacion: los cobros EN_LINEA van a Checkout Pro y los de
 * TERMINAL a la pasarela de la terminal. Para operar sobre un cobro ya creado se mira su identificador
 * ({@code co:} = Checkout Pro).
 */
@Component
@Primary
@ConditionalOnProperty(name = "pagos.tarjeta.pasarela-en-linea", havingValue = "checkout")
public class PasarelaPorCanalAdapter implements PasarelaDePagoPort {

    private final PasarelaCheckoutProAdapter enLinea;
    private final PasarelaDePagoPort terminal;

    @Autowired
    public PasarelaPorCanalAdapter(PasarelaCheckoutProAdapter enLinea,
                                   @Qualifier("pasarelaSimuladaAdapter") PasarelaDePagoPort terminal) {
        this.enLinea = enLinea;
        this.terminal = terminal;
    }

    private PasarelaDePagoPort de(String idExterno) {
        return PasarelaCheckoutProAdapter.esDeEstaPasarela(idExterno) ? enLinea : terminal;
    }

    @Override
    public String crearCobro(IntentoDePago intento) {
        return (intento.canal() == CanalDePago.EN_LINEA ? enLinea : terminal).crearCobro(intento);
    }

    @Override
    public EstadoExterno consultar(String idExterno) {
        return de(idExterno).consultar(idExterno);
    }

    @Override
    public Optional<CobroEnPasarela> buscarPorReferencia(Long intentoId) {
        Optional<CobroEnPasarela> enLineaEncontrado = enLinea.buscarPorReferencia(intentoId);
        return enLineaEncontrado.isPresent() ? enLineaEncontrado : terminal.buscarPorReferencia(intentoId);
    }

    @Override
    public void cancelar(String idExterno) {
        de(idExterno).cancelar(idExterno);
    }

    @Override
    public void devolver(String idExterno, BigDecimal monto, String claveIdempotencia) {
        de(idExterno).devolver(idExterno, monto, claveIdempotencia);
    }

    @Override
    public Optional<String> urlDePago(String idExterno) {
        return de(idExterno).urlDePago(idExterno);
    }
}
