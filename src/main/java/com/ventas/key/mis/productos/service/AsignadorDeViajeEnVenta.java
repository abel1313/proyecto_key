package com.ventas.key.mis.productos.service;

import com.ventas.key.hexagonal.flete.dominio.modelo.LineaVendida;
import com.ventas.key.hexagonal.flete.dominio.puerto.entrada.AsignarViajeAVentaCasoUso;
import com.ventas.key.mis.productos.entity.DetalleVentaVariante;
import com.ventas.key.mis.productos.entity.Venta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Puente entre las ventas de siempre y el dominio de fletes ({@code hexagonal/flete}): se llama justo DESPUÉS de guardar una venta (venta directa,
 * pedido cobrado, abono que liquida y cobro con tarjeta aprobado) y liga sus líneas a los viajes de donde salieron las piezas.
 * Nunca lanza: si algo falla (o la migración no está), la venta sigue y las piezas quedan «sin viaje».
 */
@Component
public class AsignadorDeViajeEnVenta {

    private static final Logger log = LoggerFactory.getLogger(AsignadorDeViajeEnVenta.class);

    private final AsignarViajeAVentaCasoUso asignar;

    public AsignadorDeViajeEnVenta(AsignarViajeAVentaCasoUso asignar) {
        this.asignar = asignar;
    }

    public void asignar(Venta venta) {
        try {
            if (venta == null || venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
                return;
            }
            List<LineaVendida> lineas = new ArrayList<>();
            for (DetalleVentaVariante d : venta.getDetalles()) {
                if (d.getId() == null || d.getVariante() == null || d.getVariante().getProducto() == null || d.getCantidad() == null || d.getCantidad() <= 0) {
                    continue;
                }
                lineas.add(new LineaVendida(d.getId(), d.getVariante().getProducto().getId(), d.getCantidad()));
            }
            asignar.asignar(lineas);
        } catch (Throwable e) {
            log.warn("No se pudieron ligar a un viaje las líneas de la venta {}: {}", venta == null ? null : venta.getId(), e.toString());
        }
    }
}
