package com.ventas.key.hexagonal.flete.aplicacion.servicio;

import com.ventas.key.hexagonal.flete.dominio.excepcion.ViajeInvalidoException;
import com.ventas.key.hexagonal.flete.dominio.modelo.LigaManual;
import com.ventas.key.hexagonal.flete.dominio.modelo.LineaSinViaje;
import com.ventas.key.hexagonal.flete.dominio.modelo.ReglasLigaManual;
import com.ventas.key.hexagonal.flete.dominio.puerto.entrada.LigarVentasAnterioresCasoUso;
import com.ventas.key.hexagonal.flete.dominio.puerto.salida.LigasManualesPort;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** [Hexagonal: Application] [Clean: Use Case] Orquesta; las reglas viven en {@link ReglasLigaManual}. Java puro. */
public class LigarVentasAnterioresService implements LigarVentasAnterioresCasoUso {

    private static final String SIN_MIGRACION = "Falta correr la migración de ventas por viaje (migration_flete_venta.sql).";

    private final LigasManualesPort ligas;

    public LigarVentasAnterioresService(LigasManualesPort ligas) {
        this.ligas = ligas;
    }

    @Override
    public List<LineaSinViaje> lineasSinViaje(int productoId, LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new ViajeInvalidoException("La fecha final no puede ser antes que la inicial.");
        }
        return ligas.disponible() ? ligas.lineasSinViaje(productoId, desde, hasta) : List.of();
    }

    @Override
    public int ligar(int viajeId, int productoId, List<LigaManual> lista) {
        if (!ligas.disponible()) {
            throw new ViajeInvalidoException(SIN_MIGRACION);
        }
        var renglon = ligas.bloquearRenglon(viajeId, productoId)
                .orElseThrow(() -> new ViajeInvalidoException("Ese producto no está en el viaje elegido."));
        List<LigaManual> pedidas = lista == null ? List.of() : lista;
        Map<Integer, Integer> pendientes = ligas.pendientesDe(productoId, pedidas.stream().map(LigaManual::detalleVentaId).toList());
        ReglasLigaManual.validar(renglon, pendientes, pedidas);
        ligas.guardarManuales(renglon.renglonId(), renglon.fletePorPieza(), pedidas);
        return pedidas.stream().mapToInt(LigaManual::cantidad).sum();
    }

    @Override
    public void quitarLiga(int ligaId) {
        if (!ligas.disponible()) {
            throw new ViajeInvalidoException(SIN_MIGRACION);
        }
        if (!ligas.quitarManual(ligaId)) {
            throw new ViajeInvalidoException("Esa liga no existe o es automática (solo se deshacen las que se ligaron a mano).");
        }
    }
}
