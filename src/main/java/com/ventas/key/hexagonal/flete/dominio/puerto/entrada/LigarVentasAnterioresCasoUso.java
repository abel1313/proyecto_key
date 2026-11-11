package com.ventas.key.hexagonal.flete.dominio.puerto.entrada;

import com.ventas.key.hexagonal.flete.dominio.modelo.LigaManual;
import com.ventas.key.hexagonal.flete.dominio.modelo.LigaManualDelViaje;
import com.ventas.key.hexagonal.flete.dominio.modelo.LineaSinViaje;

import java.time.LocalDate;
import java.util.List;

/** [Hexagonal: Driving Port] Ligar a mano ventas anteriores a un viaje (solo administrador): ver las ventas sin viaje, ligarlas y deshacer una liga manual. */
public interface LigarVentasAnterioresCasoUso {
    List<LineaSinViaje> lineasSinViaje(int productoId, LocalDate desde, LocalDate hasta);

    /** Liga las piezas indicadas al viaje (guarda el flete por pieza de ese viaje). Devuelve cuántas piezas quedó ligadas. */
    int ligar(int viajeId, int productoId, List<LigaManual> ligas);

    /** Las ligas hechas a mano de un viaje, para poder deshacerlas. */
    List<LigaManualDelViaje> ligasManualesDelViaje(int viajeId);

    void quitarLiga(int ligaId);
}
