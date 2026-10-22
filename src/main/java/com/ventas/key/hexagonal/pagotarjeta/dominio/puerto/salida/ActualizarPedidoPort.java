package com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida;

import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.ResultadoAplicacion;

import java.math.BigDecimal;

/**
 * Lo que el cobro le pide al pedido cuando algo se confirma. Es el unico lugar donde el cobro
 * con tarjeta toca el pedido (T4 y T8).
 *
 * <p>Contrato: AMBOS metodos son <b>idempotentes</b>. Si el guardado posterior falla, el siguiente intento de
 * confirmar los vuelve a llamar. Si no fueran idempotentes, un fallo intermedio dejaria el pedido pagado dos
 * veces (o dos ventas).
 *
 * <p>[Hexagonal: Driven Port] [Clean: Interface Adapter]
 */
public interface ActualizarPedidoPort {

    /**
     * T4: el servidor confirmo el pago con la pasarela; el pedido pasa a Pagado.
     *
     * <p>El adaptador BLOQUEA la fila del pedido, vuelve a revisarlo con los datos de ese instante (cancelado,
     * forma de cobro, abonos, total, ya pagado) y solo entonces lo marca. Si ya no admite el pago devuelve
     * {@link ResultadoAplicacion#NO_APLICABLE} sin tocar nada; el servicio decide con esa respuesta, no con una
     * lectura anterior. Lanza {@code PagoConTarjetaException.NoSePudoAplicar} si faltan datos para registrar la venta (usuario,
     * catalogo de pago): el servicio lo trata como cobro por devolver y no como un cobro perdido.
     */
    ResultadoAplicacion marcarPagado(Integer pedidoId, Long intentoId);

    /**
     * T8: queda registrado cuanto se devolvio, quien y cuando. Idempotente por {@code clave}
     * (la misma que se mando a la pasarela).
     */
    void registrarDevolucion(Integer pedidoId, Long intentoId, BigDecimal monto, String hechaPor, String clave);
}
