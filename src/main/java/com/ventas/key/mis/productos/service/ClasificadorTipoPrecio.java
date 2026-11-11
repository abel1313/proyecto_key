package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.entity.DetallePedidoPrecioGrupo;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IDetallePedidoPrecioGrupoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dice A QUÉ TIPO DE PRECIO se vendió una línea, para guardarlo en {@code detalle_venta_variantes.tipo_precio} al registrar la venta
 * (2026-11-11): VENTA (el precio normal), REBAJA (el precio con descuento del producto o artículo), CAMPANA (precio de campaña),
 * GRUPO (precio por cantidad), PROMOCION (combo/promoción) u OTRO (cualquier otro precio).
 *
 * <p>Nunca tira una venta: ante cualquier falla devuelve {@code null} (la venta queda sin tipo y el reporte la estima). Los pedidos ya
 * guardados no recuerdan el origen en memoria, así que se lee de {@code detalle_pedido_precio_grupo}.
 */
@Slf4j
@Component
public class ClasificadorTipoPrecio {

    public static final String VENTA = "VENTA";
    public static final String REBAJA = "REBAJA";
    public static final String CAMPANA = "CAMPANA";
    public static final String GRUPO = "GRUPO";
    public static final String PROMOCION = "PROMOCION";
    public static final String OTRO = "OTRO";

    private static final double TOLERANCIA = 0.005;

    /** Opcional: sin la tabla del precio de grupo/campaña, el origen no se conoce y se clasifica solo comparando precios. */
    @Autowired(required = false)
    private IDetallePedidoPrecioGrupoRepository origenes;

    /** El origen (GRUPO o CAMPANA) de las líneas que ya están guardadas: detallePedidoId → origen. Vacío si no se pudo leer. */
    public Map<Integer, String> origenesDe(Collection<DetallePedido> lineas) {
        Map<Integer, String> mapa = new HashMap<>();
        try {
            if (origenes == null || lineas == null) return mapa;
            List<Integer> ids = new ArrayList<>();
            for (DetallePedido dp : lineas) if (dp.getId() != null) ids.add(dp.getId());
            if (ids.isEmpty()) return mapa;
            for (DetallePedidoPrecioGrupo g : origenes.findByDetallePedidoIdIn(ids)) mapa.put(g.getDetallePedidoId(), g.getOrigen());
        } catch (RuntimeException e) {
            log.warn("No se pudo leer el origen del precio de las líneas: {}", e.getMessage());
        }
        return mapa;
    }

    /** El tipo de precio de una línea de pedido (a punto de volverse venta). {@code origenesGuardados} viene de {@link #origenesDe}; puede ser null. */
    public String deLinea(DetallePedido dp, Map<Integer, String> origenesGuardados) {
        try {
            String origen = dp.getOrigenDelDescuento() != null ? dp.getOrigenDelDescuento().name()
                    : (dp.getId() != null && origenesGuardados != null ? origenesGuardados.get(dp.getId()) : null);
            Variantes v = dp.getVariante();
            Producto p = v != null ? v.getProducto() : dp.getProducto();
            Double normal = v != null ? v.precioNormal() : (p != null ? p.getPrecioVenta() : null);
            Double descuento = v != null ? v.precioDescuento() : (p != null ? p.getPrecioRebaja() : null);
            return clasificar(dp.getPromocion() != null, origen, dp.getPrecioUnitario(), normal, descuento);
        } catch (RuntimeException e) {
            log.warn("No se pudo clasificar el tipo de precio de una línea: {}", e.getMessage());
            return null;
        }
    }

    /** La regla, sin Spring: promoción → PROMOCION; origen CAMPANA/GRUPO → ese; si no, se compara el precio cobrado con la rebaja y con el normal. */
    public static String clasificar(boolean promocion, String origen, Double cobrado, Double normal, Double descuento) {
        if (promocion) return PROMOCION;
        if (CAMPANA.equals(origen)) return CAMPANA;
        if (GRUPO.equals(origen)) return GRUPO;
        if (cobrado == null) return OTRO;
        boolean hayRebaja = descuento != null && descuento > 0 && (normal == null || descuento < normal);
        if (hayRebaja && Math.abs(cobrado - descuento) < TOLERANCIA) return REBAJA;
        if (normal != null && Math.abs(cobrado - normal) < TOLERANCIA) return VENTA;
        return OTRO;
    }
}
