package com.ventas.key.hexagonal.flete.dominio.puerto.salida;

import com.ventas.key.hexagonal.flete.dominio.modelo.LigaManual;
import com.ventas.key.hexagonal.flete.dominio.modelo.LigaManualDelViaje;
import com.ventas.key.hexagonal.flete.dominio.modelo.LineaSinViaje;
import com.ventas.key.hexagonal.flete.dominio.modelo.RenglonParaLigar;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** [Hexagonal: Driven Port] Lo que necesita ligar a mano ventas anteriores a un viaje. */
public interface LigasManualesPort {
    /** ¿Ya existen las tablas (migración corrida)? */
    boolean disponible();

    /** Líneas de venta (no devueltas) del producto con piezas aún sin ligar, de la más vieja a la más nueva; {@code desde}/{@code hasta} pueden ser null. */
    List<LineaSinViaje> lineasSinViaje(int productoId, LocalDate desde, LocalDate hasta);

    /** Bloquea el renglón del producto en ese viaje y devuelve cuántas piezas le quedan por vender; vacío si el producto no está en el viaje. */
    Optional<RenglonParaLigar> bloquearRenglon(int viajeId, int productoId);

    /** Piezas sin ligar de cada una de estas líneas (solo líneas del producto, no devueltas, con pendientes). */
    Map<Integer, Integer> pendientesDe(int productoId, Collection<Integer> detalleVentaIds);

    void guardarManuales(int renglonId, BigDecimal fletePorPieza, List<LigaManual> ligas);

    /** Las ligas hechas a mano de este viaje (las de ventas devueltas no se incluyen), de la más nueva a la más vieja. */
    List<LigaManualDelViaje> ligasManualesDe(int viajeId);

    /** Quita una liga hecha a mano (origen MANUAL); false si no existe o no es manual (las automáticas no se tocan). */
    boolean quitarManual(int ligaId);
}
