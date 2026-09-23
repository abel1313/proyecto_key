package com.ventas.key.hexagonal.grupopedido.dominio.modelo;

import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.AbonoAlGrupoInvalidoException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Dos o mas pedidos que se cobran y se recogen juntos, sin dejar de ser pedidos distintos.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>El grupo no copia nada: los totales se calculan siempre desde los pedidos vivos, asi que si
 * a uno se le agrega un articulo o se cancela, el grupo ya lo refleja sin sincronizar nada.
 *
 * @param grupoId         id del grupo
 * @param pedidoTitularId el pedido cuyo cliente paga y recoge (R5)
 * @param activo          {@code false} una vez que se deshizo
 * @param creado          cuando se unio
 * @param nota            texto libre de quien lo unio
 * @param pedidos         los pedidos del grupo
 */
public record GrupoPedidos(
        Integer grupoId,
        Integer pedidoTitularId,
        boolean activo,
        LocalDateTime creado,
        String nota,
        List<PedidoDelGrupo> pedidos) {

    public long totalCentavos() {
        return pedidos.stream().filter(p -> !p.estaCancelado()).mapToLong(PedidoDelGrupo::totalCentavos).sum();
    }

    public long pagadoCentavos() {
        return pedidos.stream().filter(p -> !p.estaCancelado()).mapToLong(PedidoDelGrupo::pagadoCentavos).sum();
    }

    /** Lo que falta por pagar entre todos; los cancelados no cuentan (R8). */
    public long saldoCentavos() {
        return pedidos.stream().mapToLong(PedidoDelGrupo::saldoCentavos).sum();
    }

    public Optional<PedidoDelGrupo> titular() {
        return pedidos.stream().filter(p -> p.pedidoId().equals(pedidoTitularId)).findFirst();
    }

    /** Los pedidos que todavia deben algo, del mas viejo al mas nuevo (R6). */
    public List<PedidoDelGrupo> pendientesDeCobro() {
        return pedidos.stream()
                .filter(PedidoDelGrupo::estaAbierto)
                .filter(p -> p.saldoCentavos() > 0)
                .sorted(Comparator.comparing(PedidoDelGrupo::registro,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PedidoDelGrupo::pedidoId))
                .toList();
    }

    /**
     * Reparte un abono entre los pedidos, llenando primero el mas viejo (R6).
     *
     * <p>Liquidar primero el mas viejo es lo que el cliente espera ("ya termine de pagar el
     * primero") y hace que cada pedido que se completa genere su venta y se pueda entregar.
     */
    public List<Reparto> repartir(long montoCentavos) {
        if (!activo) {
            throw new AbonoAlGrupoInvalidoException("El grupo " + grupoId + " ya se deshizo: abona a cada pedido por separado");
        }
        List<PedidoDelGrupo> pendientes = pendientesDeCobro();
        if (pendientes.stream().anyMatch(p -> !p.esDeCredito())) {
            throw new AbonoAlGrupoInvalidoException("El grupo " + grupoId + " es de contado: los pedidos de contado "
                    + "no llevan abonos, se cobran completos al confirmarlos");
        }
        if (montoCentavos <= 0) {
            throw new AbonoAlGrupoInvalidoException("El monto del abono debe ser mayor a cero");
        }
        long saldo = saldoCentavos();
        if (montoCentavos > saldo) {
            throw new AbonoAlGrupoInvalidoException(String.format(
                    "El monto $%.2f excede el saldo del grupo de $%.2f", montoCentavos / 100.0, saldo / 100.0));
        }

        List<Reparto> repartos = new ArrayList<>();
        long restante = montoCentavos;
        for (PedidoDelGrupo pedido : pendientes) {
            if (restante == 0) {
                break;
            }
            long parte = Math.min(restante, pedido.saldoCentavos());
            repartos.add(new Reparto(pedido.pedidoId(), parte, parte == pedido.saldoCentavos()));
            restante -= parte;
        }
        return repartos;
    }
}
