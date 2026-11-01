package com.ventas.key.hexagonal.revisionventas.aplicacion.servicio;

import com.ventas.key.hexagonal.revisionventas.dominio.excepcion.RevisionVentasException;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.BusquedaDeHallazgos;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.HallazgosDeUnTipo;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.ResultadoRevision;
import com.ventas.key.hexagonal.revisionventas.dominio.modelo.TipoHallazgo;
import com.ventas.key.hexagonal.revisionventas.dominio.puerto.entrada.RevisarVentasCasoUso;
import com.ventas.key.hexagonal.revisionventas.dominio.puerto.salida.HallazgosPort;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * [Hexagonal: Use Case] [Clean: Use Cases] Java puro, sin Spring.
 *
 * <p>Pone las fechas por defecto, recorre cada tipo de hallazgo y junta lo que encuentra. Si un tipo no se puede
 * revisar en este ambiente lo anota en {@code noRevisados} y sigue con los demás: una tabla que falta no tumba la revisión.
 */
public class RevisarVentasService implements RevisarVentasCasoUso {

    /** Cuántos casos concretos se muestran por tipo. */
    public static final int EJEMPLOS_POR_TIPO = 50;
    public static final int DIAS_POR_DEFECTO = 90;
    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private final HallazgosPort hallazgos;
    private final Clock reloj;

    public RevisarVentasService(HallazgosPort hallazgos, Clock reloj) {
        this.hallazgos = hallazgos;
        this.reloj = reloj;
    }

    @Override
    public ResultadoRevision revisar(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now(reloj.withZone(ZONA));
        LocalDate inicio = desde != null ? desde : fin.minusDays(DIAS_POR_DEFECTO);
        if (inicio.isAfter(fin)) {
            throw new RevisionVentasException.FechasInvalidas("La fecha «desde» no puede ser después de «hasta».");
        }
        List<HallazgosDeUnTipo> revisados = new ArrayList<>();
        List<TipoHallazgo> noRevisados = new ArrayList<>();
        for (TipoHallazgo tipo : TipoHallazgo.values()) {
            try {
                BusquedaDeHallazgos b = hallazgos.buscar(tipo, inicio, fin, EJEMPLOS_POR_TIPO);
                revisados.add(new HallazgosDeUnTipo(tipo, b.total(), b.ejemplos()));
            } catch (RevisionVentasException.RevisionNoDisponible e) {
                noRevisados.add(tipo);
            }
        }
        return new ResultadoRevision(inicio, fin, reloj.instant(), revisados, noRevisados);
    }
}
