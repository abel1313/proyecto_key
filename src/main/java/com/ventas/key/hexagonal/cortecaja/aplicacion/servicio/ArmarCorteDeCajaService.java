package com.ventas.key.hexagonal.cortecaja.aplicacion.servicio;

import com.ventas.key.hexagonal.cortecaja.dominio.modelo.CorteDeCaja;
import com.ventas.key.hexagonal.cortecaja.dominio.modelo.CorteDeZona;
import com.ventas.key.hexagonal.cortecaja.dominio.modelo.PedidoPorCobrar;
import com.ventas.key.hexagonal.cortecaja.dominio.puerto.entrada.ArmarCorteDeCajaCasoUso;
import com.ventas.key.hexagonal.cortecaja.dominio.puerto.salida.PedidosAbiertosPort;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * [Hexagonal: Use Case] [Clean: Use Cases] Java puro, sin Spring.
 *
 * <p>Deja solo los pedidos con algo por cobrar, los agrupa por zona (zonas por nombre, «Sin zona» al final) y ordena los pedidos por número.
 */
public class ArmarCorteDeCajaService implements ArmarCorteDeCajaCasoUso {

    private final PedidosAbiertosPort pedidos;
    private final Clock reloj;

    public ArmarCorteDeCajaService(PedidosAbiertosPort pedidos, Clock reloj) {
        this.pedidos = pedidos;
        this.reloj = reloj;
    }

    @Override
    public CorteDeCaja armar(Integer zonaId) {
        // Clave null = «Sin zona». Se agrupa por id de zona.
        Map<Integer, List<PedidoPorCobrar>> porZona = new LinkedHashMap<>();
        for (PedidoPorCobrar p : pedidos.sinEntregar()) {
            if (!p.tieneSaldo()) {
                continue;
            }
            if (zonaId != null && !Objects.equals(zonaId, p.zonaId())) {
                continue;
            }
            porZona.computeIfAbsent(p.zonaId(), k -> new ArrayList<>()).add(p);
        }

        List<CorteDeZona> zonas = new ArrayList<>();
        porZona.forEach((id, lista) -> {
            lista.sort(Comparator.comparingInt(PedidoPorCobrar::pedidoId));
            zonas.add(new CorteDeZona(id, id == null ? null : lista.get(0).zona(), lista));
        });
        // Zonas por nombre; «Sin zona» (sin id) siempre al final.
        zonas.sort(Comparator
                .comparing((CorteDeZona z) -> z.zonaId() == null)
                .thenComparing(z -> z.zona() == null ? "" : z.zona().toLowerCase()));
        return new CorteDeCaja(reloj.instant(), zonas);
    }
}
