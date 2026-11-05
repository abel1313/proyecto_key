package com.ventas.key.hexagonal.cortecaja.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.cortecaja.dominio.modelo.CorteDeCaja;
import com.ventas.key.hexagonal.cortecaja.dominio.modelo.CorteDeZona;
import com.ventas.key.hexagonal.cortecaja.dominio.modelo.MontosDeGanancia;
import com.ventas.key.hexagonal.cortecaja.dominio.modelo.PedidoPorCobrar;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * El JSON del corte de caja. Los montos van en número (pesos con centavos). El objeto {@code ganancia} (costo y ganancia) va en el pedido,
 * en la zona y en el total: todo este endpoint es solo de administrador.
 */
public record CorteDeCajaResponse(Instant generado, double total, int cantidadPedidos, int articulos, Ganancia ganancia, List<Zona> zonas) {

    public record Zona(Integer zonaId, String zona, double subtotal, int cantidad, int articulos, Ganancia ganancia, List<Pedido> pedidos) {
    }

    public record Pedido(int pedidoId, String cliente, int articulos, double total, double pagado, int pagos, boolean dioAnticipo,
                         double saldo, LocalDate fechaPedido, String estado, Ganancia ganancia) {
    }

    /** costo; precio normal y su ganancia; con descuento y su ganancia. {@code hayDescuento} = algún artículo tiene descuento. */
    public record Ganancia(double costo, double ventaNormal, double gananciaNormal, double conDescuento, double gananciaConDescuento,
                           boolean hayDescuento) {
    }

    public static CorteDeCajaResponse de(CorteDeCaja corte) {
        return new CorteDeCajaResponse(corte.generado(), corte.total().doubleValue(), corte.cantidadPedidos(),
                corte.zonas().stream().mapToInt(CorteDeZona::articulos).sum(), ganancia(corte.montos()),
                corte.zonas().stream().map(CorteDeCajaResponse::zona).toList());
    }

    private static Zona zona(CorteDeZona z) {
        return new Zona(z.zonaId(), z.zonaId() == null ? "Sin zona" : z.zona(), z.subtotal().doubleValue(), z.cantidad(), z.articulos(),
                ganancia(z.montos()), z.pedidos().stream().map(CorteDeCajaResponse::pedido).toList());
    }

    private static Pedido pedido(PedidoPorCobrar p) {
        return new Pedido(p.pedidoId(), p.cliente(), p.articulos(), p.total().doubleValue(), p.pagado().doubleValue(), p.pagos(),
                p.dioAnticipo(), p.saldo().doubleValue(), p.fechaPedido(), p.estado(), ganancia(p.montos()));
    }

    private static Ganancia ganancia(MontosDeGanancia m) {
        return new Ganancia(m.costo().doubleValue(), m.ventaNormal().doubleValue(), m.gananciaNormal().doubleValue(),
                m.conDescuento().doubleValue(), m.gananciaConDescuento().doubleValue(), m.hayDescuento());
    }
}
