package com.ventas.key.hexagonal.grupopedido.dominio.excepcion;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Los pedidos a unir no tienen la misma forma de cobro (R3).
 *
 * <p>Trae el tipo de cada pedido para que la pantalla diga cuales no coinciden. Se corrige con el
 * boton "Cambiar forma de cobro" del detalle de cada pedido, y despues se vuelve a unir.
 */
public class PedidosDeDistintoTipoException extends GrupoPedidoException {

    private final transient Map<Integer, String> tipoPorPedido;

    public PedidosDeDistintoTipoException(Map<Integer, String> tipoPorPedido) {
        super("Solo se pueden unir pedidos con la misma forma de cobro. "
                + tipoPorPedido.entrySet().stream()
                        .map(e -> "Pedido #" + e.getKey() + ": " + nombreDe(e.getValue()))
                        .collect(Collectors.joining(", "))
                + ". Cambia la forma de cobro de los que no coinciden desde el detalle del pedido "
                + "y vuelve a unirlos.");
        this.tipoPorPedido = tipoPorPedido;
    }

    public Map<Integer, String> tipoPorPedido() {
        return tipoPorPedido;
    }

    private static String nombreDe(String tipo) {
        return switch (tipo == null ? "" : tipo) {
            case "NORMAL" -> "Contado";
            case "APARTADO" -> "Apartado";
            case "FIADO" -> "Ir pagando";
            default -> String.valueOf(tipo);
        };
    }
}
