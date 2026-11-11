package com.ventas.key.hexagonal.flete.dominio.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * [Hexagonal: Domain] [Clean: Entity] Java puro. Reparte las piezas de una línea de venta entre los viajes del producto.
 * Reglas del dueño: primero salen las piezas que ya había antes de los viajes (quedan «sin viaje», sin fila), después las del viaje más viejo que
 * aún tenga piezas, y así hacia los más nuevos. Si ya no hay piezas en ningún viaje, el resto también queda sin viaje.
 */
public final class AsignacionDeViaje {

    private AsignacionDeViaje() {
    }

    /**
     * @param cantidad       piezas de la línea
     * @param piezasSinViaje piezas que había antes de los viajes y siguen en existencia (salen primero)
     * @param disponibles    renglones con piezas restantes, del viaje más viejo al más nuevo
     */
    public static List<ParteAsignada> repartir(int cantidad, int piezasSinViaje, List<RenglonDisponible> disponibles) {
        List<ParteAsignada> partes = new ArrayList<>();
        int pendiente = Math.max(0, cantidad - Math.max(0, piezasSinViaje));
        for (RenglonDisponible r : disponibles) {
            if (pendiente <= 0) {
                break;
            }
            int toma = Math.min(pendiente, Math.max(0, r.restantes()));
            if (toma > 0) {
                partes.add(new ParteAsignada(r.renglonId(), toma, r.fletePorPieza()));
                pendiente -= toma;
            }
        }
        return partes;
    }

    /** Las piezas anteriores a los viajes que siguen en existencia: lo que hay en total menos lo que aún queda en los viajes (nunca negativo). */
    public static int piezasSinViaje(int existenciaAntesDeLaLinea, List<RenglonDisponible> disponibles) {
        int enViajes = disponibles.stream().mapToInt(r -> Math.max(0, r.restantes())).sum();
        return Math.max(0, existenciaAntesDeLaLinea - enViajes);
    }
}
