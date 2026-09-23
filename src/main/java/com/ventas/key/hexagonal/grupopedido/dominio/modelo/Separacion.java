package com.ventas.key.hexagonal.grupopedido.dominio.modelo;

import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.SeparacionInvalidaException;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Las reglas para separar uno o varios pedidos de un grupo (R14-R16). Sin estado.
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 */
public final class Separacion {

    private Separacion() {
    }

    /**
     * @param grupo      el grupo tal como esta
     * @param abonos     los abonos de sus pedidos no cancelados: lo que el cliente ha dado
     * @param salen      los pedidos que se separan
     * @param reparto    pedidoId -> centavos que se queda cada pedido que sale (solo a credito)
     * @param nuevoTitular quien recoge a los que siguen unidos, si cambia
     */
    public static PlanDeSeparacion planear(GrupoPedidos grupo,
                                           List<AbonoRegistrado> abonos,
                                           Collection<Integer> salen,
                                           Map<Integer, Long> reparto,
                                           Integer nuevoTitular) {
        if (!grupo.activo()) {
            throw new SeparacionInvalidaException("El grupo " + grupo.grupoId() + " ya se habia separado");
        }
        Set<Integer> miembros = grupo.pedidos().stream().map(PedidoDelGrupo::pedidoId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Integer> eligio = salen == null ? new LinkedHashSet<>()
                : salen.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (eligio.isEmpty()) {
            throw new SeparacionInvalidaException("Elige que pedido se separa");
        }
        for (Integer id : eligio) {
            if (!miembros.contains(id)) {
                throw new SeparacionInvalidaException("El pedido #" + id + " no esta en el grupo " + grupo.grupoId());
            }
        }

        // Un cancelado ya no cuenta (R8): sale con lo suyo y no entra al reparto.
        List<PedidoDelGrupo> vivos = grupo.pedidos().stream().filter(p -> !p.estaCancelado())
                .sorted(masViejoPrimero()).toList();
        List<Integer> quedan = vivos.stream().map(PedidoDelGrupo::pedidoId).filter(id -> !eligio.contains(id)).toList();
        boolean termina = quedan.size() < 2;

        Set<Integer> salenTodos = new LinkedHashSet<>(miembros);
        if (!termina) {
            salenTodos.removeAll(quedan);
        }

        Map<Integer, Long> objetivos = grupo.esDeCredito()
                ? objetivosDeCredito(vivos, abonos, eligio, reparto == null ? Map.of() : reparto)
                : sinDinero(reparto);

        Integer titular = termina ? null : titularDeLosQueQuedan(grupo, quedan, nuevoTitular);
        return new PlanDeSeparacion(List.copyOf(salenTodos), termina ? List.of() : quedan, titular, objetivos);
    }

    /**
     * Cada pedido que sale se queda con lo que se escribio. Lo que no se reparte sigue siendo del
     * grupo y se acomoda en los que quedan, del mas viejo al mas nuevo (R6). La suma tiene que ser
     * exacta: ni mas ni menos de lo que el cliente ha dado (R14).
     */
    private static Map<Integer, Long> objetivosDeCredito(List<PedidoDelGrupo> vivos, List<AbonoRegistrado> abonos,
                                                         Set<Integer> eligio, Map<Integer, Long> reparto) {
        Set<Integer> idsVivos = vivos.stream().map(PedidoDelGrupo::pedidoId).collect(Collectors.toSet());
        long abonado = abonos.stream().filter(a -> idsVivos.contains(a.pedidoId())).mapToLong(AbonoRegistrado::centavos).sum();

        Map<Integer, Long> objetivos = new LinkedHashMap<>();
        long repartido = 0;
        for (PedidoDelGrupo p : vivos) {
            if (!eligio.contains(p.pedidoId())) {
                continue;
            }
            Long monto = reparto.get(p.pedidoId());
            if (monto == null) {
                throw new SeparacionInvalidaException("Falta decir cuanto de lo abonado se queda el pedido #" + p.pedidoId());
            }
            if (monto < 0) {
                throw new SeparacionInvalidaException("El monto del pedido #" + p.pedidoId() + " no puede ser negativo");
            }
            if (monto > p.totalCentavos()) {
                throw new SeparacionInvalidaException(String.format(
                        "El pedido #%d cuesta $%.2f: no se le pueden dejar $%.2f", p.pedidoId(),
                        p.totalCentavos() / 100.0, monto / 100.0));
            }
            objetivos.put(p.pedidoId(), monto);
            repartido += monto;
        }
        if (repartido > abonado) {
            throw new SeparacionInvalidaException(String.format(
                    "Repartiste $%.2f pero el cliente ha dado $%.2f", repartido / 100.0, abonado / 100.0));
        }

        long resto = abonado - repartido;
        List<PedidoDelGrupo> siguen = vivos.stream().filter(p -> !eligio.contains(p.pedidoId())).toList();
        if (siguen.isEmpty() && resto != 0) {
            throw new SeparacionInvalidaException(String.format(
                    "Repartiste $%.2f y el cliente ha dado $%.2f: tiene que ser exacto (faltan $%.2f)",
                    repartido / 100.0, abonado / 100.0, resto / 100.0));
        }
        for (PedidoDelGrupo p : siguen) {
            long parte = Math.min(resto, p.totalCentavos());
            objetivos.put(p.pedidoId(), parte);
            resto -= parte;
        }
        if (resto > 0) {
            throw new SeparacionInvalidaException(String.format(
                    "Sobran $%.2f: no caben en los pedidos que siguen unidos. Deja mas dinero en los que se separan",
                    resto / 100.0));
        }
        return objetivos;
    }

    private static Map<Integer, Long> sinDinero(Map<Integer, Long> reparto) {
        if (reparto != null && reparto.values().stream().anyMatch(m -> m != null && m != 0)) {
            throw new SeparacionInvalidaException("Un grupo de contado no tiene abonos que repartir");
        }
        return Map.of();
    }

    private static Integer titularDeLosQueQuedan(GrupoPedidos grupo, List<Integer> quedan, Integer nuevoTitular) {
        if (nuevoTitular != null) {
            if (!quedan.contains(nuevoTitular)) {
                throw new SeparacionInvalidaException("Quien recoge tiene que ser uno de los pedidos que siguen unidos");
            }
            return nuevoTitular;
        }
        if (quedan.contains(grupo.pedidoTitularId())) {
            return grupo.pedidoTitularId();
        }
        throw new SeparacionInvalidaException("El pedido #" + grupo.pedidoTitularId()
                + " era el que recoge y se separa: elige quien recoge los que siguen unidos");
    }

    static Comparator<PedidoDelGrupo> masViejoPrimero() {
        return Comparator.comparing(PedidoDelGrupo::registro, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PedidoDelGrupo::pedidoId);
    }
}
