package com.ventas.key.hexagonal.grupopedido.dominio.modelo;

import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.AbonoAlGrupoInvalidoException;
import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.CambioDeFormaDeCobroInvalidoException;
import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.CobroDelGrupoInvalidoException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    public static GrupoPedidos de(RegistroGrupo registro, List<PedidoDelGrupo> pedidos) {
        return new GrupoPedidos(registro.grupoId(), registro.pedidoTitularId(), registro.activo(),
                registro.creado(), registro.nota(), pedidos);
    }

    public long totalCentavos() {
        return pedidos.stream().filter(p -> !p.estaCancelado()).mapToLong(PedidoDelGrupo::totalCentavos).sum();
    }

    public long pagadoCentavos() {
        return pedidos.stream().filter(p -> !p.estaCancelado()).mapToLong(PedidoDelGrupo::cobradoCentavos).sum();
    }

    /** Lo que falta por pagar entre todos; los cancelados no cuentan (R8). */
    public long saldoCentavos() {
        return pedidos.stream().mapToLong(PedidoDelGrupo::saldoCentavos).sum();
    }

    /** A credito (Apartado / Ir pagando): lleva abonos, y al separar se reparten. */
    public boolean esDeCredito() {
        return pedidos.stream().filter(p -> !p.estaCancelado()).anyMatch(PedidoDelGrupo::esDeCredito);
    }

    /**
     * La forma de cobro del grupo: todos sus pedidos vivos la comparten (R3, R9). Se toma la del
     * titular; si el titular se cancelo, la de cualquier otro vivo.
     */
    public String tipo() {
        return titular().filter(p -> !p.estaCancelado())
                .or(() -> pedidos.stream().filter(p -> !p.estaCancelado()).findFirst())
                .or(this::titular)
                .map(PedidoDelGrupo::tipo)
                .orElse(null);
    }

    public boolean contiene(Integer pedidoId) {
        return pedidos.stream().anyMatch(p -> p.pedidoId().equals(pedidoId));
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
     * Los pedidos que se confirman al cobrar el grupo de contado de una vez (R11), del mas viejo al
     * mas nuevo.
     *
     * <p>Los que ya estan entregados se saltan en vez de rechazar el cobro: con la terminal, el
     * aviso de Mercado Pago puede confirmar el pedido titular antes de que llegue este cobro.
     */
    public List<Integer> porCobrarDeContado() {
        if (!activo) {
            throw new CobroDelGrupoInvalidoException("El grupo " + grupoId + " ya se deshizo: cobra cada pedido por separado");
        }
        List<PedidoDelGrupo> abiertos = pedidos.stream()
                .filter(PedidoDelGrupo::estaAbierto)
                .sorted(Comparator.comparing(PedidoDelGrupo::registro,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PedidoDelGrupo::pedidoId))
                .toList();
        if (abiertos.stream().anyMatch(PedidoDelGrupo::esDeCredito)) {
            throw new CobroDelGrupoInvalidoException("El grupo " + grupoId + " es a credito: se cobra abonando al grupo "
                    + "desde el detalle del pedido");
        }
        if (abiertos.isEmpty()) {
            throw new CobroDelGrupoInvalidoException("Los pedidos del grupo " + grupoId + " ya estan cobrados o cancelados");
        }
        return abiertos.stream().map(PedidoDelGrupo::pedidoId).toList();
    }

    /**
     * Cambiar la forma de cobro del grupo entero (R20, A10 del plan): todos sus pedidos vivos pasan
     * juntos, asi el grupo sigue con una sola forma de cobro (R3).
     *
     * <p>Caso que lo origina (2026-10-13): el cliente hace dos pedidos desde su cuenta (quedan
     * Pendiente), se unen, y luego pide irlos pagando. Antes habia que separar, cambiar cada uno y
     * volver a unir.
     *
     * <ul>
     *   <li>Ir pagando: siempre; si deja dinero ahora, entra como abono del grupo (R6).</li>
     *   <li>Apartado: solo sin dinero, ni el que ya dio ni uno nuevo (un Apartado se liquida
     *       completo al recogerlo).</li>
     *   <li>Contado: no con este cambio. Para que quede pagado se cobra completo: Pagar el grupo
     *       completo (credito) o Cobrar el grupo (contado, R11).</li>
     *   <li>Un pedido que ya se cobro de contado (Entregado) primero sale del grupo: pasarlo a
     *       credito anula su venta, y eso se hace pedido por pedido.</li>
     * </ul>
     *
     * @param tipoNuevo          {@code APARTADO} o {@code FIADO}
     * @param cobroAhoraCentavos lo que el cliente deja en este momento (0 si nada)
     * @return los pedidos que cambian, del mas viejo al mas nuevo
     */
    public List<Integer> porCambiarFormaDeCobro(String tipoNuevo, long cobroAhoraCentavos) {
        if (!activo) {
            throw new CambioDeFormaDeCobroInvalidoException("El grupo " + grupoId
                    + " ya se deshizo: cambia la forma de cobro de cada pedido por separado");
        }
        String tipo = tipoNuevo == null ? "" : tipoNuevo.trim().toUpperCase();
        if (!Set.of("NORMAL", "APARTADO", "FIADO").contains(tipo)) {
            throw new CambioDeFormaDeCobroInvalidoException("Forma de cobro inválida: " + tipoNuevo);
        }
        if (tipo.equals(tipo())) {
            throw new CambioDeFormaDeCobroInvalidoException("El grupo ya es " + nombre(tipo));
        }
        if ("NORMAL".equals(tipo)) {
            throw new CambioDeFormaDeCobroInvalidoException("Un grupo no se pasa a contado con este cambio: "
                    + "para que quede pagado, cóbralo completo con 💵 Pagar el grupo completo");
        }
        pedidos.stream().filter(PedidoDelGrupo::estaEntregado).findFirst().ifPresent(p -> {
            throw new CambioDeFormaDeCobroInvalidoException("El pedido " + p.pedidoId() + " ya se cobró de contado: "
                    + "sepáralo del grupo y cámbialo solo, antes de cambiar la forma de cobro del grupo");
        });
        if ("APARTADO".equals(tipo)) {
            if (cobroAhoraCentavos > 0) {
                throw new CambioDeFormaDeCobroInvalidoException("Un Apartado es sin dinero: si el cliente deja algo "
                        + "ahora, el grupo pasa a Ir pagando");
            }
            if (pagadoCentavos() > 0) {
                throw new CambioDeFormaDeCobroInvalidoException(String.format(
                        "Un Apartado es sin dinero y el grupo ya tiene $%.2f pagados: se queda en Ir pagando",
                        pagadoCentavos() / 100.0));
            }
        }
        if (cobroAhoraCentavos < 0) {
            throw new CambioDeFormaDeCobroInvalidoException("El monto que deja el cliente no puede ser negativo");
        }
        if (cobroAhoraCentavos > saldoCentavos()) {
            throw new CambioDeFormaDeCobroInvalidoException(String.format(
                    "El monto $%.2f excede lo que debe el grupo, $%.2f", cobroAhoraCentavos / 100.0, saldoCentavos() / 100.0));
        }
        return pedidos.stream()
                .filter(p -> !p.estaCancelado())
                .filter(p -> !tipo.equals(p.tipo()))
                .sorted(Comparator.comparing(PedidoDelGrupo::registro,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PedidoDelGrupo::pedidoId))
                .map(PedidoDelGrupo::pedidoId)
                .toList();
    }

    /** Como le dice el dueño a cada forma de cobro (skill reglas-pedidos 1.2). */
    public static String nombre(String tipo) {
        if (tipo == null) {
            return "";
        }
        return switch (tipo) {
            case "APARTADO" -> "Apartado";
            case "FIADO" -> "Ir pagando";
            case "NORMAL" -> "de contado";
            default -> tipo;
        };
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
        // Un Apartado es sin dinero: se paga completo al recogerlo. Un adelanto es Ir pagando.
        if ("APARTADO".equals(tipo()) && montoCentavos < saldo) {
            throw new AbonoAlGrupoInvalidoException(String.format(
                    "Un Apartado se paga completo al recogerlo: el grupo debe $%.2f. "
                            + "Para dar un adelanto, los pedidos tienen que pasar a Ir pagando", saldo / 100.0));
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
