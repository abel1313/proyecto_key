package com.ventas.key.hexagonal.campana.dominio.modelo;

import java.math.BigDecimal;

/** [Hexagonal: Domain] [Clean: Entity] Java puro. Las dos reglas de la campaña: quién es recurrente y a qué precio se cobra cada línea. */
public final class ReglasCampana {

    private ReglasCampana() {
    }

    /**
     * ¿Es cliente recurrente? Si el administrador lo marcó a mano, sí (ignora la meta). Si no, cuando lo que lleva acumulado en la campaña más lo de
     * este ticket (sin contar los juguetes de la campaña) llega a la meta. Una meta de 0 significa «sin meta»: solo se es recurrente a mano.
     */
    public static boolean califica(BigDecimal acumulado, BigDecimal ticketSinJuguetes, BigDecimal meta, boolean manual) {
        if (manual) {
            return true;
        }
        if (meta == null || meta.signum() <= 0) {
            return false;
        }
        BigDecimal a = acumulado == null ? BigDecimal.ZERO : acumulado;
        BigDecimal t = ticketSinJuguetes == null ? BigDecimal.ZERO : ticketSinJuguetes;
        return a.add(t).compareTo(meta) >= 0;
    }

    /**
     * El precio de una pieza: el de campaña solo si es recurrente, la venta es Contado o Apartado y es MÁS BAJO que el que ya llevaba
     * (gana el más bajo). En cualquier otro caso, el que llevaba.
     */
    public static BigDecimal precioParaLinea(FormaDeCobro cobro, boolean recurrente, BigDecimal precioBase, BigDecimal precioCampana) {
        if (recurrente && cobro != null && cobro.admiteDescuentoDeCampana() && precioCampana != null
                && precioCampana.compareTo(precioBase) < 0) {
            return precioCampana;
        }
        return precioBase;
    }
}
