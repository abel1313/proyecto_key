package com.ventas.key.hexagonal.flete.aplicacion.servicio;

import com.ventas.key.hexagonal.flete.dominio.modelo.AsignacionDeViaje;
import com.ventas.key.hexagonal.flete.dominio.modelo.LineaVendida;
import com.ventas.key.hexagonal.flete.dominio.modelo.ParteAsignada;
import com.ventas.key.hexagonal.flete.dominio.modelo.RenglonDisponible;
import com.ventas.key.hexagonal.flete.dominio.puerto.entrada.AsignarViajeAVentaCasoUso;
import com.ventas.key.hexagonal.flete.dominio.puerto.salida.LigasDeVentaPort;

import java.util.Comparator;
import java.util.List;

/**
 * [Hexagonal: Application] [Clean: Use Case] Orquesta; la regla vive en {@link AsignacionDeViaje}. Java puro.
 * Las líneas se procesan por producto (de menor a mayor id) para bloquear siempre en el mismo orden y no cruzar bloqueos entre ventas.
 * Si algo falla, la línea queda «sin viaje» y la venta sigue: aquí nunca se lanza la excepción.
 */
public class AsignarViajeAVentaService implements AsignarViajeAVentaCasoUso {

    private final LigasDeVentaPort ligas;

    public AsignarViajeAVentaService(LigasDeVentaPort ligas) {
        this.ligas = ligas;
    }

    @Override
    public void asignar(List<LineaVendida> lineas) {
        if (lineas == null || lineas.isEmpty() || !ligas.disponible()) {
            return;
        }
        lineas.stream().sorted(Comparator.comparingInt(LineaVendida::productoId).thenComparingInt(LineaVendida::detalleVentaId)).forEach(l -> {
            try {
                List<RenglonDisponible> disponibles = ligas.bloquearYLeer(l.productoId());
                if (disponibles.isEmpty()) {
                    return;
                }
                // La existencia ya trae descontada esta línea (el stock baja al crear el pedido), así que antes de ella había existencia + cantidad.
                int antes = ligas.existenciaActual(l.productoId()) + l.cantidad();
                int sinViaje = AsignacionDeViaje.piezasSinViaje(antes, disponibles);
                List<ParteAsignada> partes = AsignacionDeViaje.repartir(l.cantidad(), sinViaje, disponibles);
                if (!partes.isEmpty()) {
                    ligas.guardar(l.detalleVentaId(), partes);
                }
            } catch (RuntimeException e) {
                // La venta ya está hecha (y a veces cobrada): no se deshace por un viaje. La línea queda sin viaje.
                org.slf4j.LoggerFactory.getLogger(AsignarViajeAVentaService.class)
                        .warn("No se pudo ligar a un viaje la línea {} (producto {}): {}", l.detalleVentaId(), l.productoId(), e.getMessage());
            }
        });
    }
}
