package com.ventas.key.hexagonal.grupopedido.dominio.modelo;

import com.ventas.key.hexagonal.grupopedido.dominio.excepcion.SeparacionInvalidaException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Que abonos hay que mover para que cada pedido termine con lo que le toca (R14).
 *
 * <p>[Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Se mueven abonos que ya existen en vez de registrar abonos nuevos: el dinero ya entro el dia
 * que se cobro, y un abono nuevo lo contaria otra vez en el corte de ese dia. Cada abono conserva
 * su fecha y su forma de pago; solo cambia de pedido. De cada pedido que sobra se mueven primero
 * los abonos mas nuevos.
 */
public final class ReacomodoDeAbonos {

    private ReacomodoDeAbonos() {
    }

    /**
     * @param abonos    los abonos de los pedidos que entran al reparto
     * @param objetivos pedidoId -> con cuantos centavos tiene que quedar
     */
    public static List<Movimiento> planear(List<AbonoRegistrado> abonos, Map<Integer, Long> objetivos) {
        Map<Integer, Long> actual = new HashMap<>();
        objetivos.keySet().forEach(id -> actual.put(id, 0L));
        for (AbonoRegistrado a : abonos) {
            if (!objetivos.containsKey(a.pedidoId())) {
                throw new SeparacionInvalidaException("El abono " + a.abonoId() + " es de un pedido que no entra al reparto");
            }
            actual.merge(a.pedidoId(), a.centavos(), Long::sum);
        }
        long abonado = actual.values().stream().mapToLong(Long::longValue).sum();
        long repartido = objetivos.values().stream().mapToLong(Long::longValue).sum();
        if (abonado != repartido) {
            throw new SeparacionInvalidaException(String.format(
                    "Lo repartido ($%.2f) tiene que ser igual a lo abonado ($%.2f)", repartido / 100.0, abonado / 100.0));
        }

        Deque<long[]> receptores = new ArrayDeque<>();
        new TreeMap<>(objetivos).forEach((id, objetivo) -> {
            long falta = objetivo - actual.get(id);
            if (falta > 0) {
                receptores.add(new long[]{id, falta});
            }
        });

        List<Movimiento> movimientos = new ArrayList<>();
        for (Map.Entry<Integer, Long> e : new TreeMap<>(objetivos).entrySet()) {
            Integer donante = e.getKey();
            long sobra = actual.get(donante) - e.getValue();
            if (sobra <= 0) {
                continue;
            }
            List<AbonoRegistrado> suyos = abonos.stream()
                    .filter(a -> a.pedidoId().equals(donante))
                    .sorted(Comparator.comparing(AbonoRegistrado::fecha, Comparator.nullsFirst(Comparator.naturalOrder()))
                            .thenComparing(AbonoRegistrado::abonoId).reversed())
                    .toList();
            for (AbonoRegistrado abono : suyos) {
                long disponible = Math.min(abono.centavos(), sobra);
                while (disponible > 0) {
                    long[] receptor = receptores.peek();
                    long parte = Math.min(disponible, receptor[1]);
                    movimientos.add(new Movimiento(abono.abonoId(), donante, (int) receptor[0], parte));
                    receptor[1] -= parte;
                    disponible -= parte;
                    sobra -= parte;
                    if (receptor[1] == 0) {
                        receptores.poll();
                    }
                }
                if (sobra == 0) {
                    break;
                }
            }
        }
        return movimientos;
    }
}
