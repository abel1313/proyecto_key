package com.ventas.key.hexagonal.flete.dominio.modelo;

import com.ventas.key.hexagonal.flete.dominio.excepcion.ViajeInvalidoException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * [Hexagonal: Domain] [Clean: Entity] Java puro. Las reglas de ligar a mano ventas anteriores a un viaje:
 * cada liga lleva al menos 1 pieza y no se repite la línea; no se puede ligar más de lo que esa línea tiene pendiente (cantidad menos lo ya ligado);
 * y el total no puede pasar de las piezas que aún le quedan al producto en ese viaje.
 */
public final class ReglasLigaManual {

    private ReglasLigaManual() {
    }

    /** @param pendientesPorLinea piezas sin ligar de cada línea (solo las líneas de ESE producto que no están devueltas); una línea que no aparece no se puede ligar */
    public static void validar(RenglonParaLigar renglon, Map<Integer, Integer> pendientesPorLinea, List<LigaManual> ligas) {
        if (ligas == null || ligas.isEmpty()) {
            throw new ViajeInvalidoException("Elige al menos una venta para ligar.");
        }
        Set<Integer> vistas = new HashSet<>();
        int total = 0;
        for (LigaManual l : ligas) {
            if (l.cantidad() < 1) {
                throw new ViajeInvalidoException("Cada venta debe ligar al menos 1 pieza.");
            }
            if (!vistas.add(l.detalleVentaId())) {
                throw new ViajeInvalidoException("Una misma venta está dos veces en la lista.");
            }
            Integer pendientes = pendientesPorLinea.get(l.detalleVentaId());
            if (pendientes == null) {
                throw new ViajeInvalidoException("Una de las ventas no es de «" + renglon.producto() + "», está devuelta o ya no tiene piezas por ligar.");
            }
            if (l.cantidad() > pendientes) {
                throw new ViajeInvalidoException("Una de las ventas solo tiene " + pendientes + " pieza(s) por ligar y quieres ligar " + l.cantidad() + ".");
            }
            total += l.cantidad();
        }
        if (total > renglon.restantes()) {
            throw new ViajeInvalidoException("«" + renglon.producto() + "» solo tiene " + renglon.restantes() + " pieza(s) por vender en «" + renglon.viaje()
                    + "» y quieres ligar " + total + ".");
        }
    }
}
